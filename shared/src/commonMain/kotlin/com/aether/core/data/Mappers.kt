package com.aether.core.data

import com.aether.core.db.DailyCheckIn as DailyCheckInRow
import com.aether.core.db.Goal as GoalRow
import com.aether.core.db.JournalEntry as JournalEntryRow
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.JournalEntry

internal fun JournalEntryRow.toDomain() = JournalEntry(
    id = id,
    createdAt = createdAt,
    content = content,
    mood = mood?.toInt()
)

internal fun GoalRow.toDomain() = Goal(
    id = id,
    title = title,
    domain = domain,
    targetDate = targetDate,
    createdAt = createdAt,
    progress = progress,
    isArchived = isArchived == 1L
)

internal fun DailyCheckInRow.toDomain() = DailyCheckIn(
    date = date,
    mood = mood?.toInt(),
    energy = energy?.toInt(),
    sleepHours = sleepHours,
    executedMission = executedMission == 1L
)
