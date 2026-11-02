package com.aether.core.engine

enum class QuestionType(val label: String) { MCQ("MCQ"), MSQ("MSQ"), NAT("Numerical") }

/** A GATE-style item: single-correct (MCQ), multi-correct (MSQ) or numerical answer type (NAT). */
data class PracticeQuestion(
    val type: QuestionType,
    val prompt: String,
    val options: List<String>,
    val correct: Set<Int>,
    val numeric: Double?,
    val explanation: String
) {
    /** MSQ is all-or-nothing, as in the real exam; NAT allows a small rounding tolerance. */
    fun isCorrect(selected: Set<Int>, typed: Double?): Boolean = when (type) {
        QuestionType.MCQ, QuestionType.MSQ -> selected == correct
        QuestionType.NAT -> typed != null && numeric != null && kotlin.math.abs(typed - numeric) <= maxOf(0.01, kotlin.math.abs(numeric) * 0.01)
    }
}

/**
 * Exam-aware teaching on top of the grounded study tools. The user's own
 * description of how a subject is examined ([examNotes]) is injected into
 * the prompts, so the same sources get explained and drilled the way *that*
 * exam asks about them. Everything is built only from the passages given.
 */
class ExamEngine(private val study: StudyEngine = StudyEngine()) {

    internal fun explainPrompt(text: String, examNotes: String?): String = buildString {
        appendLine("You are an excellent teacher. Explain the following material clearly for someone preparing for an exam.")
        appendLine("Structure: 1) the core idea in plain words, 2) a tiny worked example or analogy, 3) the common traps. Use only the material; do not add facts that are not in it.")
        if (!examNotes.isNullOrBlank()) appendLine("How this exam usually tests it: ${examNotes.trim()}")
        appendLine()
        appendLine("Material:")
        appendLine(text.take(2500))
        append("Explanation:")
    }

    suspend fun explain(text: String, examNotes: String?, llm: LocalLlm?): String? {
        if (llm == null || !llm.isReady) return null
        return runCatching { llm.generate(explainPrompt(text, examNotes)) }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    internal fun practicePrompt(passages: String, examNotes: String?, count: Int): String = buildString {
        appendLine("Write $count exam practice questions based ONLY on the passages below. Mix types: MCQ (one correct option), MSQ (one or more correct options), NAT (a numerical answer).")
        if (!examNotes.isNullOrBlank()) appendLine("Match this exam style: ${examNotes.trim()}")
        appendLine("Use exactly this format for every question:")
        appendLine("Q: <question>")
        appendLine("A) <option>")
        appendLine("B) <option>")
        appendLine("C) <option>")
        appendLine("D) <option>")
        appendLine("TYPE: MCQ or MSQ or NAT")
        appendLine("ANSWER: <letters like B or A,C — or the number for NAT>")
        appendLine("WHY: <one-sentence explanation>")
        appendLine("(For NAT questions omit the options.)")
        appendLine()
        appendLine("Passages:")
        appendLine(passages)
        append("Questions:")
    }

    /** Generates practice questions about [topic] (or the whole notebook when blank). Empty without a model, or if its output is unusable. */
    suspend fun practice(chunks: List<Chunk>, topic: String?, examNotes: String?, llm: LocalLlm?, count: Int = 4): List<PracticeQuestion> {
        if (llm == null || !llm.isReady || chunks.isEmpty()) return emptyList()
        val selected = if (!topic.isNullOrBlank()) study.retrieve(topic, chunks, 4).map { it.first } else spread(chunks, 4)
        if (selected.isEmpty()) return emptyList()
        val passages = selected.joinToString("\n") { "- ${it.text}" }.take(2800)
        val raw = runCatching { llm.generate(practicePrompt(passages, examNotes, count)) }.getOrNull() ?: return emptyList()
        return parseQuestions(raw)
    }

    private fun spread(chunks: List<Chunk>, n: Int): List<Chunk> {
        if (chunks.size <= n) return chunks
        val step = chunks.size.toDouble() / n
        return (0 until n).map { chunks[(it * step).toInt()] }
    }

    fun parseQuestions(raw: String): List<PracticeQuestion> =
        raw.split(Regex("(?m)^\\s*(?:Q\\d*\\s*[:.)]|\\d+\\s*[.)])\\s*")).drop(1).mapNotNull { parseBlock(it) }

    private fun parseBlock(block: String): PracticeQuestion? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val optionRegex = Regex("^\\(?([A-Da-d])[).:]\\s*(.+)$")
        val prompt = lines.takeWhile { !optionRegex.matches(it) && !it.uppercase().startsWith("TYPE") && !it.uppercase().startsWith("ANSWER") }
            .joinToString(" ").trim()
        val options = lines.mapNotNull { optionRegex.find(it)?.groupValues?.get(2)?.trim() }
        val typeText = lines.firstOrNull { it.uppercase().startsWith("TYPE") }?.substringAfter(':')?.trim()?.uppercase().orEmpty()
        val answer = lines.firstOrNull { it.uppercase().startsWith("ANSWER") }?.substringAfter(':')?.trim() ?: return null
        val why = lines.firstOrNull { it.uppercase().startsWith("WHY") }?.substringAfter(':')?.trim().orEmpty()
        if (prompt.length < 10) return null

        val type = when {
            "NAT" in typeText || options.isEmpty() -> QuestionType.NAT
            "MSQ" in typeText -> QuestionType.MSQ
            else -> QuestionType.MCQ
        }
        if (type == QuestionType.NAT) {
            val number = Regex("-?\\d+(?:\\.\\d+)?").find(answer)?.value?.toDoubleOrNull() ?: return null
            return PracticeQuestion(type, prompt, emptyList(), emptySet(), number, why)
        }
        if (options.size < 2) return null
        val correct = Regex("[A-Da-d]").findAll(answer.takeWhile { it in "ABCDabcd,&() \t" }).map { it.value.uppercase()[0] - 'A' }.filter { it < options.size }.toSet()
        if (correct.isEmpty() || (type == QuestionType.MCQ && correct.size != 1)) return null
        return PracticeQuestion(type, prompt, options, correct, null, why)
    }
}
