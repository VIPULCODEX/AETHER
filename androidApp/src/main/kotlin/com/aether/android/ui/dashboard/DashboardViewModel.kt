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
import com.aether.core.model.BodyLog
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.ResearchNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class DashboardUiState(
    val lifeScore: LifeScoreBreakdown = LifeScoreBreakdown(0, 0, 0, 0),
    val suggestion: Suggestion? = null,
    val activeGoals: List<Goal> = emptyList(),
    val missionDoneToday: Boolean = false,
    /** Oldest to newest, ending today — whether the mission was done that day. */
    val weekStrip: List<Boolean> = List(7) { false },
    val lifeVisionProgress: LifeVisionProgress? = null,
    /** This-week executed count minus the week before — the identity-over-streaks trend line. */
    val weekOverWeekDelta: Int = 0,
    val focusAreas: List<String> = emptyList(),
    val journalEntriesThisWeek: Int = 0,
    val latestBodyLog: BodyLog? = null,
    val latestResearchNote: ResearchNote? = null,
    val researchNoteCount: Int = 0
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
            val core = combine(
                repository.observeRecentCheckIns(),
                goalsRepository.observeActiveGoals(),
                goalsRepository.observeAllTasks()
            ) { checkIns, goals, tasks -> Triple(checkIns, goals, tasks) }

            val extra = combine(
                repository.observeFocusAreas(),
                repository.observeJournalEntries(),
                repository.observeBodyLogs(),
                repository.observeResearchNotes()
            ) { focusAreas, journalEntries, bodyLogs, researchNotes ->
                Extra(focusAreas, journalEntries.size, bodyLogs.firstOrNull(), researchNotes.firstOrNull(), researchNotes.size, journalEntries.count(::isThisWeek))
            }

            combine(core, extra) { (checkIns, goals, tasks), extraData ->
                Snapshot(checkIns, goals, tasks, extraData)
            }
                .collect { (checkIns, goals, tasks, extraData) ->
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
                    val thisWeekCount = weekStrip.count { it }
                    val prevWeekCount = (13 downTo 7).count { offset ->
                        val date = todayDate.minusDays(offset.toLong()).toString()
                        checkIns.find { it.date == date }?.executedMission == true
                    }

                    _uiState.value = DashboardUiState(
                        lifeScore = lifeScore,
                        suggestion = suggestion,
                        activeGoals = goals,
                        missionDoneToday = todayCheckIn?.executedMission == true,
                        weekStrip = weekStrip,
                        lifeVisionProgress = lifeVisionProgress,
                        weekOverWeekDelta = thisWeekCount - prevWeekCount,
                        focusAreas = extraData.focusAreas,
                        journalEntriesThisWeek = extraData.journalEntriesThisWeek,
                        latestBodyLog = extraData.latestBodyLog,
                        latestResearchNote = extraData.latestResearchNote,
                        researchNoteCount = extraData.researchNoteCount
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

    private data class Snapshot(
        val checkIns: List<DailyCheckIn>,
        val goals: List<Goal>,
        val tasks: List<com.aether.core.model.Task>,
        val extra: Extra
    )

    private data class Extra(
        val focusAreas: List<String>,
        val journalEntryCount: Int,
        val latestBodyLog: BodyLog?,
        val latestResearchNote: ResearchNote?,
        val researchNoteCount: Int,
        val journalEntriesThisWeek: Int
    )

    private fun isThisWeek(entry: com.aether.core.model.JournalEntry): Boolean {
        val entryDate = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        return !entryDate.isBefore(LocalDate.now().minusDays(6))
    }
}

/** Time-of-day greeting — small touch, but a static header is the first thing that reads as unpolished. */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning."
    in 12..16 -> "Good afternoon."
    in 17..20 -> "Good evening."
    else -> "Still up."
}
