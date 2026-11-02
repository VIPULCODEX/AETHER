package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aether.core.db.AetherDatabase
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
            minutes.toLong(), daysMask.toLong(), fixedStartMinute?.toLong(), currentTimeMillis()
        )
    }

    /** Archived, not deleted: earlier completions still count toward streaks. */
    suspend fun archiveRoutine(id: String) {
        q.archiveRoutine(id)
    }

    fun observeLogsSince(date: String): Flow<List<RoutineLog>> =
        q.selectRoutineLogsSince(date).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { RoutineLog(it.routineId, it.date) } }

    /** Every date with at least one completed routine, for streaks. */
    fun observeLogDates(): Flow<Set<String>> =
        q.selectRoutineLogDates().asFlow().mapToList(Dispatchers.Default).map { it.toSet() }

    suspend fun setDone(routineId: String, date: String, done: Boolean) {
        if (done) q.insertRoutineLog(routineId, date) else q.deleteRoutineLog(routineId, date)
    }
}
