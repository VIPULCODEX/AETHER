package com.aether.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.aether.android.ui.dashboard.DashboardViewModel
import com.aether.android.ui.goals.GoalsViewModel
import com.aether.android.ui.journal.JournalViewModel
import com.aether.core.data.AetherRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.ScoringEngine

class AetherViewModelFactory(
    private val repository: AetherRepository,
    private val scoringEngine: ScoringEngine,
    private val contextEngine: ContextEngine
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when (modelClass) {
            DashboardViewModel::class.java ->
                DashboardViewModel(repository, scoringEngine, contextEngine) as T

            JournalViewModel::class.java ->
                JournalViewModel(repository) as T

            GoalsViewModel::class.java ->
                GoalsViewModel(repository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}
