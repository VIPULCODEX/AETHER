package com.aether.android.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.model.Goal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoalsViewModel(private val repository: AetherRepository) : ViewModel() {

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeActiveGoals().collect { _goals.value = it }
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
}
