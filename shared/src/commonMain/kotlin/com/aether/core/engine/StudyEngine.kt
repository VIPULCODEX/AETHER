package com.aether.core.engine

import com.aether.core.model.Flashcard
import com.aether.core.model.StudySource
import kotlin.math.ln
import kotlin.math.max
import kotlin.random.Random

data class Chunk(val sourceId: String, val sourceTitle: String, val index: Int, val text: String)

data class Citation(val sourceTitle: String, val snippet: String)

data class StudyAnswer(val text: String, val citations: List<Citation>, val found: Boolean)

data class CardDraft(val sourceId: String, val front: String, val back: String)

data class QuizQuestion(val prompt: String, val options: List<String>, val correctIndex: Int)

private const val MAX_CHUNK_CHARS = 420
private const val MAX_LLM_CONTEXT_CHARS = 2200
private const val NOT_IN_SOURCES = "NOT_IN_SOURCES"
private const val K1 = 1.5
private const val B = 0.75

/**
 * NotebookLM-style study tools that run with no model and no network:
 * BM25 retrieval over your own sources, answers assembled *only* from your
 * text (so every claim is cited and nothing is invented), extractive
 * summaries, and flashcard/quiz generation. A generative on-device LLM can
 * later sit on top of [retrieve] for fluent phrasing without changing this
 * grounding.
 */
class StudyEngine {

