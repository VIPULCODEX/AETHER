package com.aether.android.alarm

import android.content.Context

/** Whether the user has opted in to real wake-up alarms for their weekly timetable. */
class AlarmPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("aether_alarms", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Slot ids currently holding a live AlarmManager registration — tracked so a reschedule can cancel stale ones first. */
    fun getScheduledSlotIds(): Set<String> = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()) ?: emptySet()

    fun setScheduledSlotIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_SCHEDULED_IDS, ids).apply()
    }

    private companion object {
        const val KEY_ENABLED = "alarms_enabled"
        const val KEY_SCHEDULED_IDS = "scheduled_slot_ids"
    }
}
