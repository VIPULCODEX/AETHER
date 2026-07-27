package com.aether.core.model

data class Goal(
    val id: String,
    val title: String,
    val domain: String,
    val targetDate: Long?,
    val createdAt: Long,
    val progress: Double,
    val isArchived: Boolean
)
