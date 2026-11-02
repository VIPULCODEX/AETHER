package com.aether.core.engine

import com.aether.core.model.Routine
import com.aether.core.model.ScheduleSlot
import kotlin.math.max
import kotlin.math.min

data class PlanResult(val slots: List<ScheduleSlot>, val unplaced: List<String>)

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private const val GRID = 15
private const val BUFFER = 10
private const val HEAVY_MINUTES = 60

/**
 * Builds the weekly timetable from the user's routines, deterministically and
 * on-device. Fixed commitments (college, office) are placed exactly where they
 * are and carve out busy time; each flexible routine is then fitted into the
 * remaining waking hours, longest first. Long, demanding routines are steered
 * into the person's peak-focus window; short ones take the earliest gap. A
 * routine that can't fit is reported rather than silently dropped.
 */
class TimetablePlanner {

    fun plan(routines: List<Routine>, wakeHour: Int, sleepHour: Int, peakStartHour: Int?, peakHours: Int = 3): PlanResult {
        val awakeStart = wakeHour * 60
        val awakeEnd = if (sleepHour * 60 > awakeStart) sleepHour * 60 else 24 * 60
        val peak = (peakStartHour ?: 9) * 60 to ((peakStartHour ?: 9) + peakHours) * 60

        val slots = mutableListOf<ScheduleSlot>()
        val unplaced = mutableListOf<String>()

        for (day in 0..6) {
            val todays = routines.filter { !it.archived && it.occursOn(day) }
            var free = listOf(awakeStart to awakeEnd)

            todays.filter { it.isFixed }.forEach { r ->
                val start = r.fixedStartMinute!!
                slots += slot(day, r, start)
                free = subtract(free, start, start + r.minutes)
            }

            todays.filter { !it.isFixed }.sortedByDescending { it.minutes }.forEach { r ->
                val heavy = r.minutes >= HEAVY_MINUTES
                var bestStart = -1
                var bestScore = Double.NEGATIVE_INFINITY
                for ((a, b) in free) {
                    var s = ((a + GRID - 1) / GRID) * GRID
                    while (s + r.minutes <= b) {
                        val overlap = max(0, min(s + r.minutes, peak.second) - max(s, peak.first))
                        val score = (if (heavy) overlap.toDouble() else 0.0) - s / 100_000.0
                        if (score > bestScore) { bestScore = score; bestStart = s }
                        s += GRID
                    }
                }
                if (bestStart < 0) {
                    unplaced += "${r.title} (${DAY_NAMES[day]})"
                } else {
                    slots += slot(day, r, bestStart)
                    free = subtract(free, bestStart, bestStart + r.minutes + BUFFER)
                }
            }
        }
        return PlanResult(slots.sortedWith(compareBy({ it.dayOfWeek }, { startOf(it.timeLabel) })), unplaced)
    }

    private fun slot(day: Int, r: Routine, start: Int) = ScheduleSlot(
        // "fx_" marks a fixed commitment: shown on the timetable but silent (you are already there).
        id = "${if (r.isFixed) "fx" else "rt"}_${day}_${r.id}",
        dayOfWeek = day,
        timeLabel = "${clock(start)} - ${clock(start + r.minutes)}",
        activityLabel = r.title,
        domain = r.domain
    )

    private fun subtract(free: List<Pair<Int, Int>>, from: Int, to: Int): List<Pair<Int, Int>> =
        free.flatMap { (a, b) ->
            if (to <= a || from >= b) listOf(a to b)
            else listOfNotNull(if (from > a) a to from else null, if (to < b) to to b else null)
        }

    private fun startOf(label: String): Int {
        val t = label.substringBefore("-").trim()
        val (h, rest) = t.split(":")
        val m = rest.take(2).toInt()
        val pm = rest.contains("PM")
        return ((h.toInt() % 12) + if (pm) 12 else 0) * 60 + m
    }

    /** "7:00 AM" — the format AlarmScheduler parses. */
    private fun clock(minute: Int): String {
        val h24 = (minute / 60) % 24
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "$h12:${(minute % 60).toString().padStart(2, '0')} ${if (h24 < 12) "AM" else "PM"}"
    }
}
