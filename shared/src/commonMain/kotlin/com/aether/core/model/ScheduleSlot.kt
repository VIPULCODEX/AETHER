package com.aether.core.model

data class ScheduleSlot(
    val id: String,
    val dayOfWeek: Int,
    val timeLabel: String,
    val activityLabel: String,
    val domain: String
)
