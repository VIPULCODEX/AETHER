package com.aether.android.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aether.core.model.ScheduleSlot

const val ALARM_CHANNEL_ID = "aether_alarms"
private const val NOTIFICATION_ID_BASE = 90_000

/**
 * Fires when a scheduled slot's time arrives. Alarm apps can't reliably
 * start an Activity directly from a background broadcast receiver on modern
 * Android, so the correct pattern is used instead: post a high-priority
 * notification with a full-screen intent, which the system is allowed to
 * launch over the lock screen even from the background.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getStringExtra(EXTRA_SLOT_ID) ?: return
        val activityLabel = intent.getStringExtra(EXTRA_ACTIVITY_LABEL) ?: "Scheduled block"
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, 0)
        val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL) ?: ""

        ensureChannel(context)
        showAlarmNotification(context, slotId, activityLabel)

        // Exact alarms don't repeat on their own — line up next week's occurrence now.
        // A negative dayOfWeek marks a one-off snooze re-ring, which has no "next week" to schedule.
        if (dayOfWeek >= 0) {
            AlarmScheduler(context).schedule(
                ScheduleSlot(id = slotId, dayOfWeek = dayOfWeek, timeLabel = timeLabel, activityLabel = activityLabel, domain = "")
            )
        }
    }

    private fun showAlarmNotification(context: Context, slotId: String, activityLabel: String) {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_SLOT_ID, slotId)
            putExtra(EXTRA_ACTIVITY_LABEL, activityLabel)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, slotId.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("AETHER · $activityLabel")
            .setContentText("It's time — tap to open.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + slotId.hashCode(), notification)
    }

    companion object {
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(ALARM_CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Timetable alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake-up alarms for your generated weekly timetable."
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
