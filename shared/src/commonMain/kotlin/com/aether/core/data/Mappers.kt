package com.aether.core.data

import com.aether.core.db.BodyLog as BodyLogRow
import com.aether.core.db.DailyCheckIn as DailyCheckInRow
import com.aether.core.db.Goal as GoalRow
import com.aether.core.db.JournalEntry as JournalEntryRow
import com.aether.core.db.ResearchNote as ResearchNoteRow
import com.aether.core.db.ScheduleSlot as ScheduleSlotRow
import com.aether.core.db.Task as TaskRow
import com.aether.core.db.UserProfile as UserProfileRow
import com.aether.core.model.BodyLog
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.GoalType
import com.aether.core.model.JournalEntry
import com.aether.core.model.ResearchNote
import com.aether.core.model.ScheduleSlot
import com.aether.core.model.Task
import com.aether.core.model.UserProfile

internal fun JournalEntryRow.toDomain() = JournalEntry(
    id = id,
    createdAt = createdAt,
    content = content,
    mood = mood?.toInt(),
    attachmentUri = attachmentUri
)

internal fun BodyLogRow.toDomain() = BodyLog(
    id = id,
    createdAt = createdAt,
    weightKg = weightKg,
    photoUri = photoUri,
    note = note
)

internal fun GoalRow.toDomain() = Goal(
    id = id,
    title = title,
    domain = domain,
    targetDate = targetDate,
    createdAt = createdAt,
    progress = progress,
    isArchived = isArchived == 1L,
    goalType = runCatching { GoalType.valueOf(goalType) }.getOrDefault(GoalType.WEEKLY),
    parentGoalId = parentGoalId,
    priority = priority?.toInt(),
    estimatedEffort = estimatedEffort?.toInt(),
    estimatedImpact = estimatedImpact?.toInt(),
    aiImportanceScore = aiImportanceScore
)

internal fun TaskRow.toDomain() = Task(
    id = id,
    goalId = goalId,
    title = title,
    dueDate = dueDate,
    priority = priority?.toInt(),
    estimatedEffort = estimatedEffort?.toInt(),
    estimatedImpact = estimatedImpact?.toInt(),
    aiImportanceScore = aiImportanceScore,
    isDone = isDone == 1L,
    completedAt = completedAt,
    createdAt = createdAt
)

internal fun DailyCheckInRow.toDomain() = DailyCheckIn(
    date = date,
    mood = mood?.toInt(),
    energy = energy?.toInt(),
    sleepHours = sleepHours,
    executedMission = executedMission == 1L
)

internal fun ScheduleSlotRow.toDomain() = ScheduleSlot(
    id = id,
    dayOfWeek = dayOfWeek.toInt(),
    timeLabel = timeLabel,
    activityLabel = activityLabel,
    domain = domain
)

internal fun UserProfileRow.toDomain() = UserProfile(
    heightCm = heightCm,
    weightKg = weightKg,
    age = age?.toInt(),
    isMale = isMale?.let { it == 1L },
    activityLevel = activityLevel,
    bodyGoal = bodyGoal
)

internal fun ResearchNoteRow.toDomain() = ResearchNote(
    id = id,
    title = title,
    note = note,
    status = status,
    createdAt = createdAt,
    attachmentUri = attachmentUri
)
