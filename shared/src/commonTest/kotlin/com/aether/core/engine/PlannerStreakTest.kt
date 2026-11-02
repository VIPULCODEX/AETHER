package com.aether.core.engine

import com.aether.core.model.ALL_DAYS
import com.aether.core.model.Routine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlannerStreakTest {
    private fun routine(id: String, minutes: Int, fixed: Int? = null, days: Int = ALL_DAYS, title: String = id) =
        Routine(id, title, "GATE", minutes, days, fixed, 0)

    private fun minutes(label: String): Pair<Int, Int> {
        fun m(t: String): Int {
            val (h, rest) = t.trim().split(":")
            return ((h.toInt() % 12) + if (rest.contains("PM")) 12 else 0) * 60 + rest.take(2).toInt()
        }
        return m(label.substringBefore("-")) to m(label.substringAfter("-"))
    }

    @Test fun fixedBlocksStayPutAndNothingOverlaps() {
        val college = routine("college", 7 * 60, fixed = 9 * 60, days = 0b0011111)
        val study = routine("study", 120)
        val gym = routine("gym", 45)
        val plan = TimetablePlanner().plan(listOf(college, study, gym), 7, 23, peakStartHour = 17)
        assertTrue(plan.unplaced.isEmpty())
        for (day in 0..6) {
            val ranges = plan.slots.filter { it.dayOfWeek == day }.map { minutes(it.timeLabel) }.sortedBy { it.first }
            ranges.zipWithNext().forEach { (a, b) -> assertTrue(a.second <= b.first, "overlap on day $day: $a $b") }
        }
        val monday = plan.slots.filter { it.dayOfWeek == 0 }
        assertEquals("9:00 AM - 4:00 PM", monday.first { it.activityLabel == "college" }.timeLabel)
        assertEquals(2, plan.slots.count { it.dayOfWeek == 6 })
    }

    @Test fun heavyRoutineLandsInPeakWindow() {
        val plan = TimetablePlanner().plan(listOf(routine("deep", 120)), 7, 23, peakStartHour = 18)
        val (start, _) = minutes(plan.slots.first().timeLabel)
        assertTrue(start in 18 * 60 until 21 * 60, "start=$start")
    }

    @Test fun unfittableRoutineIsReportedNotDropped() {
        val plan = TimetablePlanner().plan(listOf(routine("wall", 15 * 60, fixed = 7 * 60), routine("extra", 120)), 7, 23, null)
        assertTrue(plan.unplaced.any { it.startsWith("extra") })
    }

    @Test fun slotLabelsAreAlarmParsable() {
        val plan = TimetablePlanner().plan(listOf(routine("a", 30, fixed = 0 * 60 + 30)), 0, 24, null)
        assertTrue(plan.slots.all { Regex("^\\d{1,2}:\\d{2} (AM|PM) - \\d{1,2}:\\d{2} (AM|PM)$").matches(it.timeLabel) })
    }

    @Test fun streakCountsConsecutiveDaysAndSurvivesUnfinishedToday() {
        val active = setOf("2026-11-01", "2026-10-31", "2026-10-30", "2026-10-20", "2026-10-19")
        val s = StreakEngine.compute(active, today = "2026-11-02")
        assertEquals(3, s.current)
        assertFalse(s.todayDone)
        assertEquals(3, s.best)
        val done = StreakEngine.compute(active + "2026-11-02", today = "2026-11-02")
        assertEquals(4, done.current)
        assertTrue(done.todayDone)
    }

    @Test fun streakBreaksAfterAMissedDay() {
        val s = StreakEngine.compute(setOf("2026-10-28", "2026-10-29"), today = "2026-11-02")
        assertEquals(0, s.current)
        assertEquals(2, s.best)
    }

    @Test fun epochDayHandlesMonthAndLeapBoundaries() {
        assertEquals(0L, StreakEngine.epochDay("1970-01-01"))
        assertEquals(1L, StreakEngine.epochDay("1970-01-02"))
        assertEquals(StreakEngine.epochDay("2024-03-01")!! - 1, StreakEngine.epochDay("2024-02-29"))
        assertEquals(StreakEngine.epochDay("2026-01-01")!! - 1, StreakEngine.epochDay("2025-12-31"))
        assertEquals(null, StreakEngine.epochDay("nonsense"))
    }
}
