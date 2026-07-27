package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.aether.core.db.AetherDatabase
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.JournalEntry
import com.aether.core.model.ScheduleSlot
import com.aether.core.model.UserProfile
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

    fun observeFocusAreas(): Flow<List<String>> =
        database.aetherQueries.selectFocusAreas()
            .asFlow()
            .mapToList(Dispatchers.Default)

    suspend fun setFocusAreaEnabled(name: String, enabled: Boolean) {
        if (enabled) {
            database.aetherQueries.insertFocusArea(name)
        } else {
            database.aetherQueries.deleteFocusArea(name)
        }
    }

    fun observeScheduleSlots(): Flow<List<ScheduleSlot>> =
        database.aetherQueries.selectAllScheduleSlots()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun regenerateSchedule(slots: List<ScheduleSlot>) {
        database.aetherQueries.deleteAllScheduleSlots()
        slots.forEach { slot ->
            database.aetherQueries.insertScheduleSlot(
                id = slot.id,
                dayOfWeek = slot.dayOfWeek.toLong(),
                timeLabel = slot.timeLabel,
                activityLabel = slot.activityLabel,
                domain = slot.domain
            )
        }
    }

    suspend fun updateScheduleSlotLabel(id: String, newLabel: String) {
        database.aetherQueries.updateScheduleSlotLabel(newLabel, id)
    }

    fun observeUserProfile(): Flow<UserProfile?> =
        database.aetherQueries.selectUserProfile()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }

    suspend fun saveUserProfile(profile: UserProfile) {
        database.aetherQueries.upsertUserProfile(
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            age = profile.age?.toLong(),
            isMale = profile.isMale?.let { if (it) 1L else 0L },
            activityLevel = profile.activityLevel,
            bodyGoal = profile.bodyGoal
        )
    }
}
