package com.aether.android.ui.personalize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.engine.LearnedProfile
import com.aether.core.engine.PersonalizationEngine
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

data class PersonalizeUiState(
    val prefs: PersonalizationPrefs = PersonalizationPrefs(),
    val learned: LearnedProfile = LearnedProfile.EMPTY
)

class PersonalizeViewModel(
    repository: AetherRepository,
    goalsRepository: GoalsRepository,
    private val personalizationRepository: PersonalizationRepository,
    personalizationEngine: PersonalizationEngine
) : ViewModel() {

    private var latest = PersonalizationPrefs()

    val uiState: StateFlow<PersonalizeUiState> = combine(
        personalizationRepository.observePrefs(),
        personalizationRepository.observeBehaviorEvents(),
        goalsRepository.observeActiveGoals(),
        goalsRepository.observeAllTasks(),
        repository.observeRecentCheckIns(60)
    ) { prefs, events, goals, tasks, checkIns ->
        latest = prefs
        val learned = if (prefs.learningEnabled) {
            personalizationEngine.learn(tasks, goals, checkIns, events, System.currentTimeMillis()) { millis ->
                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour
            }
        } else {
            LearnedProfile.EMPTY
        }
        PersonalizeUiState(prefs, learned)
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
