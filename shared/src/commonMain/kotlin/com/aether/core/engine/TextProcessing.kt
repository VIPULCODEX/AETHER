package com.aether.core.engine

private val STOPWORDS = setOf(
    "a", "an", "the", "and", "or", "but", "of", "to", "in", "on", "at", "for", "with", "by", "from", "as", "is", "are", "was", "were",
    "be", "been", "being", "it", "its", "this", "that", "these", "those", "which", "who", "whom", "what", "when", "where", "why", "how",
    "do", "does", "did", "has", "have", "had", "can", "could", "will", "would", "should", "may", "might", "not", "no", "so", "than",
    "occur", "occurs", "use", "uses", "used", "using", "takes", "place", "called", "each", "most", "mostly", "more", "many", "one", "two", "then", "there", "their", "they", "them", "we", "you", "your", "i", "he", "she", "his", "her", "also", "such", "into", "about", "between"
)

/** Lowercased, stopword-free, lightly stemmed terms. Deliberately simple: it must run instantly on a phone with no model. */
fun tokenize(text: String): List<String> =
    text.lowercase().split(Regex("[^\\p{L}\\p{Nd}]+"))
        .filter { it.length >= 2 && it !in STOPWORDS }
        .map(::stem)

private fun stem(word: String): String = when {
    word.length > 5 && word.endsWith("ing") -> word.dropLast(3)
    word.length > 4 && word.endsWith("ed") -> word.dropLast(2)
    word.length > 4 && word.endsWith("es") -> word.dropLast(2)
    word.length > 3 && word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
    else -> word
}

private val SENTENCE_END = Regex("(?<=[.!?])\\s+(?=[\"'(\\[]?[A-Z0-9])")

fun splitSentences(text: String): List<String> =
    text.replace(Regex("[ \\t]+"), " ")
        .split(Regex("\\n\\s*\\n|\\n(?=[-*•]\\s)"))
        .filter { !it.trimStart().startsWith("#") }
        .flatMap { block -> block.trim().replace('\n', ' ').split(SENTENCE_END) }
        .map { it.trim() }
        .filter { it.length >= 20 }

/** Section headings in web-extracted text, which marks them as "## Heading" lines. */
fun extractHeadings(text: String): List<String> =
    text.lines().map { it.trim() }.filter { it.startsWith("#") }.map { it.trimStart('#', ' ') }.filter { it.isNotBlank() }
