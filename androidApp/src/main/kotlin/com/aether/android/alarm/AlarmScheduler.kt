package com.aether.android.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aether.core.model.ScheduleSlot
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val EXTRA_SLOT_ID = "slot_id"
const val EXTRA_ACTIVITY_LABEL = "activity_label"
const val EXTRA_DAY_OF_WEEK = "day_of_week"
const val EXTRA_TIME_LABEL = "time_label"

/**
 * Turns weekly [ScheduleSlot]s into real `AlarmManager` wake-ups. Exact
 * alarms don't repeat natively without accumulating drift, so the standard
 * pattern is used instead: schedule the single next occurrence, and once
 * [AlarmReceiver] fires it reschedules the same slot 7 days out.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = AlarmPreferences(context)

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    /** Cancels every currently-scheduled slot alarm, then — if enabled and permitted — schedules fresh ones for [slots]. */
    fun rescheduleAll(slots: List<ScheduleSlot>) {
        preferences.getScheduledSlotIds().forEach { cancel(it) }
        if (!preferences.isEnabled() || !canScheduleExact()) {
            preferences.setScheduledSlotIds(emptySet())
            return
        }
        slots.forEach { schedule(it) }
        preferences.setScheduledSlotIds(slots.map { it.id }.toSet())
    }

    fun schedule(slot: ScheduleSlot) {
        val triggerAt = nextOccurrence(slot.dayOfWeek, slot.timeLabel) ?: return
        val pendingIntent = pendingIntentFor(slot)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (e: SecurityException) {
            // Permission revoked between our check and this call — nothing more we can do here.
        }
    }

    /** A one-off re-ring some minutes from now — used for "Snooze", not tied into the weekly reschedule set. */
    fun scheduleSnooze(slotId: String, activityLabel: String, delayMinutes: Int = 10) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SLOT_ID, "${slotId}_snooze")
            putExtra(EXTRA_ACTIVITY_LABEL, activityLabel)
            putExtra(EXTRA_DAY_OF_WEEK, -1)
            putExtra(EXTRA_TIME_LABEL, "")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, "${slotId}_snooze".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (e: SecurityException) {
            // Permission revoked between our check and this call — nothing more we can do here.
        }
    }

    fun cancel(slotId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, slotId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun pendingIntentFor(slot: ScheduleSlot): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SLOT_ID, slot.id)
            putExtra(EXTRA_ACTIVITY_LABEL, slot.activityLabel)
            putExtra(EXTRA_DAY_OF_WEEK, slot.dayOfWeek)
            putExtra(EXTRA_TIME_LABEL, slot.timeLabel)
        }
        return PendingIntent.getBroadcast(
            context, slot.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        /** [dayOfWeek] is 0=Monday..6=Sunday, matching Goals' day ordering. Returns epoch millis of the next match. */
        fun nextOccurrence(dayOfWeek: Int, timeLabel: String): Long? {
            val startText = timeLabel.substringBefore("-").trim()
            val time = runCatching { LocalTime.parse(startText, TIME_FORMAT) }.getOrNull() ?: return null
            val targetDow = DayOfWeek.of(dayOfWeek + 1)

            val now = LocalDateTime.now()
            var date = now.toLocalDate()
            var candidate = LocalDateTime.of(date, time)
            while (candidate.dayOfWeek != targetDow || candidate.isBefore(now)) {
                date = date.plusDays(1)
                candidate = LocalDateTime.of(date, time)
            }
            return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}
