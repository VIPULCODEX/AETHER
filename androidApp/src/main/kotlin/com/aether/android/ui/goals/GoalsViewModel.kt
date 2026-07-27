package com.aether.android.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.GroqScheduleClient
import com.aether.core.data.AetherRepository
import com.aether.core.engine.BasicScheduleGenerator
import com.aether.core.model.Goal
import com.aether.core.model.ScheduleSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val STANDARD_FOCUS_AREAS = listOf("Gym", "Research", "GATE", "JEE", "College Work")

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val focusAreas: List<String> = emptyList(),
    val scheduleSlots: List<ScheduleSlot> = emptyList(),
    val isGeneratingWithAi: Boolean = false,
    val aiErrorMessage: String? = null
)

class GoalsViewModel(
    private val repository: AetherRepository,
    private val apiKeyStore: ApiKeyStore,
    private val groqClient: GroqScheduleClient
) : ViewModel() {

    private val scheduleGenerator = BasicScheduleGenerator()

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeActiveGoals(),
                repository.observeFocusAreas(),
                repository.observeScheduleSlots()
            ) { goals, focusAreas, slots -> Triple(goals, focusAreas, slots) }
                .collect { (goals, focusAreas, slots) ->
                    _uiState.value = _uiState.value.copy(
                        goals = goals,
                        focusAreas = focusAreas,
                        scheduleSlots = slots
                    )
                }
        }
    }

    fun addGoal(title: String, domain: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addGoal(
                title = title.trim(),
                domain = domain.trim().ifBlank { "General" },
                targetDate = null
            )
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
        }
    }
}
