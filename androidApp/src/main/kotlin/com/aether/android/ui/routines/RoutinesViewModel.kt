package com.aether.android.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.android.alarm.AlarmScheduler
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.data.RoutineRepository
import com.aether.core.engine.PersonalizationEngine
import com.aether.core.engine.TimetablePlanner
import com.aether.core.engine.routineSignals
import com.aether.core.model.Routine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class RoutinesViewModel(
    private val routineRepository: RoutineRepository,
    private val repository: AetherRepository,
    private val goalsRepository: GoalsRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val personalizationEngine: PersonalizationEngine,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val planner = TimetablePlanner()

    val routines: StateFlow<List<Routine>> = routineRepository.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addRoutine(title: String, domain: String, minutes: Int, daysMask: Int, fixedStartMinute: Int?) {
        if (title.isBlank() || daysMask == 0) return
        viewModelScope.launch {
            routineRepository.addRoutine(title, domain, minutes, daysMask, fixedStartMinute)
            rebuildTimetable()
        }
    }

    fun removeRoutine(id: String) {
        viewModelScope.launch {
            routineRepository.archiveRoutine(id)
            rebuildTimetable()
        }
    }

    /**
     * Re-plans the whole week from the current routines: waking hours from Personalize, and the peak-focus window
     * from what AETHER has learned (or the chronotype you set). Replaces the existing timetable and its alarms.
     */
    fun rebuildTimetable() {
        viewModelScope.launch {
            val routines = routineRepository.observeRoutines().first()
            if (routines.isEmpty()) {
                _message.value = "Add a routine first."
                return@launch
            }
            val prefs = personalizationRepository.observePrefs().first()
            val goals = goalsRepository.observeActiveGoals().first()
            val tasks = goalsRepository.observeAllTasks().first()
            val events = personalizationRepository.observeBehaviorEvents().first()
            val today = java.time.LocalDate.now()
            val logs = routineRepository.observeLogsSince(today.minusDays(30).toString()).first()
            val learned = if (prefs.learningEnabled) {
                personalizationEngine.learn(
                    tasks, goals, emptyList(), events, System.currentTimeMillis(),
                    { millis -> Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour },
                    routineSignals(routines, logs, today.toString())
                )
            } else null
            val peak = prefs.selfChronotype?.peakStartHour ?: learned?.peakStartHour

            val plan = planner.plan(routines, prefs.wakeHour, prefs.sleepHour, peak)
            repository.regenerateSchedule(plan.slots)
            alarmScheduler.rescheduleAll(plan.slots)
            _message.value = if (plan.unplaced.isEmpty()) {
                "Timetable rebuilt: ${plan.slots.size} blocks this week."
            } else {
                "Rebuilt, but there wasn't room for: ${plan.unplaced.take(4).joinToString(", ")}" +
                    if (plan.unplaced.size > 4) " and ${plan.unplaced.size - 4} more." else "."
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
