package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aether.core.db.AetherDatabase
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.JournalEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single facade over the local Life Data Store. Everything in AETHER —
 * every module, the Scoring Engine, the Context Engine — reads and writes
 * through this repository. Nothing here ever leaves the device.
 */
class AetherRepository(private val database: AetherDatabase) {

    fun observeJournalEntries(): Flow<List<JournalEntry>> =
        database.aetherQueries.selectAllJournalEntries()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun addJournalEntry(content: String, mood: Int?) {
        database.aetherQueries.insertJournalEntry(
            id = generateId(),
            createdAt = currentTimeMillis(),
            content = content,
            mood = mood?.toLong()
        )
    }

    fun observeActiveGoals(): Flow<List<Goal>> =
        database.aetherQueries.selectActiveGoals()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun addGoal(title: String, domain: String, targetDate: Long?) {
        database.aetherQueries.insertGoal(
            id = generateId(),
            title = title,
            domain = domain,
            targetDate = targetDate,
            createdAt = currentTimeMillis(),
            progress = 0.0,
            isArchived = 0L
        )
    }

    suspend fun updateGoalProgress(goalId: String, progress: Double) {
        database.aetherQueries.updateGoalProgress(progress, goalId)
    }

    fun observeRecentCheckIns(limit: Long = 14): Flow<List<DailyCheckIn>> =
        database.aetherQueries.selectRecentCheckIns(limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun upsertTodayCheckIn(
        date: String,
        mood: Int?,
        energy: Int?,
        sleepHours: Double?,
        executedMission: Boolean
    ) {
        database.aetherQueries.upsertDailyCheckIn(
            date = date,
            mood = mood?.toLong(),
            energy = energy?.toLong(),
            sleepHours = sleepHours,
            executedMission = if (executedMission) 1L else 0L
        )
    }
}
