package com.aether.core.engine

import com.aether.core.model.BehaviorEvent
import com.aether.core.model.BehaviorEventType
import com.aether.core.model.Chronotype
import com.aether.core.model.CoachTone
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Modules
import com.aether.core.model.Persona
import com.aether.core.model.PersonalizationPrefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PersonalizationTest {
    private val now = 100 * DAY + 9 * 3_600_000L
    private val hourOf = { t: Long -> ((t / 3_600_000L) % 24).toInt() }
    private val goals = listOf(goal("a", "Research"), goal("b", "Gym"))
    private val done = (1..8).map { task("d$it", "a", true, now - it * DAY + 1_800_000L) }
    private val open = listOf(task("o1", "a", effort = 4), task("o2", "b", effort = 2))

    private fun learned() = PersonalizationEngine().learn(done + open, goals, emptyList(), emptyList(), now, hourOf)

    @Test fun learnsPeakWindowAndChronotype() {
        val l = learned()
        assertNotNull(l.peakStartHour)
        assertTrue(l.isPeakHour(9))
        assertEquals(Chronotype.MORNING, l.chronotype)
    }

    @Test fun confidenceGrowsWithDataAndStartsAtZero() {
        assertEquals(0.0, PersonalizationEngine().learn(emptyList(), emptyList(), emptyList(), emptyList(), now, hourOf).confidence)
        assertTrue(learned().confidence in 0.2..0.6)
    }

    @Test fun repeatedDismissalsLowerAffinity() {
        val events = (1..5).map { BehaviorEvent("e$it", now, BehaviorEventType.SUGGESTION_DISMISSED, "Gym", 9) }
        val stat = PersonalizationEngine().learn(done + open, goals, emptyList(), events, now, hourOf).domainStats.getValue("Gym")
        assertTrue(stat.acceptRate < 0.3)
    }

    @Test fun sleepEnergyInsightNeedsEnoughSamples() {
        val few = listOf(DailyCheckIn("1", 3, 5, 8.0, true))
        assertTrue(PersonalizationEngine().learn(emptyList(), emptyList(), few, emptyList(), now, hourOf).insights.isEmpty())
    }

    private fun ctx(prefs: PersonalizationPrefs = PersonalizationPrefs(), l: LearnedProfile = learned(), hour: Int = 9, tasks: List<com.aether.core.model.Task> = open) =
        SuggestionContext(now, hour, DailyCheckIn("t", 3, 4, 7.0, false), goals, tasks, prefs, l)

    @Test fun suggestsHeavyTaskInPeakWindowWithGroundedReason() {
        val s = ContextEngine().suggestNow(ctx())
        assertEquals("o1", s.taskId)
        assertTrue("peak window" in s.reason)
    }

    @Test fun worksColdStartWithoutLearning() {
        assertNotNull(ContextEngine().suggestNow(ctx(l = LearnedProfile.EMPTY)).taskId)
    }

    @Test fun skippedItemsAreNotRepeated() {
        val s = ContextEngine().suggestNow(ctx().copy(skipped = setOf("o1")))
        assertNotEquals("o1", s.taskId)
    }

    @Test fun disabledModuleNeverSuggested() {
        val prefs = PersonalizationPrefs(enabledModules = setOf(Modules.RESEARCH))
        val s = ContextEngine().suggestNow(ctx(prefs).copy(skipped = setOf("o1")))
        assertNotEquals("Gym", s.domain)
    }

    @Test fun outsideWakingHoursSuggestsWindDownUnlessOverdue() {
        assertEquals("Wind down", ContextEngine().suggestNow(ctx(hour = 2)).title)
        val overdue = ctx(hour = 2, tasks = listOf(task("od", "a", due = now - DAY)))
        assertEquals("od", ContextEngine().suggestNow(overdue).taskId)
    }

    @Test fun toneChangesPhrasing() {
        val gentle = ContextEngine().suggestNow(ctx(PersonalizationPrefs(tone = CoachTone.GENTLE))).reason
        val data = ContextEngine().suggestNow(ctx(PersonalizationPrefs(tone = CoachTone.DATA))).reason
        assertTrue(gentle.startsWith("A good fit"))
        assertTrue(data.startsWith("Score"))
    }

    @Test fun nightOwlWakingHoursWrapAround() {
        val owl = PersonalizationPrefs(wakeHour = 14, sleepHour = 4)
        assertTrue(owl.isAwake(2) && owl.isAwake(20))
        assertFalse(owl.isAwake(9))
    }

    @Test fun personaSetsDefaultsButStaysOverridable() {
        val p = PersonalizationPrefs().withPersona(Persona.ATHLETE)
        assertTrue(Modules.GYM in p.enabledModules && p.onboarded)
    }
}