    fun chunk(sources: List<StudySource>): List<Chunk> = sources.flatMap { source ->
        val chunks = mutableListOf<Chunk>()
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isNotBlank()) chunks += Chunk(source.id, source.title, chunks.size, buffer.toString().trim())
            buffer.clear()
        }
        splitSentences(source.content).forEach { sentence ->
            if (buffer.isNotEmpty() && buffer.length + sentence.length > MAX_CHUNK_CHARS) flush()
            if (buffer.isNotEmpty()) buffer.append(' ')
            buffer.append(sentence)
        }
        flush()
        chunks
    }

    /** Top [limit] chunks for [query] by BM25, best first. Chunks sharing no term with the query are never returned. */
    fun retrieve(query: String, chunks: List<Chunk>, limit: Int = 3): List<Pair<Chunk, Double>> {
        val queryTerms = tokenize(query).distinct()
        if (queryTerms.isEmpty() || chunks.isEmpty()) return emptyList()
        val docs = chunks.map { tokenize(it.text) }
        val avgLen = docs.sumOf { it.size }.toDouble() / docs.size
        val docFreq = queryTerms.associateWith { term -> docs.count { term in it } }
        return chunks.indices.map { i ->
            val tf = docs[i].groupingBy { it }.eachCount()
            val score = queryTerms.sumOf { term ->
                val f = tf[term] ?: 0
                if (f == 0) 0.0 else {
                    val n = docFreq.getValue(term)
                    val idf = ln(1.0 + (docs.size - n + 0.5) / (n + 0.5))
                    idf * f * (K1 + 1) / (f + K1 * (1 - B + B * docs[i].size / max(avgLen, 1.0)))
                }
            }
            chunks[i] to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(limit)
    }

    fun answer(question: String, chunks: List<Chunk>): StudyAnswer {
        val hits = retrieve(question, chunks)
        if (hits.isEmpty()) {
            return StudyAnswer("I couldn't find that in your sources.", emptyList(), found = false)
        }
        val terms = tokenize(question).toSet()
        val sentences = hits.flatMap { (chunk, _) -> splitSentences(chunk.text).map { it to chunk } }
        val best = sentences
            .map { (s, chunk) -> Triple(s, chunk, tokenize(s).count { it in terms }) }
            .filter { it.third > 0 }
            .sortedByDescending { it.third }
            .take(2)
            .sortedWith(compareBy({ hits.indexOfFirst { h -> h.first === it.second } }, { it.second.text.indexOf(it.first) }))
        return StudyAnswer(
            text = best.joinToString(" ") { it.first },
            citations = best.map { Citation(it.second.sourceTitle, it.first) }.distinct(),
            found = true
        )
    }

    /**
     * Grounded generative answer. Retrieval still decides *whether* an answer
     * exists: with no matching passage the model is never called, so it can't
     * invent one. The model only rephrases passages it is handed, must cite
     * them as [n], and any failure falls back to the extractive answer.
     */
    suspend fun answerGrounded(question: String, chunks: List<Chunk>, llm: LocalLlm?): StudyAnswer {
        val ranked = retrieve(question, chunks).map { it.first }
        if (ranked.isEmpty()) return answer(question, chunks)
        val hits = withNeighbor(ranked, chunks)
        if (llm == null || !llm.isReady) return answer(question, chunks)
        val raw = runCatching { llm.generate(answerPrompt(question, hits)) }.getOrNull()?.trim()
        if (raw.isNullOrBlank()) return answer(question, chunks)
        if (raw.contains(NOT_IN_SOURCES)) return StudyAnswer("That isn't covered in your sources.", emptyList(), found = false)

        // Don't trust the model's [n] alone (small models mis-number). Cite the sentences, across every passage it was
        // shown, that best support the *answer*; the passages it named only break ties.
        val named = Regex("\\[(\\d+)]").findAll(raw).mapNotNull { it.groupValues[1].toIntOrNull() }
            .filter { it in 1..hits.size }.map { hits[it - 1] }.toSet()
        val questionTerms = tokenize(question).toSet()
        val answerOnlyTerms = tokenize(raw).toSet() - questionTerms
        val citations = hits.flatMap { chunk -> splitSentences(chunk.text).map { it to chunk } }
            .map { (sentence, chunk) ->
                val score = tokenize(sentence).sumOf { t -> (if (t in answerOnlyTerms) 3 else if (t in questionTerms) 1 else 0).toInt() }
                Triple(sentence, chunk, score + if (chunk in named) 0.5 else 0.0)
            }
            .filter { it.third > 0 }
            .sortedByDescending { it.third }
            .distinctBy { it.first }
            .take(2)
            .map { Citation(it.second.sourceTitle, it.first) }
            .ifEmpty { listOf(Citation(hits.first().sourceTitle, hits.first().text.take(160))) }
        return StudyAnswer(raw, citations, found = true)
    }

    /**
     * Chunk boundaries can split a thought ("The Calvin cycle uses... / This stage occurs in the stroma"),
     * so the best hit is accompanied by the chunk that follows it in the same source.
     */
    private fun withNeighbor(ranked: List<Chunk>, all: List<Chunk>): List<Chunk> {
        val top = ranked.first()
        val next = all.firstOrNull { it.sourceId == top.sourceId && it.index == top.index + 1 }
        return (ranked + listOfNotNull(next)).distinct().take(4)
    }

    internal fun answerPrompt(question: String, hits: List<Chunk>): String = buildString {
        appendLine("Answer the question using ONLY the numbered passages below. Cite the passages you use like [1]. If the answer is not in the passages, reply exactly: $NOT_IN_SOURCES")
        appendLine()
        hits.forEachIndexed { i, c -> appendLine("[${i + 1}] (${c.sourceTitle}) ${c.text}") }
        appendLine()
        appendLine("Question: ${question.trim()}")
        append("Answer:")
    }

    /** Short bullet summary written by the model, falling back to the extractive one if it is unavailable or fails. */
    suspend fun summarizeGrounded(text: String, llm: LocalLlm?, count: Int = 4): List<String> {
        if (llm != null && llm.isReady) {
            val prompt = "Summarize the notes below as $count short bullet points. Use only information in the notes.\n\nNotes:\n${text.take(MAX_LLM_CONTEXT_CHARS)}\n\nSummary:"
            val bullets = runCatching { llm.generate(prompt) }.getOrNull().orEmpty()
                .lines().map { it.trim().trimStart('-', '*', '•', ' ') }.filter { it.length > 8 }
            if (bullets.isNotEmpty()) return bullets.take(count + 2)
        }
        return summarize(text, count)
    }

    /** Centroid-based extractive summary: the [count] sentences richest in the document's most frequent terms, kept in original order. */
    fun summarize(text: String, count: Int = 4): List<String> {
        val sentences = splitSentences(text)
        if (sentences.size <= count) return sentences
        val termFreq = tokenize(text).groupingBy { it }.eachCount()
        val scored = sentences.mapIndexed { i, s ->
            val terms = tokenize(s)
            val score = if (terms.isEmpty()) 0.0 else terms.sumOf { (termFreq[it] ?: 0).toDouble() } / (terms.size + 5)
            Triple(i, s, score)
        }
        return scored.sortedByDescending { it.third }.take(count).sortedBy { it.first }.map { it.second }
    }

    internal val definitionPattern = Regex(
        "^([A-Z][\\w\\- ]{1,50}?)\\s+(?:is|are|refers to|is defined as|means|is called)\\s+(.{15,})$"
    )

    /** Definition sentences become "What is X?" cards; other fact-dense sentences become cloze deletions of their key term. */
    fun generateCards(source: StudySource, maxCards: Int = 12): List<CardDraft> {
        val sentences = splitSentences(source.content)
        val termFreq = tokenize(source.content).groupingBy { it }.eachCount()
        val drafts = mutableListOf<CardDraft>()
        val usedTerms = mutableSetOf<String>()

        sentences.forEach { s ->
            val match = definitionPattern.find(s.trimEnd('.'))
            val term = match?.groupValues?.get(1)?.trim()
            if (term != null && term.split(' ').size <= 5 && usedTerms.add(term.lowercase())) {
                drafts += CardDraft(source.id, "What ${if (isPlural(match.value, term)) "are" else "is"} $term?", s)
            }
        }
        sentences
            .filter { s -> drafts.none { it.back == s } && s.length in 40..220 }
            .sortedByDescending { s -> tokenize(s).sumOf { (termFreq[it] ?: 0).toDouble() } / (tokenize(s).size + 5) }
            .forEach { s ->
                if (drafts.size >= maxCards) return@forEach
                val key = keyTerm(s, termFreq) ?: return@forEach
                if (usedTerms.add(key.lowercase())) {
                    drafts += CardDraft(source.id, s.replaceFirst(key, "_____", ignoreCase = true), key)
                }
            }
        return drafts.take(maxCards)
    }

    /** The longest, most document-frequent word in the sentence that actually appears verbatim (so it can be blanked). */
    private fun keyTerm(sentence: String, termFreq: Map<String, Int>): String? =
        Regex("[\\p{L}][\\p{L}\\-]{4,}").findAll(sentence).map { it.value }
            .filter { w -> tokenize(w).firstOrNull()?.let { it in termFreq } == true }
            .maxByOrNull { w -> w.length + (termFreq[tokenize(w).first()] ?: 0) * 2 }

    private fun isPlural(sentence: String, term: String): Boolean =
        sentence.removePrefix(term).trimStart().startsWith("are")

    /** Multiple-choice quiz from cloze cards (their answers double as each other's distractors). Deterministic for a given [seed]. */
    fun buildQuiz(cards: List<Flashcard>, count: Int = 5, seed: Int = 0): List<QuizQuestion> {
        val random = Random(seed)
        val answers = cards.map { it.back }.filter { it.length <= 40 }.distinct()
        return cards.filter { it.back.length <= 40 && "_____" in it.front }
            .shuffled(random).take(count).mapNotNull { card ->
                val distractors = answers.filter { !it.equals(card.back, ignoreCase = true) }.shuffled(random).take(3)
                if (distractors.size < 2) return@mapNotNull null
                val options = (distractors + card.back).shuffled(random)
                QuizQuestion(card.front, options, options.indexOf(card.back))
            }
    }
}

data class Schedule(val ease: Double, val intervalDays: Int, val repetitions: Int, val dueAt: Long)

enum class Grade(val quality: Int, val label: String) { AGAIN(1, "Again"), HARD(3, "Hard"), GOOD(4, "Good"), EASY(5, "Easy") }

/** SuperMemo-2: the classic spaced-repetition scheduler. */
object SpacedRepetition {
    private const val DAY = 86_400_000L

    fun review(card: Flashcard, grade: Grade, nowMillis: Long): Schedule {
        val q = grade.quality
        val ease = max(1.3, card.ease + 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val (reps, interval) = if (q < 3) {
            0 to 1
        } else {
            val next = when (card.repetitions) {
                0 -> 1
                1 -> 6
                else -> kotlin.math.round(card.intervalDays * ease).toInt()
            }
            card.repetitions + 1 to next
        }
        return Schedule(ease, interval, reps, nowMillis + interval * DAY)
    }
}
