package com.aether.core.engine

/**
 * One block from the user's own timetable. [startMinute] is null for duration-only tables ("GATE | 1 hour"),
 * which the planner then fits in itself. Breaks and meals stay on the timetable but are not ticked or alarmed.
 */
data class ImportedBlock(val title: String, val startMinute: Int?, val minutes: Int, val domain: String) {
    val isBreak: Boolean get() = domain == BREAK_DOMAIN
}

/** One table found in the pasted text. [wakeMinute]/[sleepMinute] come from its sleep row, if it has one. */
data class ImportedTimetable(
    val title: String,
    val blocks: List<ImportedBlock>,
    val wakeMinute: Int? = null,
    val sleepMinute: Int? = null
)

/** A "Day | ..." table: what each weekday (0 = Monday) is for. */
data class WeekFocus(val title: String, val byDay: Map<Int, String>)

data class ImportResult(val timetables: List<ImportedTimetable>, val weeks: List<WeekFocus>, val rules: List<String>) {
    val isEmpty: Boolean get() = timetables.isEmpty() && weeks.isEmpty() && rules.isEmpty()
}

const val BREAK_DOMAIN = "Break"

/**
 * Reads a timetable the user already made (markdown, tab-separated or plain lines) so AETHER follows
 * their plan instead of inventing one. Deterministic and on-device; whatever it can't read is left out
 * and the user sees the result before anything is saved.
 */
object TimetableImporter {

    private const val TIME = """(\d{1,2})(?:[:.](\d{2}))?\s*([AaPp]\.?[Mm]\.?)?"""
    private val RANGE = Regex("""^$TIME\s*(?:-|–|—|\bto\b)\s*$TIME(?![\d])""")
    private val SINGLE = Regex("""^(\d{1,2})(?:[:.](\d{2})\s*([AaPp]\.?[Mm]\.?)?|()\s*([AaPp]\.?[Mm]\.?))(?![\d])""")
    private val DURATION = Regex(
        """^(?:(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hour|hours)\b)?\s*(?:(\d+)\s*(?:m|min|mins|minutes)\b)?$""",
        RegexOption.IGNORE_CASE
    )
    private val BULLET = Regex("""^(?:[*•+-]|\d+[.)])\s+""")
    private val DAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    private data class Clock(val hour: Int, val minute: Int, val pm: Boolean?) {
        /** Minutes of the day this could mean: one if AM/PM (or 24 h) says so, otherwise both halves. */
        fun candidates(): List<Int> = when {
            pm != null -> listOf((hour % 12 + if (pm) 12 else 0) * 60 + minute)
            hour == 0 || hour > 12 -> listOf(hour * 60 + minute)
            else -> listOf((hour % 12) * 60 + minute, (hour % 12 + 12) * 60 + minute)
        }
    }

    private data class Row(val start: Clock, val end: Clock?, var label: String)

