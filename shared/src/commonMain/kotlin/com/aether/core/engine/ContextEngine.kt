package com.aether.core.engine

import com.aether.core.model.Chronotype
import com.aether.core.model.CoachTone
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.GoalType
import com.aether.core.model.Modules
import com.aether.core.model.PersonalizationPrefs
import com.aether.core.model.Routine
import com.aether.core.model.PlanningStyle
import com.aether.core.model.Task
import kotlin.math.min

/**
 * A suggestion always carries its reason. "What should I do now?" must
 * never get a generic answer — the reason is what proves it's grounded in
 * real, current data (time, energy, deadlines, and this person's own
 * history), not a template. [taskId]/[goalId]/[domain] let the UI send
 * accept/dismiss feedback back to the learning loop.
 */
data class Suggestion(
    val title: String,
    val reason: String,
    val taskId: String? = null,
    val goalId: String? = null,
    val domain: String? = null,
    /** Set when the suggestion is one of today's still-open routines: ticking it is the "done" action. */
    val routineId: String? = null
)

data class SuggestionContext(
    val nowMillis: Long,
    val hourOfDay: Int,
    val todayCheckIn: DailyCheckIn?,
    val goals: List<Goal>,
    val openTasks: List<Task>,
    val prefs: PersonalizationPrefs = PersonalizationPrefs(),
    val learned: LearnedProfile = LearnedProfile.EMPTY,
    /** Ids (task, goal or "rt-<routineId>") the user already said "not now" to this session. */
    val skipped: Set<String> = emptySet(),
    /** Routines that occur today and haven't been ticked yet. */
    val pendingRoutines: List<Routine> = emptyList(),
    val endOfTodayMillis: Long = nowMillis + 12 * 3_600_000L
)

private const val MILLIS_PER_DAY = 86_400_000.0

/** One scored reason a candidate was ranked where it was; the top ones become the sentence the user reads. */
private class Factor(val score: Double, val phrase: String)

private class Candidate(
    val id: String,
    val goalId: String,
    val taskId: String?,
    val title: String,
    val domain: String,
    val dueDate: Long?,
    val effort: Int,
    val impact: Int,
    val routineId: String? = null
)

class ContextEngine {

    fun suggestNow(ctx: SuggestionContext): Suggestion {
        val energy = ctx.todayCheckIn?.energy ?: 3
        val prefs = ctx.prefs

        val urgentOverdue = candidates(ctx).any { it.dueDate != null && it.dueDate < ctx.nowMillis }
        if (!urgentOverdue && (!prefs.isAwake(ctx.hourOfDay) || (energy <= 2 && ctx.hourOfDay >= 21))) {
            return Suggestion(
                title = "Wind down",
                reason = tone(prefs.tone, "Rest is part of the plan — recovery tonight is what moves you forward tomorrow.", "It's outside your waking hours or energy is low: recovery beats another task."),
                domain = null
            )
        }

        val ranked = candidates(ctx)
            .filter { it.id !in ctx.skipped }
            .map { it to factorsFor(it, ctx, energy) }
            .maxByOrNull { (_, factors) -> factors.sumOf { it.score } }

        if (ranked == null) {
            return Suggestion(
                title = "Set today's mission",
                reason = tone(prefs.tone, "Nothing queued yet — a small, clear intention for today is a great start.", "No open goals or tasks. Define one thing that matters today.")
            )
        }

        val (best, factors) = ranked
        val total = factors.sumOf { it.score }
        val phrases = factors.filter { it.score > 0.05 }.sortedByDescending { it.score }.take(3).map { it.phrase }
        val reason = when (prefs.tone) {
            CoachTone.DATA -> "Score ${formatScore(total)} · ${phrases.joinToString(" · ")}"
            CoachTone.GENTLE -> "A good fit right now: ${phrases.joinToString(", ").replaceFirstChar { it.lowercase() }}."
            CoachTone.DIRECT -> phrases.joinToString(". ") + "."
        }.ifBlank { "It's your best open item right now." }

        return Suggestion(
            title = if (best.routineId != null) "Do: ${best.title}" else "Work on: ${best.title}",
            reason = reason,
            taskId = best.taskId,
            goalId = if (best.routineId != null) null else best.goalId,
            domain = best.domain,
            routineId = best.routineId
        )
    }

