package com.aether.android.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aether.android.AetherApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** AlarmManager exact alarms are cleared on reboot — this puts them back if the user opted in. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as AetherApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val slots = app.repository.observeScheduleSlots().first()
                AlarmScheduler(context).rescheduleAll(slots)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
