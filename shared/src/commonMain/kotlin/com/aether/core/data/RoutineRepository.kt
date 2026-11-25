package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aether.core.db.AetherDatabase
import com.aether.core.engine.ImportedBlock
import com.aether.core.model.ALL_DAYS
import com.aether.core.model.Routine
import com.aether.core.model.RoutineLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineRepository(private val database: AetherDatabase) {

    private val q get() = database.aetherQueries

    fun observeRoutines(): Flow<List<Routine>> =
        q.selectActiveRoutines().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    suspend fun addRoutine(title: String, domain: String, minutes: Int, daysMask: Int, fixedStartMinute: Int?) {
        q.insertRoutine(
            generateId(), title.trim(), domain.trim().ifBlank { "General" },
            minutes.toLong(), daysMask.toLong(), fixedStartMinute?.toLong(), currentTimeMillis(), 0L
        )
    }

    /**
     * Makes [blocks] the whole daily routine. A block identical to an existing routine keeps that routine
     * (and its history); every other routine is archived, so past ticks still count toward streaks.
     */
    suspend fun replaceWithImported(blocks: List<ImportedBlock>) {
        q.transaction {
            val unused = q.selectActiveRoutines().executeAsList().map { it.toDomain() }.toMutableList()
            val now = currentTimeMillis()
            blocks.forEachIndexed { i, b ->
                val remind = b.startMinute != null && !b.isBreak
                val same = unused.firstOrNull {
                    it.title == b.title && it.minutes == b.minutes && it.fixedStartMinute == b.startMinute &&
                        it.remind == remind && it.daysMask == ALL_DAYS
                }
                if (same != null) unused.remove(same)
                else q.insertRoutine(
                    generateId(), b.title, b.domain, b.minutes.toLong(), ALL_DAYS.toLong(),
                    b.startMinute?.toLong(), now + i, if (remind) 1L else 0L
                )
            }
            unused.forEach { q.archiveRoutine(it.id) }
        }
    }

    /** Archived, not deleted: earlier completions still count toward streaks. */
    suspend fun archiveRoutine(id: String) {
        q.archiveRoutine(id)
    }

    fun observeLogsSince(date: String): Flow<List<RoutineLog>> =
        q.selectRoutineLogsSince(date).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { RoutineLog(it.routineId, it.date, it.completedAt) } }

    /** Every date with at least one completed routine, for streaks. */
    fun observeLogDates(): Flow<Set<String>> =
        q.selectRoutineLogDates().asFlow().mapToList(Dispatchers.Default).map { it.toSet() }

    suspend fun setDone(routineId: String, date: String, done: Boolean) {
        if (done) q.insertRoutineLog(routineId, date, currentTimeMillis()) else q.deleteRoutineLog(routineId, date)
    }
}
