package com.aether.android.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aether.android.R
import com.aether.core.model.ScheduleSlot

// Channel sound is immutable once created, so the branded-sound channel gets a new id (and the old one is removed).
const val ALARM_CHANNEL_ID = "aether_alarms_v2"
private const val LEGACY_ALARM_CHANNEL_ID = "aether_alarms"
private const val ACCENT = 0xFF6C5CE7.toInt()
const val ACTION_SNOOZE = "com.aether.android.action.SNOOZE"
const val ACTION_DISMISS = "com.aether.android.action.DISMISS"
const val NOTIFICATION_ID_BASE_PUBLIC = 90_000
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

        fun action(action: String, label: String, offset: Int): NotificationCompat.Action {
            val intent = Intent(context, AlarmActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_SLOT_ID, slotId)
                putExtra(EXTRA_ACTIVITY_LABEL, activityLabel)
            }
            val pending = PendingIntent.getBroadcast(
                context, slotId.hashCode() * 4 + offset, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action.Builder(0, label, pending).build()
        }

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aether)
            .setColor(ACCENT)
            .setContentTitle(activityLabel)
            .setContentText("Starts now. Tap to open.")
            .setSubText("AETHER")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(activityLabel)
                    .bigText("Your $activityLabel block starts now.\nOpen it, or push it back 10 minutes.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(action(ACTION_SNOOZE, "Snooze 10 min", 1))
            .addAction(action(ACTION_DISMISS, "Dismiss", 2))
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + slotId.hashCode(), notification)
    }

    companion object {
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel(LEGACY_ALARM_CHANNEL_ID)
            if (manager.getNotificationChannel(ALARM_CHANNEL_ID) != null) return
            val chime = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.aether_chime}")
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Timetable alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake-up alarms for your generated weekly timetable."
                setSound(
                    chime,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                // tick-tick-hold: matches the chime's three rising notes.
                vibrationPattern = longArrayOf(0, 60, 50, 60, 50, 240)
                enableLights(true)
                lightColor = Color.rgb(108, 92, 231)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
