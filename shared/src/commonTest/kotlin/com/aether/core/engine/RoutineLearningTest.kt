package com.aether.core.engine

import com.aether.core.model.ALL_DAYS
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.PersonalizationPrefs
import com.aether.core.model.Routine
import com.aether.core.model.RoutineLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoutineLearningTest {
    private val hourOf = { t: Long -> ((t / 3_600_000L) % 24).toInt() }
    private val gate = Routine("r1", "GATE revision", "GATE", 120, ALL_DAYS, null, 0)
    private val gym = Routine("r2", "Gym", "Gym", 45, ALL_DAYS, null, 0)
    private val dayMillis = 86_400_000L

    // 2026-11-02 is a Monday; build an epoch-ms for "N days ago at 06:30 UTC"
    private val todayEpoch = StreakEngine.epochDay("2026-11-02")!!
    private val now = todayEpoch * dayMillis + 8 * 3_600_000L
    private fun iso(back: Int): String {
        // inverse of epochDay for the dates we use: only a handful of days in Oct/Nov 2026
        val d = 2 - back
        return if (d >= 1) "2026-11-0$d" else "2026-10-${31 + d}"
    }
    private fun tick(routine: Routine, back: Int, hour: Int) =
        RoutineLog(routine.id, iso(back), (todayEpoch - back) * dayMillis + hour * 3_600_000L)

    @Test fun routineTicksAloneProduceObservationsAndAPeakWindow() {
        val logs = (0..6).map { tick(gate, it, hour = 6) } + (0..2).map { tick(gym, it, hour = 18) }
        val signals = routineSignals(listOf(gate, gym), logs, "2026-11-02")
        val learned = PersonalizationEngine().learn(emptyList(), emptyList(), emptyList(), emptyList(), now, hourOf, signals)
        assertEquals(10, learned.routineCompletions)
        assertEquals(10, learned.observations)
        assertNotNull(learned.peakStartHour)
        assertTrue(learned.isPeakHour(6), "peak=${learned.peakStartHour}")
    }

    @Test fun followThroughReflectsScheduledVersusDone() {
        val logs = (0..6).map { tick(gate, it, 6) }              // GATE: 7 of 14 done
        val signals = routineSignals(listOf(gate, gym), logs, "2026-11-02")
        assertEquals(14, signals.scheduledByDomain["GATE"])
        assertEquals(7, signals.doneByDomain["GATE"])
        val learned = PersonalizationEngine().learn(emptyList(), emptyList(), emptyList(), emptyList(), now, hourOf, signals)
        val gateStat = learned.domainStats.getValue("GATE")
        val gymStat = learned.domainStats.getValue("Gym")
        assertTrue(gateStat.completionRate > gymStat.completionRate)   // Gym was scheduled 14x and never done
    }

    @Test fun fixedCommitmentsNeverCountAsFollowThrough() {
        val college = Routine("c", "College", "College", 420, ALL_DAYS, 9 * 60, 0)
        val signals = routineSignals(listOf(college), emptyList(), "2026-11-02")
        assertTrue(signals.scheduledByDomain.isEmpty())
    }

    @Test fun dayOfWeekMathMatchesTheCalendar() {
        // Monday 2026-11-02 only: a Monday-only routine is scheduled exactly twice in 14 days (Nov 2 and Oct 26).
        val mondays = Routine("m", "Mon only", "X", 30, 0b0000001, null, 0)
        assertEquals(2, routineSignals(listOf(mondays), emptyList(), "2026-11-02").scheduledByDomain["X"])
    }

    @Test fun anOpenRoutineBecomesTheSuggestionWithHonestReason() {
        val ctx = SuggestionContext(
            nowMillis = now, hourOfDay = 8, todayCheckIn = DailyCheckIn("t", 3, 4, 7.0, false),
            goals = emptyList(), openTasks = emptyList(), prefs = PersonalizationPrefs(), pendingRoutines = listOf(gate, gym)
        )
        val s = ContextEngine().suggestNow(ctx)
        assertEquals("r1", s.routineId)              // the longer routine, at a good hour, with high energy
        assertTrue(s.title.startsWith("Do: "))
        assertTrue("routine" in s.reason.lowercase())
        val next = ContextEngine().suggestNow(ctx.copy(skipped = setOf("rt-r1")))
        assertEquals("r2", next.routineId)
    }

    @Test fun lowEnergyPrefersTheLightRoutine() {
        val ctx = SuggestionContext(
            nowMillis = now, hourOfDay = 10, todayCheckIn = DailyCheckIn("t", 2, 1, 5.0, false),
            goals = emptyList(), openTasks = emptyList(), prefs = PersonalizationPrefs(), pendingRoutines = listOf(gate, gym)
        )
        assertEquals("r2", ContextEngine().suggestNow(ctx).routineId)
    }

    @Test fun studyReviewsTeachWhenYouStudyWithoutPollutingDomains() {
        val events = (0..7).map { i ->
            com.aether.core.model.BehaviorEvent("s$i", now - i * dayMillis, com.aether.core.model.BehaviorEventType.STUDY_REVIEW, "Study", 21)
        }
        val learned = PersonalizationEngine().learn(emptyList(), emptyList(), emptyList(), events, now, hourOf)
        assertEquals(8, learned.studyReviews)
        assertEquals(0, learned.feedbackEvents)
        assertTrue(learned.isPeakHour(21), "peak=${learned.peakStartHour}")
        assertTrue(learned.domainStats.isEmpty())
    }
}
