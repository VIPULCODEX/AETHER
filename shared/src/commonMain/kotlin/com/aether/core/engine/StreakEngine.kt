package com.aether.core.engine

data class StreakStats(val current: Int, val best: Int, val last7: Int, val todayDone: Boolean)

/**
 * Streaks over a set of "active" dates (yyyy-MM-dd). Today not being done yet
 * doesn't break the streak: it stays alive until the day actually ends.
 * Pure arithmetic on epoch days, so it works in common code with no date library.
 */
object StreakEngine {

    fun compute(activeDates: Set<String>, today: String): StreakStats {
        val todayDay = epochDay(today) ?: return StreakStats(0, 0, 0, false)
        val days = activeDates.mapNotNull(::epochDay).toSet()
        val todayDone = todayDay in days

        var cursor = if (todayDone) todayDay else todayDay - 1
        var current = 0
        while (cursor in days) { current++; cursor-- }

        var best = 0
        var run = 0
        var previous: Long? = null
        for (d in days.sorted()) {
            run = if (previous != null && d == previous + 1) run + 1 else 1
            best = maxOf(best, run)
            previous = d
        }
        val last7 = (0..6).count { todayDay - it in days }
        return StreakStats(current, maxOf(best, current), last7, todayDone)
    }

    /** Days since 1970-01-01 for an ISO date, or null if malformed (Howard Hinnant's civil-days algorithm). */
    fun epochDay(iso: String): Long? {
        val parts = iso.split("-")
        if (parts.size != 3) return null
        var y = parts[0].toLongOrNull() ?: return null
        val m = parts[1].toLongOrNull() ?: return null
        val d = parts[2].toLongOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        if (m <= 2) y -= 1
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }
}