    private fun candidates(ctx: SuggestionContext): List<Candidate> {
        val goalsById = ctx.goals.associateBy { it.id }
        val enabled = ctx.prefs.enabledModules
        fun allowed(domain: String) = Modules.forDomain(domain)?.let { it in enabled } ?: true

        val taskCandidates = ctx.openTasks.mapNotNull { task ->
            val goal = goalsById[task.goalId] ?: return@mapNotNull null
            if (!allowed(goal.domain)) return@mapNotNull null
            Candidate(task.id, goal.id, task.id, task.title, goal.domain, task.dueDate ?: goal.targetDate, task.estimatedEffort ?: goal.estimatedEffort ?: 3, task.estimatedImpact ?: goal.estimatedImpact ?: 3)
        }

        val goalsWithOpenTasks = ctx.openTasks.map { it.goalId }.toSet()
        val goalCandidates = ctx.goals
            .filter { it.goalType != GoalType.LIFE_VISION && it.id !in goalsWithOpenTasks && it.progress < 1.0 && allowed(it.domain) }
            .map { Candidate(it.id, it.id, null, it.title, it.domain, it.targetDate, it.estimatedEffort ?: 3, it.estimatedImpact ?: 3) }

        // A routine still open today is the most concrete thing there is to do, and it carries today's deadline.
        val routineCandidates = ctx.pendingRoutines
            .filter { !it.isFixed && allowed(it.domain) }
            .map { r ->
                val effort = when { r.minutes >= 90 -> 5; r.minutes >= 60 -> 4; r.minutes >= 30 -> 3; else -> 2 }
                Candidate("rt-${r.id}", r.id, null, r.title, r.domain, ctx.endOfTodayMillis, effort, 4, routineId = r.id)
            }

        return taskCandidates + goalCandidates + routineCandidates
    }

    private fun factorsFor(c: Candidate, ctx: SuggestionContext, energy: Int): List<Factor> {
        val prefs = ctx.prefs
        val learned = ctx.learned
        val confidence = if (prefs.learningEnabled) learned.confidence else 0.0
        val factors = mutableListOf<Factor>()

        // Deadline pressure: 1.0 when overdue, decaying quickly as the date recedes.
        c.dueDate?.let { due ->
            val daysLeft = (due - ctx.nowMillis) / MILLIS_PER_DAY
            val urgency = if (daysLeft <= 0) 1.0 else 1.0 / (1.0 + daysLeft)
            val phrase = when {
                c.routineId != null -> "Daily routine, still open today"
                daysLeft <= 0 -> "Overdue"
                daysLeft < 1 -> "Due today"
                else -> "Due in ${daysLeft.toInt()} day${if (daysLeft.toInt() == 1) "" else "s"}"
            }
            factors += Factor(urgency * 1.5, phrase)
        }

        // Payoff per unit of effort — effort/impact are 1..5.
        val value = c.impact.coerceIn(1, 5).toDouble() / c.effort.coerceIn(1, 5)
        if (value > 1.0) factors += Factor(min(value - 1.0, 1.0) * 0.6, "High impact for the effort")

        // Does the task's weight match the energy you reported today?
        val heavy = c.effort >= 4
        when {
            heavy && energy >= 4 -> factors += Factor(0.5, "Your energy is high enough for deep work")
            heavy && energy <= 2 -> factors += Factor(-0.8, "Heavy task on low energy")
            !heavy && c.effort <= 2 && energy <= 2 -> factors += Factor(0.5, "Light task that fits low energy")
        }

        // When in the day: learned peak window, blended with a self-declared chronotype.
        val inLearnedPeak = confidence > 0 && learned.isPeakHour(ctx.hourOfDay)
        val inSelfPeak = prefs.selfChronotype?.let { isNear(ctx.hourOfDay, it) } == true
        if (inLearnedPeak) {
            val weight = if (heavy) 0.9 else 0.5
            factors += Factor(weight * confidence, "This is your peak window (you finish most tasks ${formatHour(learned.peakStartHour!!)}–${formatHour((learned.peakStartHour + learned.peakWindowHours) % 24)})")
        } else if (inSelfPeak && heavy) {
            factors += Factor(0.4, "Matches the ${prefs.selfChronotype!!.label.lowercase()} focus time you set")
        }

        // Persona: the domains this kind of person cares about most get a nudge.
        if (c.domain in prefs.persona.boostedDomains) {
            factors += Factor(0.3, "Core to how you use AETHER (${c.domain})")
        }

        // Learned affinity: domains you follow through on / accept suggestions for float up; ones you keep skipping sink.
        learned.domainStats[c.domain]?.takeIf { confidence > 0 }?.let { stat ->
            val affinity = (stat.completionRate + stat.acceptRate) / 2.0 - 0.5
            val score = affinity * 1.2 * confidence
            if (score > 0.1) factors += Factor(score, "You reliably follow through on ${c.domain}")
            else if (score < -0.1) factors += Factor(score, "")
        }

        // Structured planners want the biggest rock first; flexible ones get a gentler push toward quick wins.
        when (prefs.planningStyle) {
            PlanningStyle.STRUCTURED -> if (c.dueDate != null) factors += Factor(0.2, "Fits your structured plan")
            PlanningStyle.FLEXIBLE -> if (c.effort <= 2) factors += Factor(0.2, "Quick win")
        }

        return factors
    }

    /** Within ±2 hours of the chronotype's nominal peak. */
    private fun isNear(hour: Int, chronotype: Chronotype): Boolean {
        val d = (hour - chronotype.peakStartHour + 24) % 24
        return d <= 2 || d >= 22
    }

    private fun tone(tone: CoachTone, gentle: String, plain: String): String =
        if (tone == CoachTone.GENTLE) gentle else plain

    private fun formatScore(score: Double): String {
        val hundredths = (score * 100).toInt()
        return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
    }
}
