package com.aether.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.LearnedProfile
import com.aether.core.engine.PersonalizationEngine
import com.aether.core.engine.SuggestionContext
import com.aether.core.engine.LifeScoreBreakdown
import com.aether.core.engine.LifeVisionProgress
import com.aether.core.engine.ScoringEngine
import com.aether.core.engine.Suggestion
import com.aether.core.model.BehaviorEvent
import com.aether.core.model.BehaviorEventType
import com.aether.core.model.BodyLog
import com.aether.core.model.DailyCheckIn
import com.aether.core.model.Goal
import com.aether.core.model.PersonalizationPrefs
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
    val researchNoteCount: Int = 0,
    val prefs: PersonalizationPrefs = PersonalizationPrefs(),
    val learned: LearnedProfile = LearnedProfile.EMPTY
)

class DashboardViewModel(
    private val repository: AetherRepository,
    private val goalsRepository: GoalsRepository,
    private val scoringEngine: ScoringEngine,
    private val contextEngine: ContextEngine,
    private val personalizationRepository: PersonalizationRepository,
    private val personalizationEngine: PersonalizationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var latestCheckIns: List<DailyCheckIn> = emptyList()
    private var latestPrefs = PersonalizationPrefs()

    /** Tasks/goals the user tapped "Not now" on this session, so the next-best item surfaces instead. */
    private val skipped = MutableStateFlow<Set<String>>(emptySet())

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

            val personal = combine(
                personalizationRepository.observePrefs(),
                personalizationRepository.observeBehaviorEvents(),
                skipped
            ) { prefs, events, skippedIds -> Personal(prefs, events, skippedIds) }

            combine(core, extra, personal) { (checkIns, goals, tasks), extraData, personalData ->
                Snapshot(checkIns, goals, tasks, extraData, personalData)
            }
                .collect { (checkIns, goals, tasks, extraData, personalData) ->
                    latestCheckIns = checkIns
                    latestPrefs = personalData.prefs
                    val today = LocalDate.now().toString()
                    val todayCheckIn = checkIns.find { it.date == today }

                    val lifeScore = scoringEngine.compute(checkIns, goals)
                    val lifeVisionProgress = scoringEngine.computeLifeVisionProgress(goals, tasks)
                    val hour = LocalTime.now().hour
                    val nowMillis = System.currentTimeMillis()
                    val learned = if (personalData.prefs.learningEnabled) {
                        personalizationEngine.learn(tasks, goals, checkIns, personalData.events, nowMillis, ::hourOf)
                    } else {
                        LearnedProfile.EMPTY
                    }
                    val suggestion = contextEngine.suggestNow(
                        SuggestionContext(
                            nowMillis = nowMillis,
                            hourOfDay = hour,
                            todayCheckIn = todayCheckIn,
                            goals = goals,
                            openTasks = tasks.filter { !it.isDone },
                            prefs = personalData.prefs,
                            learned = learned,
                            skipped = personalData.skipped
                        )
                    )

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
                        researchNoteCount = extraData.researchNoteCount,
                        prefs = personalData.prefs,
                        learned = learned
                    )
                }
        }
    }

    /** Marks (or unmarks) today's mission as done — this is what actually feeds Execution Score. */
    fun toggleMissionDone() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val todayCheckIn = latestCheckIns.find { it.date == today }
            val nowDone = todayCheckIn?.executedMission != true
            repository.upsertTodayCheckIn(
                date = today,
                mood = todayCheckIn?.mood,
                energy = todayCheckIn?.energy,
                sleepHours = todayCheckIn?.sleepHours,
                executedMission = nowDone
            )
            // Finishing the suggested mission is the strongest "this was right for me" signal.
            if (nowDone) recordFeedback(BehaviorEventType.SUGGESTION_ACCEPTED)
        }
    }

    /** "Not now": remember the rejection (so similar suggestions get rarer) and surface the next-best item. */
    fun dismissSuggestion() {
        val suggestion = _uiState.value.suggestion ?: return
        val id = suggestion.taskId ?: suggestion.goalId ?: return
        skipped.value = skipped.value + id
        viewModelScope.launch { recordFeedback(BehaviorEventType.SUGGESTION_DISMISSED) }
    }

    private suspend fun recordFeedback(type: BehaviorEventType) {
        val suggestion = _uiState.value.suggestion ?: return
        if (!latestPrefs.learningEnabled || suggestion.domain == null) return
        personalizationRepository.recordEvent(type, suggestion.domain, LocalTime.now().hour)
    }

    private data class Snapshot(
        val checkIns: List<DailyCheckIn>,
        val goals: List<Goal>,
        val tasks: List<com.aether.core.model.Task>,
        val extra: Extra,
        val personal: Personal
    )

    private data class Personal(
        val prefs: PersonalizationPrefs,
        val events: List<BehaviorEvent>,
        val skipped: Set<String>
    )

    private data class Extra(
        val focusAreas: List<String>,
        val journalEntryCount: Int,
        val latestBodyLog: BodyLog?,
        val latestResearchNote: ResearchNote?,
        val researchNoteCount: Int,
        val journalEntriesThisWeek: Int
    )

    private fun hourOf(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour

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
