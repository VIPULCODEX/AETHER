package com.aether.android.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.android.alarm.AlarmScheduler
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.GroqScheduleClient
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.engine.BasicScheduleGenerator
import com.aether.core.engine.ScoringEngine
import com.aether.core.model.Goal
import com.aether.core.model.GoalType
import com.aether.core.model.ScheduleSlot
import com.aether.core.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val STANDARD_FOCUS_AREAS = listOf("Gym", "Research", "GATE", "JEE", "College Work")

val GOAL_TYPE_LABELS = mapOf(
    GoalType.LIFE_VISION to "Life Vision",
    GoalType.LONG_TERM to "Long Term",
    GoalType.DOMAIN to "Domain Goal",
    GoalType.QUARTERLY to "Quarterly",
    GoalType.MONTHLY to "Monthly",
    GoalType.WEEKLY to "Weekly"
)

data class GoalsUiState(
    val focusAreas: List<String> = emptyList(),
    val scheduleSlots: List<ScheduleSlot> = emptyList(),
    val isGeneratingWithAi: Boolean = false,
    val aiErrorMessage: String? = null,
    val selectedGoalId: String? = null,
    /** Path from the root goal down to the selected one — empty at the top level. */
    val breadcrumb: List<Goal> = emptyList(),
    /** Children of the selected goal (or root goals, when nothing is selected). */
    val visibleGoals: List<Goal> = emptyList(),
    /** Roll-up progress percent for each visible goal, from ScoringEngine.computeGoalTreeProgress. */
    val progressByGoalId: Map<String, Int> = emptyMap(),
    /** Today's Actions for the selected goal — only meaningful once a goal is selected. */
    val tasksForSelectedGoal: List<Task> = emptyList()
) {
    val selectedGoal: Goal? get() = breadcrumb.lastOrNull()
}

class GoalsViewModel(
    private val repository: AetherRepository,
    private val goalsRepository: GoalsRepository,
    private val apiKeyStore: ApiKeyStore,
    private val groqClient: GroqScheduleClient,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val scheduleGenerator = BasicScheduleGenerator()
    private val scoringEngine = ScoringEngine()

    private val _selectedGoalId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                goalsRepository.observeActiveGoals(),
                goalsRepository.observeAllTasks(),
                repository.observeFocusAreas(),
                repository.observeScheduleSlots(),
                _selectedGoalId
            ) { goals, tasks, focusAreas, slots, selectedId ->
                val breadcrumb = buildBreadcrumb(selectedId, goals)
                val visibleGoals = goals
                    .filter { it.parentGoalId == selectedId }
                    .sortedByDescending { it.createdAt }
                val progressByGoalId = visibleGoals.associate {
                    it.id to (scoringEngine.computeGoalTreeProgress(it.id, goals, tasks) * 100).toInt()
                }
                val tasksForSelected = selectedId
                    ?.let { id -> tasks.filter { it.goalId == id }.sortedBy { it.isDone } }
                    ?: emptyList()

                Data5(focusAreas, slots, breadcrumb, visibleGoals, progressByGoalId, tasksForSelected)
            }.collect { data ->
                _uiState.value = _uiState.value.copy(
                    focusAreas = data.focusAreas,
                    scheduleSlots = data.slots,
                    selectedGoalId = _selectedGoalId.value,
                    breadcrumb = data.breadcrumb,
                    visibleGoals = data.visibleGoals,
                    progressByGoalId = data.progressByGoalId,
                    tasksForSelectedGoal = data.tasksForSelected
                )
            }
        }
    }

    private fun buildBreadcrumb(selectedId: String?, goals: List<Goal>): List<Goal> {
        val byId = goals.associateBy { it.id }
        val path = mutableListOf<Goal>()
        var current = selectedId?.let { byId[it] }
        while (current != null) {
            path.add(0, current)
            current = current.parentGoalId?.let { byId[it] }
        }
        return path
    }

    fun selectGoal(goalId: String?) {
        _selectedGoalId.value = goalId
    }

    fun addGoal(title: String, goalType: GoalType, domain: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            goalsRepository.addGoal(
                title = title.trim(),
                domain = domain.trim().ifBlank { "General" },
                targetDate = null,
                goalType = goalType,
                parentGoalId = _uiState.value.selectedGoalId
            )
        }
    }

    fun addTask(title: String) {
        val goalId = _uiState.value.selectedGoalId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            goalsRepository.addTask(goalId = goalId, title = title.trim(), dueDate = null)
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            goalsRepository.setTaskDone(task.id, !task.isDone)
        }
    }

    fun toggleFocusArea(name: String) {
        viewModelScope.launch {
            val currentlyEnabled = _uiState.value.focusAreas.contains(name)
            repository.setFocusAreaEnabled(name, enabled = !currentlyEnabled)
        }
    }

    fun regenerateSchedule() {
        viewModelScope.launch {
            val slots = scheduleGenerator.generate(_uiState.value.focusAreas)
            repository.regenerateSchedule(slots)
            alarmScheduler.rescheduleAll(slots)
        }
    }

    fun generateWithAi(description: String) {
        val apiKey = apiKeyStore.getGroqKey()
        if (apiKey == null) {
            _uiState.value = _uiState.value.copy(
                aiErrorMessage = "Add a Groq API key in Settings first (it's free)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingWithAi = true, aiErrorMessage = null)
            try {
                val focusAreas = _uiState.value.focusAreas
                val slots = withContext(Dispatchers.IO) {
                    groqClient.generateSchedule(apiKey, focusAreas, description)
                }
                repository.regenerateSchedule(slots)
                alarmScheduler.rescheduleAll(slots)
                _uiState.value = _uiState.value.copy(isGeneratingWithAi = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingWithAi = false,
                    aiErrorMessage = e.message ?: "AI schedule generation failed."
                )
            }
        }
    }

    fun editSlot(id: String, newLabel: String) {
        if (newLabel.isBlank()) return
        viewModelScope.launch {
            repository.updateScheduleSlotLabel(id, newLabel.trim())
            val slots = _uiState.value.scheduleSlots.map { if (it.id == id) it.copy(activityLabel = newLabel.trim()) else it }
            alarmScheduler.rescheduleAll(slots)
        }
    }

    private data class Data5(
        val focusAreas: List<String>,
        val slots: List<ScheduleSlot>,
        val breadcrumb: List<Goal>,
        val visibleGoals: List<Goal>,
        val progressByGoalId: Map<String, Int>,
        val tasksForSelected: List<Task>
    )
}
