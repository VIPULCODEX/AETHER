package com.aether.core.engine

/** One page of a document as read from the PDF (or OCR). Page numbers are 1-based, as printed readers expect. */
data class PageText(val page: Int, val text: String)

/** A heading-bounded slice of the document that extraction works on. [pageStart]..[pageEnd] lets every note link back to its source pages. */
data class NoteChunk(val id: Int, val heading: String, val pageStart: Int, val pageEnd: Int, val text: String)

/** Structured notes for one topic. Every field comes from the source text only; empty means "the source didn't say". */
data class StructuredNote(
    val topic: String,
    val summary: String = "",
    val definitions: List<String> = emptyList(),
    val formulas: List<String> = emptyList(),
    val traps: List<String> = emptyList(),
    val example: String = "",
    val flow: String = "",
    val pageStart: Int = 0,
    val pageEnd: Int = 0,
    /** 0..1: share of checkable claims (numbers, formulas, definitions) found in the source page text. */
    val confidence: Double = 1.0,
    /** Human-readable reasons this note should be double-checked against the page. */
    val flags: List<String> = emptyList()
) {
    val pageLabel: String get() = if (pageStart == pageEnd || pageEnd == 0) "p.$pageStart" else "pp.$pageStart-$pageEnd"
    val lowConfidence: Boolean get() = confidence < NotesPipeline.LOW_CONFIDENCE
}

/**
 * Document -> chunks -> structured notes -> verified notes -> detailed notes + one-page sheet.
 * Everything here is deterministic and runs on-device with no model; a model (on-device or the user's own cloud key)
 * only ever fills in [StructuredNote]s, and [verify] checks them against the source regardless of who wrote them.
 */
object NotesPipeline {
    const val LOW_CONFIDENCE = 0.7
    private const val MAX_CHUNK_CHARS = 2400

    /** PDF sources store a form feed between pages; anything else (web, pasted text) is a single page. */
    fun pagesFromContent(content: String): List<PageText> =
        content.split('\u000c').mapIndexed { i, t -> PageText(i + 1, t) }.filter { it.text.isNotBlank() }

    private val NUMBERED = Regex("^(\\d+(\\.\\d+)*[.)]?|[IVX]+\\.|[A-Z]\\.)\\s+\\S.{2,80}$")
    private val KEYWORD = Regex("^(chapter|unit|module|topic|section|lecture)\\s+[\\w.]+.*", RegexOption.IGNORE_CASE)

    private fun isHeading(line: String): Boolean {
        val t = line.trim()
        if (t.length < 3 || t.length > 90 || t.endsWith(".") || t.endsWith(",")) return false
        if (t.startsWith("#")) return true
        if (KEYWORD.matches(t)) return true
        if (NUMBERED.matches(t) && !t.contains('=') && t.count { it.isLetter() } >= 3) return true
        val letters = t.filter { it.isLetter() }
        return letters.length >= 4 && letters.all { it.isUpperCase() } && t.split(' ').size <= 10
    }