    fun parse(text: String): ImportResult {
        val timetables = mutableListOf<ImportedTimetable>()
        val weeks = mutableListOf<WeekFocus>()
        val rules = mutableListOf<String>()

        var heading = ""
        var blockTitle = ""
        val timed = mutableListOf<Row>()
        val durations = mutableListOf<ImportedBlock>()
        val days = linkedMapOf<Int, String>()
        var inRules = false

        fun flush() {
            if (timed.size >= 2) resolve(blockTitle, timed.toList())?.let { timetables += it }
            if (durations.size >= 2) timetables += ImportedTimetable(blockTitle.ifBlank { "Daily targets" }, durations.toList())
            if (days.size >= 3) weeks += WeekFocus(blockTitle.ifBlank { "Weekly plan" }, days.toMap())
            timed.clear(); durations.clear(); days.clear()
        }

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val tabular = line.startsWith("|") || line.contains('\t')
            val cells = (if (tabular) line.trim('|').split('|', '\t') else listOf(line))
                .map { clean(it) }.filter { it.isNotEmpty() }
            if (cells.isEmpty() || cells.all { c -> c.all { it == '-' || it == ':' || it == ' ' } }) continue
            val first = cells.first()

            val day = DAYS.indexOfFirst { d -> first.lowercase().let { it == d || it == d.take(3) } }
            if (day >= 0 && cells.size > 1) {
                if (days.isEmpty()) { flush(); blockTitle = heading }
                days[day] = cells.drop(1).joinToString(" · ")
                inRules = false
                continue
            }

            val stripped = first.replace(BULLET, "")
            val range = RANGE.find(stripped)
                // "1–3" is a day count in a study plan, not a time: require ":" or AM/PM somewhere.
                ?.takeIf { m -> m.value.contains(':') || m.value.contains('.') || m.groupValues[3].isNotEmpty() || m.groupValues[6].isNotEmpty() }
            val single = if (range == null) SINGLE.find(stripped) else null
            val match = range ?: single
            if (match != null) {
                if (timed.isEmpty()) { flush(); blockTitle = heading }
                val g = match.groupValues
                val start = if (range != null) clockOf(g[1], g[2], g[3]) else clockOf(g[1], g[2], g[3].ifEmpty { g[5] })
                val end = if (range != null) clockOf(g[4], g[5], g[6]) else null
                val rest = if (cells.size > 1) cells[1] else stripped.substring(match.range.last + 1).trim(' ', '-', '–', '—', ':')
                if (start != null) timed += Row(start, end, rest)
                inRules = false
                continue
            }

            // A time on its own line, with the activity on the next one (how some apps copy tables).
            if (!tabular && timed.isNotEmpty() && timed.last().label.isEmpty()) {
                timed.last().label = first
                continue
            }

            if (tabular) {
                val minutes = cells.drop(1).lastOrNull()?.let { durationMinutes(it) }
                if (minutes != null) {
                    if (durations.isEmpty()) { flush(); blockTitle = heading }
                    val label = if (cells.size >= 3) cells[cells.size - 2] else first
                    durations += ImportedBlock(label, null, minutes, domainOf(label))
                }
                continue // header rows and tables of other things are ignored, and never become titles
            }

            if (inRules && BULLET.containsMatchIn(line)) {
                rules += clean(line.replace(BULLET, ""))
                continue
            }
            // Anything else is prose: it ends the current table and may title the next one.
            val lower = first.lowercase()
            if (lower.startsWith("time ") || lower == "time" || lower == "activity") continue
            flush()
            heading = first.trimStart('#', ' ').take(80)
            inRules = Regex("""\b(rules?|niyam)\b""", RegexOption.IGNORE_CASE).containsMatchIn(heading)
        }
        flush()
        return ImportResult(timetables, weeks, rules.distinct().take(8))
    }

    /** Fills in the missing AM/PM by reading rows in order: each one starts at or after the one before. */
    private fun resolve(title: String, rows: List<Row>): ImportedTimetable? {
        val starts = IntArray(rows.size)
        val ends = arrayOfNulls<Int>(rows.size)
        var previous = -1
        rows.forEachIndexed { i, row ->
            val options = row.start.candidates().toMutableList()
            val end = row.end
            // "7:00–9:30 AM": the one AM/PM belongs to both ends, unless that would put the start after the end.
            if (row.start.pm == null && end?.pm != null) {
                val e = end.candidates().first()
                val same = row.start.copy(pm = end.pm).candidates().first()
                options.clear()
                options += if (same < e) same else row.start.copy(pm = !end.pm).candidates().first()
            }
            starts[i] = when {
                options.size == 1 -> options[0]
                previous < 0 -> if (row.start.hour in 5..11) options[0] else options[1]
                else -> options.minByOrNull { (it - previous + 1440) % 1440 }!!
            }
            ends[i] = end?.candidates()?.minByOrNull { (it - starts[i] + 1439) % 1440 }
            previous = ends[i] ?: starts[i]
        }

        var wake: Int? = null
        var sleep: Int? = null
        val blocks = mutableListOf<ImportedBlock>()
        rows.forEachIndexed { i, row ->
            val label = row.label.ifBlank { return@forEachIndexed }
            val lower = label.lowercase()
            if (Regex("""^(sleep|sone|bed ?time|neend)""").containsMatchIn(lower)) {
                sleep = starts[i]
                if (ends[i] != null) wake = ends[i]
                return@forEachIndexed
            }
            if (lower.startsWith("wake") || lower.startsWith("uth")) wake = starts[i]
            val next = ends[i] ?: if (i + 1 < rows.size) starts[i + 1] else starts[i] + 30
            val length = ((next - starts[i] + 1440) % 1440).let { if (it == 0) 30 else it }
            blocks += ImportedBlock(label, starts[i], length.coerceAtMost(720), domainOf(label))
        }
        return if (blocks.isEmpty()) null else ImportedTimetable(title.ifBlank { "My timetable" }, blocks, wake, sleep)
    }

    private fun clockOf(h: String, m: String, ampm: String): Clock? {
        val hour = h.toIntOrNull() ?: return null
        val minute = if (m.isEmpty()) 0 else m.toIntOrNull() ?: return null
        if (hour > 23 || minute > 59) return null
        return Clock(hour, minute, if (ampm.isEmpty()) null else ampm.lowercase().startsWith("p"))
    }

    private fun durationMinutes(cell: String): Int? {
        val m = DURATION.find(cell.trim()) ?: return null
        val hours = m.groupValues[1].toDoubleOrNull()
        val mins = m.groupValues[2].toIntOrNull()
        if (hours == null && mins == null) return null
        return ((hours ?: 0.0) * 60 + (mins ?: 0)).toInt().takeIf { it in 5..720 }
    }

    private fun clean(s: String) = s.replace("**", "").replace("\\.", ".").replace("\\", "").trim()

    private val KEYWORDS = listOf(
        "wake" to "Health",
        "breakfast|lunch|dinner|break|relax|freshen|tea\\b|nashta|khana|rest\\b|free time|plan tomorrow" to BREAK_DOMAIN,
        "badminton|cricket|gym|walk|workout|run\\b|running|yoga|football|swim|physical" to "Gym",
        "guitar|music|hobby|friends|outing|reading|meditat" to "Personal",
        "internship|interview|resume|coding|dsa practice" to "Career",
        "gate\\b" to "GATE",
        "college|assignment|lecture|class|lab\\b|ta work" to "College Work",
        "research|paper writing|manuscript|experiment|thesis" to "Research",
        "net\\b|pyq|mock|revision|study|exam|syllabus|paper|drdo|maths|aptitude" to "Academic"
    ).map { (words, domain) -> Regex("\\b(?:$words)") to domain }

    /** Earliest mention wins ("Internship preparation / research work" is internship); waking and breaks are checked first. */
    fun domainOf(label: String): String {
        val lower = label.lowercase()
        KEYWORDS.take(2).firstOrNull { it.first.containsMatchIn(lower) }?.let { return it.second }
        // "College work / TA work / ... / research" is college; "NET Paper 2" is exam study, not research.
        return KEYWORDS.drop(2)
            .mapNotNull { (regex, domain) -> regex.find(lower)?.let { it.range.first to domain } }
            .minByOrNull { it.first }?.second ?: "General"
    }
}
