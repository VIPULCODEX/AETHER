package com.aether.core.engine

/** A timetable from the user's own plan that takes over on [startDate] (yyyy-MM-dd). */
data class Phase(
    val startDate: String,
    val title: String,
    val blocks: List<ImportedBlock>,
    val wakeMinute: Int? = null,
    val sleepMinute: Int? = null
)

/** Which timetable applies when, and how phase start dates are read from the user's headings. */
object PhaseSchedule {

    private const val MONTHS = "jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec"
    private val DATE = Regex("""\b(\d{1,2})(?:st|nd|rd|th)?\s+($MONTHS)[a-z]*\.?(?:\s+(\d{4}))?""", RegexOption.IGNORE_CASE)

    /** The latest phase that has started by [today], and the ones still to come. */
    fun split(phases: List<Phase>, today: String): Pair<Phase?, List<Phase>> {
        val (started, later) = phases.sortedBy { it.startDate }.partition { it.startDate <= today }
        return started.lastOrNull() to later
    }

    /**
     * A start date suggested by a heading: "25 November onward" starts then; "Until 14 December" has no start,
     * but an "After ..." heading following it starts the next day. Null when the heading says neither.
     */
    fun suggestStart(title: String, previousTitles: List<String>, today: String): String? {
        val lower = title.lowercase()
        if (lower.startsWith("after")) {
            val until = previousTitles.asReversed().firstOrNull { it.lowercase().contains("until") } ?: return null
            val end = dateIn(until, today) ?: return null
            return isoOf((StreakEngine.epochDay(end) ?: return null) + 1)
        }
        if (lower.contains("until") || lower.contains("till")) return null
        return dateIn(title, today)
    }

    /** First "14 December [2026]" in [text]. Without a year, the occurrence nearest ahead (up to 60 days back). */
    private fun dateIn(text: String, today: String): String? {
        val m = DATE.find(text) ?: return null
        val day = m.groupValues[1].toInt()
        val month = MONTHS.split('|').indexOf(m.groupValues[2].lowercase()) + 1
        if (day !in 1..31) return null
        fun iso(year: Int) = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        m.groupValues[3].toIntOrNull()?.let { return iso(it) }
        val now = StreakEngine.epochDay(today) ?: return null
        val year = today.take(4).toInt()
        return (year - 1..year + 1).map(::iso).firstOrNull { (StreakEngine.epochDay(it) ?: return null) >= now - 60 }
    }

    /** Inverse of [StreakEngine.epochDay] (Howard Hinnant's civil_from_days). */
    private fun isoOf(epochDay: Long): String {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val y = yoe + era * 400 + if (m <= 2) 1 else 0
        return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    // Stored as text in preferences: a "P" line per phase followed by a "B" line per block, tab-separated.
    fun encode(phases: List<Phase>): String = phases.joinToString("\n") { p ->
        val head = listOf("P", p.startDate, safe(p.title), p.wakeMinute?.toString().orEmpty(), p.sleepMinute?.toString().orEmpty())
        val blocks = p.blocks.map { listOf("B", safe(it.title), it.startMinute?.toString().orEmpty(), it.minutes.toString(), it.domain) }
        (listOf(head) + blocks).joinToString("\n") { it.joinToString("\t") }
    }

    fun decode(text: String): List<Phase> {
        val phases = mutableListOf<Phase>()
        text.lines().forEach { line ->
            val f = line.split('\t')
            if (f.size < 5) return@forEach
            if (f[0] == "P") phases += Phase(f[1], f[2], emptyList(), f[3].toIntOrNull(), f[4].toIntOrNull())
            else if (f[0] == "B" && phases.isNotEmpty()) {
                val minutes = f[3].toIntOrNull() ?: return@forEach
                val last = phases.last()
                phases[phases.lastIndex] = last.copy(blocks = last.blocks + ImportedBlock(f[1], f[2].toIntOrNull(), minutes, f[4]))
            }
        }
        return phases
    }

    private fun safe(s: String) = s.replace('\t', ' ').replace('\n', ' ')
}
