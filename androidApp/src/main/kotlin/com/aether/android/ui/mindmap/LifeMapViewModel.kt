package com.aether.android.ui.mindmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.GoalsRepository
import com.aether.core.engine.MindMapEngine
import com.aether.core.model.MindNode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LifeMapViewModel(goalsRepository: GoalsRepository, val engine: MindMapEngine) : ViewModel() {
    val nodes: StateFlow<List<MindNode>> = combine(
        goalsRepository.observeActiveGoals(),
        goalsRepository.observeAllTasks()
    ) { goals, tasks -> engine.fromGoals(goals, tasks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