    /** Splits on heading lines, keeping page ranges, and caps chunk size so a model's context is never blown. */
    fun chunk(pages: List<PageText>): List<NoteChunk> {
        data class Acc(var heading: String, var start: Int, var end: Int, val sb: StringBuilder)
        val out = mutableListOf<NoteChunk>()
        var cur: Acc? = null
        fun flush() {
            val c = cur ?: return
            val body = c.sb.toString().trim()
            if (body.length >= 40) {
                body.chunked(MAX_CHUNK_CHARS).forEachIndexed { i, part ->
                    val h = if (i == 0) c.heading else "${c.heading} (cont.)"
                    out += NoteChunk(out.size, h, c.start, c.end, part)
                }
            }
            cur = null
        }
        for (p in pages.sortedBy { it.page }) {
            for (raw in p.text.lines()) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                if (isHeading(line)) {
                    flush()
                    cur = Acc(line.trimStart('#', ' '), p.page, p.page, StringBuilder())
                } else {
                    val c = cur ?: Acc("Page ${p.page}", p.page, p.page, StringBuilder()).also { cur = it }
                    c.end = p.page
                    c.sb.append(line).append('\n')
                }
            }
        }
        flush()
        return out
    }

    private val DEF_CUES = Regex("\\b(is defined as|are defined as|is called|are called|refers to|is known as|is the (process|measure|ratio|rate|number|study) of|means)\\b", RegexOption.IGNORE_CASE)
    private val TRAP_CUES = Regex("\\b(note that|note:|important|caution|remember|common mistake|do not|don't|never|only if|not equal|unlike|exception)\\b", RegexOption.IGNORE_CASE)
    private val EXAMPLE_CUES = Regex("^(example|e\\.g\\.|eg:|for instance|solved example|problem)\\b", RegexOption.IGNORE_CASE)
    private val FORMULA = Regex("[A-Za-z_][\\w()\\[\\]]*\\s*=\\s*[^=].{2,}")
    private val FLOW = Regex("(->|→|⇒|=>)")

    /** Rule-based extractor: the no-model default. Picks lines by cue words, so it can omit things but never invent them. */
    fun extractRules(chunk: NoteChunk): StructuredNote {
        val lines = chunk.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val sentences = splitSentences(chunk.text)
        val formulas = lines.filter { FORMULA.containsMatchIn(it) && it.length <= 120 && it.count { c -> c.isLetter() } < it.length }.distinct().take(8)
        val defs = sentences.filter { DEF_CUES.containsMatchIn(it) }.take(4)
        val traps = sentences.filter { TRAP_CUES.containsMatchIn(it) && it !in defs }.take(3)
        val example = lines.firstOrNull { EXAMPLE_CUES.containsMatchIn(it) }
            ?.let { first -> lines.dropWhile { it != first }.take(3).joinToString(" ") } ?: ""
        val flow = lines.firstOrNull { FLOW.containsMatchIn(it) && it.length <= 140 } ?: ""
        val summary = defs.firstOrNull() ?: sentences.firstOrNull() ?: ""
        return StructuredNote(
            topic = chunk.heading, summary = summary, definitions = defs, formulas = formulas, traps = traps,
            example = example, flow = flow, pageStart = chunk.pageStart, pageEnd = chunk.pageEnd
        )
    }

    private fun squash(s: String) = s.lowercase().replace(Regex("[\\s,]+"), "").replace('×', '*').replace('−', '-').replace('–', '-')
    private val NUMBER = Regex("\\d+(\\.\\d+)?")

    /**
     * Checks a note against the text of the pages it cites. Numbers and formulas must literally appear in the source
     * (whitespace/unicode-dash tolerant); definitions must share most of their terms with it. Failures are flagged,
     * never silently fixed, so the reader is pointed at the page rather than handed a confident wrong answer.
     */
    fun verify(note: StructuredNote, source: String): StructuredNote {
        val flat = squash(source)
        val sourceTerms = tokenize(source).toSet()
        val flags = mutableListOf<String>()
        var checked = 0
        var passed = 0

        fun check(ok: Boolean, flag: String) { checked++; if (ok) passed++ else flags += flag }

        for (f in note.formulas) check(squash(f) in flat, "Formula not found verbatim on ${note.pageLabel}: $f")
        val prose = (note.definitions + note.traps + listOf(note.example, note.summary, note.flow)).filter { it.isNotBlank() }
        for (text in prose) {
            for (n in NUMBER.findAll(text).map { it.value }.distinct()) check(n in source.replace(",", ""), "Number $n isn't on ${note.pageLabel}")
        }
        for (d in note.definitions) {
            val terms = tokenize(d)
            val share = if (terms.isEmpty()) 1.0 else terms.count { it in sourceTerms }.toDouble() / terms.size
            check(share >= 0.6, "Definition wording not supported by ${note.pageLabel}: ${d.take(60)}")
        }
        val confidence = if (checked == 0) 0.8 else passed.toDouble() / checked
        return note.copy(confidence = confidence, flags = flags)
    }

    /** Verifies each note against only the chunk (page range) it came from. */
    fun verifyAll(notes: List<StructuredNote>, chunks: List<NoteChunk>): List<StructuredNote> =
        notes.mapIndexed { i, n ->
            val src = chunks.getOrNull(i)?.text ?: return@mapIndexed n.copy(confidence = 0.0, flags = listOf("No source chunk to check against"))
            verify(n, src)
        }

    /** Same topic across chunks (e.g. "Cache (cont.)") becomes one note; items are de-duplicated, page range widened. */
    fun merge(notes: List<StructuredNote>): List<StructuredNote> {
        fun key(t: String) = t.lowercase().replace(Regex("\\(cont\\.\\)"), "").replace(Regex("[^a-z0-9]+"), " ").trim()
        val groups = LinkedHashMap<String, MutableList<StructuredNote>>()
        notes.forEach { groups.getOrPut(key(it.topic)) { mutableListOf() }.add(it) }
        return groups.values.map { g ->
            if (g.size == 1) g[0] else StructuredNote(
                topic = g[0].topic.replace(" (cont.)", ""),
                summary = g.firstOrNull { it.summary.isNotBlank() }?.summary ?: "",
                definitions = g.flatMap { it.definitions }.distinct(),
                formulas = g.flatMap { it.formulas }.distinct(),
                traps = g.flatMap { it.traps }.distinct(),
                example = g.firstOrNull { it.example.isNotBlank() }?.example ?: "",
                flow = g.firstOrNull { it.flow.isNotBlank() }?.flow ?: "",
                pageStart = g.minOf { it.pageStart }, pageEnd = g.maxOf { it.pageEnd },
                confidence = g.map { it.confidence }.average(), flags = g.flatMap { it.flags }.distinct()
            )
        }
    }

    /** Detailed notes as readable text; low-confidence parts say so and point at the page. */
    fun renderDetailed(notes: List<StructuredNote>): String = buildString {
        for (n in notes) {
            append("## ${n.topic}  (${n.pageLabel})")
            if (n.lowConfidence) append("  ⚠ check source")
            append('\n')
            if (n.summary.isNotBlank()) append("In short: ${n.summary}\n")
            n.definitions.forEach { append("• Definition: $it\n") }
            n.formulas.forEach { append("• Formula: $it\n") }
            if (n.flow.isNotBlank()) append("• Flow: ${n.flow}\n")
            n.traps.forEach { append("• Trap: $it\n") }
            if (n.example.isNotBlank()) append("• Example: ${n.example}\n")
            n.flags.forEach { append("  ⚠ $it\n") }
            append('\n')
        }
    }.trim()

    /**
     * The one-page revision sheet: a hard character cap, never exceeded. Formulas first (highest value per character),
     * then one-liners, then traps; whole lines are dropped rather than cut mid-sentence.
     */
    fun revisionSheet(notes: List<StructuredNote>, maxChars: Int = 2800): String {
        val lines = mutableListOf<String>()
        for (n in notes) {
            lines += "▸ ${n.topic} (${n.pageLabel})${if (n.lowConfidence) " ⚠" else ""}"
            n.formulas.take(2).forEach { lines += "   = $it" }
            val line = n.summary.ifBlank { n.definitions.firstOrNull() ?: "" }
            if (line.isNotBlank()) lines += "   ${line.take(140)}"
            n.traps.firstOrNull()?.let { lines += "   ✗ ${it.take(120)}" }
        }
        val sb = StringBuilder()
        for (l in lines) {
            if (sb.length + l.length + 1 > maxChars) break
            sb.append(l).append('\n')
        }
        return sb.toString().trimEnd()
    }
}
