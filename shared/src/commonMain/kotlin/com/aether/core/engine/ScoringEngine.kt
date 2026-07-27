package com.aether.core.engine

import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.GoalType
import com.aether.core.model.Task

/**
 * Every field here must be traceable back to real logged data — no black-box
 * numbers. This is v1: Consistency, Execution and Goal Completion roll up
 * into Life Score. Health/Research/Learning/Discipline/Mental Recovery
 * scores get added once their source modules (Gym, Research OS, GATE prep,
 * Journal mood trends) exist.
 */
data class LifeScoreBreakdown(
    val consistencyScore: Int,
    val executionScore: Int,
    val goalCompletionPercent: Int,
    val lifeScore: Int
)

class ScoringEngine {

    fun compute(checkIns: List<DailyCheckIn>, goals: List<Goal>): LifeScoreBreakdown {
        val consistency = consistencyScore(checkIns)
        val execution = executionScore(checkIns)
        val goalCompletion = goalCompletionPercent(goals)
        val life = (consistency + execution + goalCompletion) / 3

        return LifeScoreBreakdown(
            consistencyScore = consistency,
            executionScore = execution,
            goalCompletionPercent = goalCompletion,
            lifeScore = life
        )
    }

    /** Rolling 14-day engagement regularity — never a streak, so one missed day never resets it. */
    private fun consistencyScore(checkIns: List<DailyCheckIn>): Int {
        if (checkIns.isEmpty()) return 0
        val window = checkIns.take(14)
        return ((window.size.toDouble() / 14.0) * 100).toInt().coerceIn(0, 100)
    }

    /** Rolling 7-day plan-vs-actual follow-through. */
    private fun executionScore(checkIns: List<DailyCheckIn>): Int {
        val window = checkIns.take(7)
        if (window.isEmpty()) return 0
        val executed = window.count { it.executedMission }
        return ((executed.toDouble() / window.size) * 100).toInt().coerceIn(0, 100)
    }

    private fun goalCompletionPercent(goals: List<Goal>): Int {
        val active = goals.filter { !it.isArchived }
        if (active.isEmpty()) return 0
        return (active.map { it.progress }.average() * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Rolls up progress toward a Goal from its subtree: if it has child goals,
     * average their (recursively computed) progress; otherwise, if it has
     * tasks, the fraction of those marked done; otherwise its own manually
     * tracked `progress`. No black-box weighting — same "traceable back to
     * real logged data" rule as the rest of this engine.
     */
    fun computeGoalTreeProgress(goalId: String, goals: List<Goal>, tasks: List<Task>): Double {
        return rollUp(goalId, goals, tasks, mutableSetOf())
    }

    private fun rollUp(goalId: String, goals: List<Goal>, tasks: List<Task>, visited: MutableSet<String>): Double {
        if (!visited.add(goalId)) return 0.0
        val children = goals.filter { it.parentGoalId == goalId && !it.isArchived }
        if (children.isNotEmpty()) {
            return children.map { rollUp(it.id, goals, tasks, visited) }.average()
        }
        val ownTasks = tasks.filter { it.goalId == goalId }
        if (ownTasks.isNotEmpty()) {
            return ownTasks.count { it.isDone }.toDouble() / ownTasks.size
        }
        return goals.find { it.id == goalId }?.progress ?: 0.0
    }

    /** The nearest Life Vision goal and how close its whole subtree is to completion, for the Dashboard's identity card. */
    fun computeLifeVisionProgress(goals: List<Goal>, tasks: List<Task>): LifeVisionProgress? {
        val vision = goals
            .filter { it.goalType == GoalType.LIFE_VISION && !it.isArchived }
            .minByOrNull { it.createdAt }
            ?: return null
        val progress = computeGoalTreeProgress(vision.id, goals, tasks)
        return LifeVisionProgress(
            goalId = vision.id,
            title = vision.title,
            progressPercent = (progress * 100).toInt().coerceIn(0, 100)
        )
    }
}

data class LifeVisionProgress(val goalId: String, val title: String, val progressPercent: Int)
