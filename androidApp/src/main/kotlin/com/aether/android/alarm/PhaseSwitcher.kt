package com.aether.android.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aether.android.AetherApplication
import com.aether.core.data.AetherRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.data.RoutineRepository
import com.aether.core.engine.Phase
import com.aether.core.engine.PhaseSchedule
import com.aether.core.engine.TimetablePlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Moves to the next timetable of the user's plan on its start date. Checked when the app comes to the
 * front, after a reboot, and by a wake-up shortly after midnight on the next start date.
 */
class PhaseSwitcher(
    private val context: Context,
    private val repository: AetherRepository,
    private val routineRepository: RoutineRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val alarmScheduler: AlarmScheduler
) {
    private val mutex = Mutex()

    suspend fun applyDue() = mutex.withLock {
        val phases = personalizationRepository.observePhases().first()
        val (due, later) = PhaseSchedule.split(phases, LocalDate.now().toString())
        if (due != null) {
            apply(due)
            personalizationRepository.savePhases(later)
        }
        scheduleWakeUp(later.firstOrNull())
    }

    /** Replaces the stored upcoming phases (after an import, or when the user cancels them). */
    suspend fun setUpcoming(phases: List<Phase>) {
        mutex.withLock { personalizationRepository.savePhases(phases) }
        applyDue()
    }

    private suspend fun apply(phase: Phase) {
        val prefs = personalizationRepository.observePrefs().first().let {
            it.copy(
                wakeHour = phase.wakeMinute?.div(60) ?: it.wakeHour,
                sleepHour = phase.sleepMinute?.div(60) ?: it.sleepHour
            )
        }
        personalizationRepository.savePrefs(prefs)
        routineRepository.replaceWithImported(phase.blocks)
        val routines = routineRepository.observeRoutines().first()
        val plan = TimetablePlanner().plan(routines, prefs.wakeHour, prefs.sleepHour, prefs.selfChronotype?.peakStartHour)
        repository.regenerateSchedule(plan.slots)
        alarmScheduler.rescheduleAll(plan.slots)
    }

    private fun scheduleWakeUp(next: Phase?) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context, "aether_phase".hashCode(), Intent(context, PhaseReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val date = next?.let { runCatching { LocalDate.parse(it.startDate) }.getOrNull() }
        if (date == null) {
            manager.cancel(pending)
            return
        }
        val at = date.atTime(LocalTime.of(0, 5)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact on purpose: needs no exact-alarm permission, and a few minutes after midnight don't matter.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }
}

class PhaseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as AetherApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.phaseSwitcher.applyDue()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
