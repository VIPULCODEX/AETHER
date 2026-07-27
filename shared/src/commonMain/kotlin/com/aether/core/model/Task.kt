package com.aether.core.model

/**
 * A Today's Action — always tied to a Goal, so it always knows why it exists:
 * walk `goalId` up through `Goal.parentGoalId` to reach the Life Vision it
 * ultimately serves.
 */
data class Task(
    val id: String,
    val goalId: String,
    val title: String,
    val dueDate: Long?,
    val priority: Int?,
    val estimatedEffort: Int?,
    val estimatedImpact: Int?,
    val aiImportanceScore: Double?,
    val isDone: Boolean,
    val completedAt: Long?,
    val createdAt: Long
)
