package com.aether.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.LifeScoreBreakdown
import com.aether.core.engine.LifeVisionProgress
import com.aether.core.engine.ScoringEngine
import com.aether.core.engine.Suggestion
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class DashboardUiState(
    val lifeScore: LifeScoreBreakdown = LifeScoreBreakdown(0, 0, 0, 0),
    val suggestion: Suggestion? = null,
    val activeGoals: List<Goal> = emptyList(),
    val missionDoneToday: Boolean = false,
    /** Oldest to newest, ending today — whether the mission was done that day. */
    val weekStrip: List<Boolean> = List(7) { false },
    val lifeVisionProgress: LifeVisionProgress? = null
)

class DashboardViewModel(
    private val repository: AetherRepository,
    private val goalsRepository: GoalsRepository,
    private val scoringEngine: ScoringEngine,
    private val contextEngine: ContextEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var latestCheckIns: List<DailyCheckIn> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                repository.observeRecentCheckIns(),
                goalsRepository.observeActiveGoals(),
                goalsRepository.observeAllTasks()
            ) { checkIns, goals, tasks -> Triple(checkIns, goals, tasks) }
                .collect { (checkIns, goals, tasks) ->
                    latestCheckIns = checkIns
                    val today = LocalDate.now().toString()
                    val todayCheckIn = checkIns.find { it.date == today }

                    val lifeScore = scoringEngine.compute(checkIns, goals)
                    val lifeVisionProgress = scoringEngine.computeLifeVisionProgress(goals, tasks)
                    val hour = LocalTime.now().hour
                    val suggestion = contextEngine.suggestNow(hour, todayCheckIn, goals)

                    val todayDate = LocalDate.now()
                    val weekStrip = (6 downTo 0).map { offset ->
                        val date = todayDate.minusDays(offset.toLong()).toString()
                        checkIns.find { it.date == date }?.executedMission == true
                    }

                    _uiState.value = DashboardUiState(
                        lifeScore = lifeScore,
                        suggestion = suggestion,
                        activeGoals = goals,
                        missionDoneToday = todayCheckIn?.executedMission == true,
                        weekStrip = weekStrip,
                        lifeVisionProgress = lifeVisionProgress
                    )
                }
        }
    }

    /** Marks (or unmarks) today's mission as done — this is what actually feeds Execution Score. */
    fun toggleMissionDone() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val todayCheckIn = latestCheckIns.find { it.date == today }
            repository.upsertTodayCheckIn(
                date = today,
                mood = todayCheckIn?.mood,
                energy = todayCheckIn?.energy,
                sleepHours = todayCheckIn?.sleepHours,
                executedMission = todayCheckIn?.executedMission != true
            )
        }
    }
}
