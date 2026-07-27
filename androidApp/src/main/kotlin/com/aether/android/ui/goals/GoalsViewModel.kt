package com.aether.android.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.engine.BasicScheduleGenerator
import com.aether.core.model.Goal
import com.aether.core.model.ScheduleSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

val STANDARD_FOCUS_AREAS = listOf("Gym", "Research", "GATE", "JEE", "College Work")

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val focusAreas: List<String> = emptyList(),
    val scheduleSlots: List<ScheduleSlot> = emptyList()
)

class GoalsViewModel(private val repository: AetherRepository) : ViewModel() {

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
                    _uiState.value = GoalsUiState(goals, focusAreas, slots)
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

    fun editSlot(id: String, newLabel: String) {
        if (newLabel.isBlank()) return
        viewModelScope.launch {
            repository.updateScheduleSlotLabel(id, newLabel.trim())
        }
    }
}
