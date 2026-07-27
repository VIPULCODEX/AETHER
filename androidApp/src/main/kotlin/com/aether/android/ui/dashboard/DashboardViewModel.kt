package com.aether.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.LifeScoreBreakdown
import com.aether.core.engine.ScoringEngine
import com.aether.core.engine.Suggestion
import com.aether.core.model.Goal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalTime

data class DashboardUiState(
    val lifeScore: LifeScoreBreakdown = LifeScoreBreakdown(0, 0, 0, 0),
    val suggestion: Suggestion? = null,
    val activeGoals: List<Goal> = emptyList()
)

class DashboardViewModel(
    private val repository: AetherRepository,
    private val scoringEngine: ScoringEngine,
    private val contextEngine: ContextEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeRecentCheckIns(),
                repository.observeActiveGoals()
            ) { checkIns, goals -> checkIns to goals }
                .collect { (checkIns, goals) ->
                    val lifeScore = scoringEngine.compute(checkIns, goals)
                    val hour = LocalTime.now().hour
                    val suggestion = contextEngine.suggestNow(hour, checkIns.firstOrNull(), goals)
                    _uiState.value = DashboardUiState(lifeScore, suggestion, goals)
                }
        }
    }
}
