package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.aether.core.db.AetherDatabase
import com.aether.core.model.BodyLog
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.JournalEntry
import com.aether.core.model.ResearchNote
import com.aether.core.model.ScheduleSlot
import com.aether.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Facade over the local Life Data Store for everything except Goals/Tasks
 * (see [GoalsRepository]) — split out once the Goal hierarchy work made a
 * single god-repository unmanageable. Nothing here ever leaves the device.
 */
class AetherRepository(private val database: AetherDatabase) {

    fun observeJournalEntries(): Flow<List<JournalEntry>> =
        database.aetherQueries.selectAllJournalEntries()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun addJournalEntry(content: String, mood: Int?, attachmentUri: String? = null) {
        database.aetherQueries.insertJournalEntry(
            id = generateId(),
            createdAt = currentTimeMillis(),
            content = content,
            mood = mood?.toLong(),
            attachmentUri = attachmentUri
        )
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

    fun observeResearchNotes(): Flow<List<ResearchNote>> =
        database.aetherQueries.selectAllResearchNotes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun addResearchNote(title: String, note: String, status: String, attachmentUri: String? = null) {
        database.aetherQueries.insertResearchNote(
            id = generateId(),
            title = title,
            note = note,
            status = status,
            createdAt = currentTimeMillis(),
            attachmentUri = attachmentUri
        )
    }

    suspend fun deleteResearchNote(id: String) {
        database.aetherQueries.deleteResearchNote(id)
    }

    fun observeBodyLogs(): Flow<List<BodyLog>> =
        database.aetherQueries.selectAllBodyLogs()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun addBodyLog(weightKg: Double?, photoUri: String?, note: String?) {
        database.aetherQueries.insertBodyLog(
            id = generateId(),
            createdAt = currentTimeMillis(),
            weightKg = weightKg,
            photoUri = photoUri,
            note = note?.takeIf { it.isNotBlank() }
        )
    }

    suspend fun deleteBodyLog(id: String) {
        database.aetherQueries.deleteBodyLog(id)
    }
}
