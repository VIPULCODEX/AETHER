package com.aether.core.engine

import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal

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
}
