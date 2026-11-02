package com.aether.android.ui.personalize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.data.RoutineRepository
import com.aether.core.engine.LearnedProfile
import com.aether.core.engine.PersonalizationEngine
import com.aether.core.engine.routineSignals
import com.aether.core.model.Chronotype
import com.aether.core.model.CoachTone
import com.aether.core.model.Persona
import com.aether.core.model.PersonalizationPrefs
import com.aether.core.model.PlanningStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private data class Inputs(
    val prefs: PersonalizationPrefs,
    val events: List<com.aether.core.model.BehaviorEvent>,
    val goals: List<com.aether.core.model.Goal>,
    val tasks: List<com.aether.core.model.Task>,
    val checkIns: List<com.aether.core.model.DailyCheckIn>
)

data class PersonalizeUiState(
    val prefs: PersonalizationPrefs = PersonalizationPrefs(),
    val learned: LearnedProfile = LearnedProfile.EMPTY
)

class PersonalizeViewModel(
    repository: AetherRepository,
    goalsRepository: GoalsRepository,
    private val personalizationRepository: PersonalizationRepository,
    personalizationEngine: PersonalizationEngine,
    routineRepository: RoutineRepository
) : ViewModel() {

    private var latest = PersonalizationPrefs()

    val uiState: StateFlow<PersonalizeUiState> = combine(
        combine(
            personalizationRepository.observePrefs(),
            personalizationRepository.observeBehaviorEvents(),
            goalsRepository.observeActiveGoals(),
            goalsRepository.observeAllTasks(),
            repository.observeRecentCheckIns(60)
        ) { prefs, events, goals, tasks, checkIns -> Inputs(prefs, events, goals, tasks, checkIns) },
        routineRepository.observeRoutines(),
        routineRepository.observeLogsSince(java.time.LocalDate.now().minusDays(30).toString())
    ) { inputs, routines, logs ->
        latest = inputs.prefs
        val learned = if (inputs.prefs.learningEnabled) {
            personalizationEngine.learn(
                inputs.tasks, inputs.goals, inputs.checkIns, inputs.events, System.currentTimeMillis(),
                { millis -> Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour },
                routineSignals(routines, logs, java.time.LocalDate.now().toString())
            )
        } else {
            LearnedProfile.EMPTY
        }
        PersonalizeUiState(inputs.prefs, learned)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalizeUiState())

    fun setPersona(persona: Persona) = update { it.withPersona(persona) }
    fun setTone(tone: CoachTone) = update { it.copy(tone = tone) }
    fun setPlanningStyle(style: PlanningStyle) = update { it.copy(planningStyle = style) }

    /** null = "learn it from my behavior". */
    fun setChronotype(chronotype: Chronotype?) = update { it.copy(selfChronotype = chronotype) }
    fun setWakeHour(hour: Int) = update { it.copy(wakeHour = hour.mod(24)) }
    fun setSleepHour(hour: Int) = update { it.copy(sleepHour = hour.mod(24)) }
    fun setLearningEnabled(enabled: Boolean) = update { it.copy(learningEnabled = enabled) }

    fun toggleModule(module: String) = update { prefs ->
        val modules = if (module in prefs.enabledModules) prefs.enabledModules - module else prefs.enabledModules + module
        prefs.copy(enabledModules = modules)
    }

    fun resetLearning() {
        viewModelScope.launch { personalizationRepository.resetLearning() }
    }

    private fun update(change: (PersonalizationPrefs) -> PersonalizationPrefs) {
        viewModelScope.launch {
            // Touching any setting is itself the user personalizing, so the Dashboard prompt can retire.
            personalizationRepository.savePrefs(change(latest).copy(onboarded = true))
        }
    }
}
