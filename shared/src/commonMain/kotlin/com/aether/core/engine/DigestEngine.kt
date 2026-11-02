package com.aether.core.engine

data class DigestPoint(val text: String, val context: String)

data class DigestTerm(val term: String, val definition: String)

/**
 * A skimmable, tappable breakdown of one source: the gist, the points worth
 * remembering (each with the surrounding line so it can be expanded), the
 * key terms with their definitions, and the section outline.
 */
data class Digest(
    val tldr: String,
    val points: List<DigestPoint>,
    val terms: List<DigestTerm>,
    val outline: List<String>,
    val readMinutes: Int,
    val byModel: Boolean
)

class DigestEngine(private val study: StudyEngine = StudyEngine()) {

    fun digest(text: String): Digest {
        val sentences = splitSentences(text)
        val points = study.summarize(text, 6).map { s ->
            val i = sentences.indexOf(s)
            val context = sentences.getOrNull(i + 1) ?: sentences.getOrNull(i - 1) ?: ""
            DigestPoint(s, context)
        }
        val terms = sentences.mapNotNull { s ->
            val m = study.definitionPattern.find(s.trimEnd('.')) ?: return@mapNotNull null
            val term = m.groupValues[1].trim().replace(Regex("^(?:A|An|The)\\s+"), "")
            if (term.split(' ').size <= 4 && term.length >= 5 && !term.startsWith("An exception")) DigestTerm(term, s) else null
        }.distinctBy { it.term.lowercase() }.take(8)
        val words = text.split(Regex("\\s+")).size
        return Digest(
            tldr = study.summarize(text, 2).joinToString(" "),
            points = points,
            terms = terms,
            outline = extractHeadings(text),
            readMinutes = maxOf(1, words / 200),
            byModel = false
        )
    }

    /** Same structure; the one-paragraph gist is written by the model when one is loaded. */
    suspend fun digestGrounded(text: String, llm: LocalLlm?): Digest {
        val base = digest(text)
        if (llm == null || !llm.isReady) return base
        val prompt = "Write a 2 sentence TL;DR of the notes below. Use only information in the notes.\n\nNotes:\n${text.take(2200)}\n\nTL;DR:"
        val tldr = runCatching { llm.generate(prompt) }.getOrNull()?.trim()
        return if (tldr.isNullOrBlank()) base else base.copy(tldr = tldr, byModel = true)
    }
}
