package com.aether.core.model

/** A single Gym progress check-in — weight and/or a progress photo, the Strong/Hevy-style timeline. */
data class BodyLog(
    val id: String,
    val createdAt: Long,
    val weightKg: Double?,
    val photoUri: String?,
    val note: String?
)
