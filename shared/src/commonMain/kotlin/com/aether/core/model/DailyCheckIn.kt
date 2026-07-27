package com.aether.core.model

data class DailyCheckIn(
    val date: String,
    val mood: Int?,
    val energy: Int?,
    val sleepHours: Double?,
    val executedMission: Boolean
)
