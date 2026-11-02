package com.aether.android.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the Snooze / Dismiss buttons on the alarm notification without opening the app. */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getStringExtra(EXTRA_SLOT_ID) ?: return
        val label = intent.getStringExtra(EXTRA_ACTIVITY_LABEL) ?: "Scheduled block"
        if (intent.action == ACTION_SNOOZE) {
            AlarmScheduler(context).scheduleSnooze(slotId, label)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_BASE_PUBLIC + slotId.hashCode())
    }
}
