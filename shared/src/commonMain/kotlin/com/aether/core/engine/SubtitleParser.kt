package com.aether.core.engine

data class Cue(val startMs: Long, val endMs: Long, val text: String)

/**
 * Turns a subtitle file (.srt or .vtt) into cues and into study text. The transcript is what makes a video a
 * Study Hub source: summaries, cited answers, flashcards and practice then work on what was said in the video,
 * and the cue times let the player jump straight to it.
 */
object SubtitleParser {

    private val TIME = Regex("(?:(\\d{1,2}):)?(\\d{2}):(\\d{2})[.,](\\d{1,3})")
    private val ARROW = Regex("(\\S+)\\s+-->\\s+(\\S+)")

    fun parse(raw: String): List<Cue> {
        val cues = mutableListOf<Cue>()
        val blocks = raw.replace("\r\n", "\n").replace('\r', '\n').removePrefix("\uFEFF").split(Regex("\n{2,}"))
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val at = lines.indexOfFirst { ARROW.containsMatchIn(it) }
            if (at < 0) continue
            val m = ARROW.find(lines[at])!!
            val start = ms(m.groupValues[1]) ?: continue
            val end = ms(m.groupValues[2]) ?: continue
            val text = lines.drop(at + 1).joinToString(" ") { clean(it) }.replace(Regex("\\s+"), " ").trim()
            if (text.isNotEmpty()) cues += Cue(start, end, text)
        }
        return cues.sortedBy { it.startMs }
    }

    /** Clean study text: cues joined into paragraphs (a pause or a long run starts a new one), each ending in punctuation. */
    fun toStudyText(cues: List<Cue>, pauseMs: Long = 2500, maxChars: Int = 420): String {
        val paragraphs = mutableListOf<StringBuilder>()
        var lastEnd = Long.MIN_VALUE
        for (cue in cues) {
            val current = paragraphs.lastOrNull()
            val startNew = current == null || cue.startMs - lastEnd > pauseMs || current.length + cue.text.length > maxChars
            if (startNew) paragraphs += StringBuilder(cue.text) else current!!.append(' ').append(cue.text)
            lastEnd = cue.endMs
        }
        return paragraphs.joinToString("\n\n") { p -> p.toString().trim().let { if (it.last() in ".!?") it else "$it." } }
    }

    /** One "startMs|text" line per cue, stored with the source so the player can show and seek a transcript. */
    fun encode(cues: List<Cue>): String = cues.joinToString("\n") { "${it.startMs}|${it.text}" }

    fun decode(stored: String?): List<Cue> =
        stored.orEmpty().lines().mapNotNull { line ->
            val start = line.substringBefore('|').toLongOrNull() ?: return@mapNotNull null
            Cue(start, start, line.substringAfter('|'))
        }

    private fun ms(token: String): Long? {
        val m = TIME.matchEntire(token) ?: return null
        val h = m.groupValues[1].ifEmpty { "0" }.toLong()
        val min = m.groupValues[2].toLong()
        val s = m.groupValues[3].toLong()
        val frac = m.groupValues[4].padEnd(3, '0').take(3).toLong()
        return ((h * 60 + min) * 60 + s) * 1000 + frac
    }

    private fun clean(line: String): String =
        line.replace(Regex("<[^>]+>"), "").replace(Regex("\\{\\\\[^}]*}"), "").replace("&amp;", "&").replace("&nbsp;", " ")
            .replace(Regex("^[-\u2013] "), "").trim()
}
