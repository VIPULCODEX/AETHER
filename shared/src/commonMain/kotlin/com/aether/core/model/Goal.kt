package com.aether.core.model

data class Goal(
    val id: String,
    val title: String,
    val domain: String,
    val targetDate: Long?,
    val createdAt: Long,
    val progress: Double,
    val isArchived: Boolean,
    val goalType: GoalType,
    val parentGoalId: String?,
    val priority: Int?,
    val estimatedEffort: Int?,
    val estimatedImpact: Int?,
    val aiImportanceScore: Double?
)
