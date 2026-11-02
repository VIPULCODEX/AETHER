package com.aether.core.engine

import com.aether.core.model.BehaviorEvent
import com.aether.core.model.BehaviorEventType
import com.aether.core.model.Chronotype
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.Task
import kotlin.math.min
import kotlin.math.pow

/** Per-domain track record. Rates are Laplace-smoothed so two data points never read as certainty. */
data class DomainStat(
    val domain: String,
    val tasksDone: Int,
    val tasksTotal: Int,
    val suggestionsAccepted: Int,
    val suggestionsDismissed: Int
) {
    val completionRate: Double get() = (tasksDone + 1.0) / (tasksTotal + 2.0)
    val acceptRate: Double get() = (suggestionsAccepted + 1.0) / (suggestionsAccepted + suggestionsDismissed + 2.0)
}

/**
 * What AETHER has inferred about one specific person, purely from their own
 * on-device history. [confidence] grows with data (0 on day one, 1 after ~30
 * observations) and is what blends "what you told me" with "what I observed" —
 * nothing here is shown or applied as certain while it's still thin.
 */
data class LearnedProfile(
    val observations: Int,
    val confidence: Double,
    val peakStartHour: Int?,
    val peakWindowHours: Int,
    val chronotype: Chronotype?,
    val domainStats: Map<String, DomainStat>,
    val insights: List<String>
) {
    fun isPeakHour(hour: Int): Boolean {
        val start = peakStartHour ?: return false
        return (hour - start + 24) % 24 < peakWindowHours
    }

    companion object {
        val EMPTY = LearnedProfile(0, 0.0, null, PEAK_WINDOW_HOURS, null, emptyMap(), emptyList())
    }
}

const val PEAK_WINDOW_HOURS = 3
private const val MIN_COMPLETIONS_FOR_PEAK = 5
private const val FULL_CONFIDENCE_OBSERVATIONS = 30
private const val RECENCY_HALF_LIFE_DAYS = 30.0
private const val MILLIS_PER_DAY = 86_400_000.0

/**
 * Turns raw history into a [LearnedProfile]. Deterministic and side-effect
 * free (no randomness, no clock, no timezone of its own) so identical history
 * always yields identical output — which also makes every "why" it surfaces
 * reproducible and testable. Recent behavior counts for more than old
 * behavior (30-day half-life) so it tracks people whose routines change.
 */
class PersonalizationEngine {

    fun learn(
        tasks: List<Task>,
        goals: List<Goal>,
        checkIns: List<DailyCheckIn>,
        events: List<BehaviorEvent>,
        nowMillis: Long,
        hourOf: (Long) -> Int
    ): LearnedProfile {
        val completions = tasks.filter { it.isDone && it.completedAt != null }
        val observations = completions.size + events.size
        val confidence = min(1.0, observations.toDouble() / FULL_CONFIDENCE_OBSERVATIONS)

        val histogram = DoubleArray(24)
        completions.forEach { task ->
            val at = task.completedAt!!
            val ageDays = ((nowMillis - at) / MILLIS_PER_DAY).coerceAtLeast(0.0)
            histogram[hourOf(at).coerceIn(0, 23)] += 0.5.pow(ageDays / RECENCY_HALF_LIFE_DAYS)
        }
        val peakStart = if (completions.size >= MIN_COMPLETIONS_FOR_PEAK) bestWindowStart(histogram) else null
        val chronotype = peakStart?.let { chronotypeFor((it + PEAK_WINDOW_HOURS / 2) % 24) }

        val domainByGoal = goals.associate { it.id to it.domain }
        val taskDomains = tasks.groupBy { domainByGoal[it.goalId] }.filterKeys { it != null }
        val domains = (taskDomains.keys.filterNotNull() + events.mapNotNull { it.domain }).toSortedSet()
        val domainStats = domains.associateWith { domain ->
            val domainTasks = taskDomains[domain].orEmpty()
            val domainEvents = events.filter { it.domain == domain }
            DomainStat(
                domain = domain,
                tasksDone = domainTasks.count { it.isDone },
                tasksTotal = domainTasks.size,
                suggestionsAccepted = domainEvents.count { it.type == BehaviorEventType.SUGGESTION_ACCEPTED },
                suggestionsDismissed = domainEvents.count { it.type == BehaviorEventType.SUGGESTION_DISMISSED }
            )
        }

        return LearnedProfile(
            observations = observations,
            confidence = confidence,
            peakStartHour = peakStart,
            peakWindowHours = PEAK_WINDOW_HOURS,
            chronotype = chronotype,
            domainStats = domainStats,
            insights = buildInsights(peakStart, chronotype, domainStats, checkIns)
        )
    }

    /** Start hour of the circular [PEAK_WINDOW_HOURS]-hour window with the most (recency-weighted) completions. */
    private fun bestWindowStart(histogram: DoubleArray): Int =
        (0 until 24).maxByOrNull { start ->
            (0 until PEAK_WINDOW_HOURS).sumOf { histogram[(start + it) % 24] }
        } ?: 0

    private fun chronotypeFor(centerHour: Int): Chronotype = when (centerHour) {
        in 5..10 -> Chronotype.MORNING
        in 11..16 -> Chronotype.DAY
        in 17..21 -> Chronotype.EVENING
        else -> Chronotype.NIGHT
    }

    private fun buildInsights(
        peakStart: Int?,
        chronotype: Chronotype?,
        domainStats: Map<String, DomainStat>,
        checkIns: List<DailyCheckIn>
    ): List<String> {
        val insights = mutableListOf<String>()
        if (peakStart != null && chronotype != null) {
            insights += "You finish most tasks between ${formatHour(peakStart)} and ${formatHour((peakStart + PEAK_WINDOW_HOURS) % 24)} — a ${chronotype.label.lowercase()} pattern."
        }
        val reliable = domainStats.values.filter { it.tasksTotal >= 4 }
        reliable.maxByOrNull { it.completionRate }?.let { best ->
            insights += "${best.domain} is where you follow through most (${(best.completionRate * 100).toInt()}% of tasks done)."
        }
        reliable.minByOrNull { it.completionRate }
            ?.takeIf { low -> reliable.size > 1 && low.completionRate < 0.5 }
            ?.let { low -> insights += "${low.domain} tasks tend to stall (${(low.completionRate * 100).toInt()}% done) — smaller steps may help." }
        domainStats.values
            .filter { it.suggestionsDismissed >= 3 && it.acceptRate < 0.35 }
            .forEach { insights += "You usually skip ${it.domain} suggestions, so they're shown less often." }

        val withSleep = checkIns.filter { it.sleepHours != null && it.energy != null }
        val rested = withSleep.filter { it.sleepHours!! >= 7.0 }
        val short = withSleep.filter { it.sleepHours!! < 7.0 }
        if (rested.size >= 3 && short.size >= 3) {
            val diff = rested.map { it.energy!! }.average() - short.map { it.energy!! }.average()
            if (diff >= 0.5) {
                insights += "Your energy is about ${formatOneDecimal(diff)} points higher after 7+ hours of sleep."
            }
        }
        return insights
    }
}

internal fun formatHour(hour: Int): String {
    val h12 = when (val h = hour % 12) { 0 -> 12; else -> h }
    return "$h12 ${if (hour % 24 < 12) "AM" else "PM"}"
}

private fun formatOneDecimal(value: Double): String {
    val tenths = (value * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}
