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
    val archived: Boolean = false,
    /** A timed block from the user's own timetable: it keeps its time, but still rings and counts as their work. */
    val remind: Boolean = false
) {
    val isFixed: Boolean get() = fixedStartMinute != null
    /** Something the person does (ticked, learned from, suggested), as opposed to somewhere they just are. */
    val isTracked: Boolean get() = !isFixed || remind
    fun occursOn(dayOfWeek: Int): Boolean = (daysMask shr dayOfWeek) and 1 == 1
}

/** A routine ticked off on a given local date (yyyy-MM-dd). */
/** [completedAt] (epoch ms) is what lets AETHER learn *when* you actually do things; older rows may lack it. */
data class RoutineLog(val routineId: String, val date: String, val completedAt: Long? = null)
