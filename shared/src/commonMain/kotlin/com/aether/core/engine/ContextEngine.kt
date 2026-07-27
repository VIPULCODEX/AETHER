package com.aether.core.engine

import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal

/**
 * A suggestion always carries its reason. "What should I do now?" must
 * never get a generic answer — the reason is what proves it's grounded in
 * real, current data (time, energy, deadlines), not a template.
 */
data class Suggestion(val title: String, val reason: String)

class ContextEngine {

    fun suggestNow(
        hourOfDay: Int,
        todayCheckIn: DailyCheckIn?,
        activeGoals: List<Goal>
    ): Suggestion {
        val energy = todayCheckIn?.energy ?: 3
        val nearestDeadlineGoal = activeGoals
            .filter { it.targetDate != null }
            .minByOrNull { it.targetDate!! }

        return when {
            energy <= 2 && hourOfDay >= 21 -> Suggestion(
                title = "Wind down",
                reason = "Energy is low and it's late — recovery tonight is what moves you forward tomorrow."
            )

            nearestDeadlineGoal != null && energy >= 3 -> Suggestion(
                title = "Work on: ${nearestDeadlineGoal.title}",
                reason = "This has the closest deadline among your active goals, and your energy can support it right now."
            )

            activeGoals.isNotEmpty() -> Suggestion(
                title = "Work on: ${activeGoals.first().title}",
                reason = "Energy is moderate — pick something lighter but still moving you forward."
            )

            else -> Suggestion(
                title = "Set today's mission",
                reason = "No active goals logged yet — start by defining what matters this week."
            )
        }
    }
}
