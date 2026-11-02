package com.aether.core.engine

import com.aether.core.model.Flashcard
import com.aether.core.model.StudySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val BIO = "Photosynthesis is the process by which plants convert light energy into chemical energy stored in glucose. It takes place mainly in the chloroplasts of leaf cells.\n\nChlorophyll is the green pigment that absorbs light, mostly in the blue and red wavelengths. The light-dependent reactions occur in the thylakoid membranes and produce ATP and NADPH.\n\nThe Calvin cycle uses ATP and NADPH to fix carbon dioxide into sugars. This stage occurs in the stroma and does not require light directly."

class StudyEngineTest {
    private val engine = StudyEngine()
    private val source = StudySource("s1", "n", "Bio notes", BIO, 0)
    private val chunks = engine.chunk(listOf(source))

    @Test fun answersFromSourcesWithCitation() {
        val a = engine.answer("What absorbs light?", chunks)
        assertTrue(a.found && "Chlorophyll" in a.text)
        assertEquals("Bio notes", a.citations.first().sourceTitle)
    }

    @Test fun saysNotFoundInsteadOfGuessing() {
        assertFalse(engine.answer("quantum chromodynamics", chunks).found)
    }

    @Test fun modelIsNeverCalledWithoutEvidence() {
        val llm = FakeLlm("invented")
        val a = runSuspend { engine.answerGrounded("quantum chromodynamics", chunks, llm) }
        assertFalse(a.found)
        assertEquals(0, llm.calls)
    }

    @Test fun citationShowsSentenceSupportingTheAnswer() {
        val llm = FakeLlm("It happens in the stroma [1].")
        val a = runSuspend { engine.answerGrounded("Where does the Calvin cycle occur?", chunks, llm) }
        assertTrue("stroma" in a.citations.first().snippet)
    }

    @Test fun modelRefusalAndFailureAreHandled() {
        assertFalse(runSuspend { engine.answerGrounded("What absorbs light?", chunks, FakeLlm("NOT_IN_SOURCES")) }.found)
        assertTrue(runSuspend { engine.answerGrounded("What absorbs light?", chunks, FakeLlm(null)) }.found)
        assertTrue(runSuspend { engine.answerGrounded("What absorbs light?", chunks, FakeLlm("x", isReady = false)) }.text != "x")
    }

    @Test fun outOfRangeCitationMarkersAreIgnored() {
        val a = runSuspend { engine.answerGrounded("What absorbs light?", chunks, FakeLlm("Chlorophyll [9].")) }
        assertTrue(a.citations.isNotEmpty() && a.citations.size <= 2)
    }

    @Test fun cardsAreGeneratedAndQuizUsesThem() {
        val drafts = engine.generateCards(source)
        assertTrue(drafts.any { it.front.startsWith("What is Photosynthesis") })
        val cards = drafts.mapIndexed { i, d -> Flashcard("c$i", "n", "s1", d.front, d.back, 2.5, 0, 0, 0, 0) }
        val quiz = engine.buildQuiz(cards, 3, seed = 1)
        assertTrue(quiz.all { it.options[it.correctIndex].isNotBlank() && it.options.distinct().size == it.options.size })
    }

    @Test fun spacedRepetitionIntervalsGrowThenReset() {
        var card = Flashcard("c", "n", null, "f", "b", 2.5, 0, 0, 0, 0)
        val seen = mutableListOf<Int>()
        for (g in listOf(Grade.GOOD, Grade.GOOD, Grade.GOOD)) {
            val s = SpacedRepetition.review(card, g, 0)
            seen += s.intervalDays
            card = card.copy(ease = s.ease, intervalDays = s.intervalDays, repetitions = s.repetitions)
        }
        assertEquals(listOf(1, 6, 15), seen)
        val lapse = SpacedRepetition.review(card, Grade.AGAIN, 0)
        assertEquals(0, lapse.repetitions)
        assertEquals(1, lapse.intervalDays)
        assertTrue(lapse.ease >= 1.3)
    }

    @Test fun digestKeepsHeadingsOutOfSentencesAndExtractsTerms() {
        val web = "## Cache memory\n\nCache memory is a small fast memory between CPU and main memory. It stores recently used data to cut average access time.\n\n## Mapping\n\nDirect mapping is a scheme where each block maps to exactly one cache line. Associative mapping allows a block to sit in any line."
        val d = DigestEngine().digest(web)
        assertEquals(listOf("Cache memory", "Mapping"), d.outline)
        assertTrue(d.terms.any { it.term.startsWith("Cache memory") })
        assertTrue(d.points.none { it.text.startsWith("#") })
    }

    @Test fun practiceQuestionsParseAndGrade() {
        val raw = "Q: A direct mapped cache has 64 lines. How many index bits?\nA) 4\nB) 6\nC) 8\nD) 10\nTYPE: MCQ\nANSWER: B\nWHY: log2(64)=6\n\nQ: Which reduce conflict misses?\nA) Associativity\nB) Bigger blocks only\nC) Victim cache\nTYPE: MSQ\nANSWER: A, C\nWHY: yes\n\nQ: Offset bits for a 16-byte block?\nTYPE: NAT\nANSWER: 4\nWHY: 2^4\n\nQ: broken question without any answer\nA) x\nB) y"
        val qs = ExamEngine().parseQuestions(raw)
        assertEquals(3, qs.size)
        assertTrue(qs[0].isCorrect(setOf(1), null))
        assertFalse(qs[1].isCorrect(setOf(0), null))
        assertTrue(qs[1].isCorrect(setOf(0, 2), null))
        assertTrue(qs[2].isCorrect(emptySet(), 4.0))
        assertNotEquals(true, qs[2].isCorrect(emptySet(), 5.0))
    }

    @Test fun practiceNeedsModelAndInjectsExamNotes() {
        assertTrue(runSuspend { ExamEngine().practice(chunks, null, null, null) }.isEmpty())
        val llm = FakeLlm("garbage")
        assertTrue(runSuspend { ExamEngine().practice(chunks, "light", "mostly numericals", llm) }.isEmpty())
        assertTrue("mostly numericals" in llm.lastPrompt)
    }
}
