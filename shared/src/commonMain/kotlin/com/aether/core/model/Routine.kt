package com.aether.core.model

const val ALL_DAYS = 0b1111111

/**
 * Something that recurs: a daily habit ("GATE revision, 2 h"), or a fixed
 * commitment ("College 9:00-16:00, Mon-Fri") when [fixedStartMinute] is set.
 * [daysMask] bit 0 = Monday ... bit 6 = Sunday. Archived rather than deleted so
 * past completions keep counting toward streaks.
 */
data class Routine(
    val id: String,
    val title: String,
    val domain: String,
    val minutes: Int,
    val daysMask: Int,
    val fixedStartMinute: Int?,
    val createdAt: Long,
    val archived: Boolean = false
) {
    val isFixed: Boolean get() = fixedStartMinute != null
    fun occursOn(dayOfWeek: Int): Boolean = (daysMask shr dayOfWeek) and 1 == 1
}

/** A routine ticked off on a given local date (yyyy-MM-dd). */
data class RoutineLog(val routineId: String, val date: String)
