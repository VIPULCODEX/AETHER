package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aether.core.db.AetherDatabase
import com.aether.core.model.Goal
import com.aether.core.model.GoalType
import com.aether.core.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Goal hierarchy (Life Vision -> ... -> Weekly) and the Tasks ("Today's
 * Actions") that hang off a Goal. Split out of [AetherRepository] when the
 * hierarchy work landed, so goal/task queries have room to grow without
 * turning one class into a god-object.
 */
class GoalsRepository(private val database: AetherDatabase) {

    fun observeActiveGoals(): Flow<List<Goal>> =
        database.aetherQueries.selectActiveGoals()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    fun observeRootGoals(): Flow<List<Goal>> =
        database.aetherQueries.selectRootGoals()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    fun observeChildGoals(parentGoalId: String): Flow<List<Goal>> =
        database.aetherQueries.selectChildGoals(parentGoalId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun addGoal(
        title: String,
        domain: String,
        targetDate: Long?,
        goalType: GoalType,
        parentGoalId: String?,
        priority: Int? = null,
        estimatedEffort: Int? = null,
        estimatedImpact: Int? = null
    ) {
        database.aetherQueries.insertGoal(
            id = generateId(),
            title = title,
            domain = domain,
            targetDate = targetDate,
            createdAt = currentTimeMillis(),
            progress = 0.0,
            isArchived = 0L,
            goalType = goalType.name,
            parentGoalId = parentGoalId,
            priority = priority?.toLong(),
            estimatedEffort = estimatedEffort?.toLong(),
            estimatedImpact = estimatedImpact?.toLong(),
            aiImportanceScore = null
        )
    }

    suspend fun updateGoalProgress(goalId: String, progress: Double) {
        database.aetherQueries.updateGoalProgress(progress, goalId)
    }

    fun observeTasksForGoal(goalId: String): Flow<List<Task>> =
        database.aetherQueries.selectTasksForGoal(goalId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    /** All tasks regardless of done state — the ScoringEngine's tree roll-up needs both. */
    fun observeAllTasks(): Flow<List<Task>> =
        database.aetherQueries.selectAllTasks()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    fun observeOpenTasks(): Flow<List<Task>> =
        database.aetherQueries.selectOpenTasks()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun addTask(
        goalId: String,
        title: String,
        dueDate: Long?,
        priority: Int? = null,
        estimatedEffort: Int? = null,
        estimatedImpact: Int? = null
    ) {
        database.aetherQueries.insertTask(
            id = generateId(),
            goalId = goalId,
            title = title,
            dueDate = dueDate,
            priority = priority?.toLong(),
            estimatedEffort = estimatedEffort?.toLong(),
            estimatedImpact = estimatedImpact?.toLong(),
            aiImportanceScore = null,
            isDone = 0L,
            completedAt = null,
            createdAt = currentTimeMillis()
        )
    }

    suspend fun setTaskDone(taskId: String, isDone: Boolean) {
        database.aetherQueries.setTaskDone(
            isDone = if (isDone) 1L else 0L,
            completedAt = if (isDone) currentTimeMillis() else null,
            id = taskId
        )
    }

    suspend fun deleteTask(taskId: String) {
        database.aetherQueries.deleteTask(taskId)
    }
}
