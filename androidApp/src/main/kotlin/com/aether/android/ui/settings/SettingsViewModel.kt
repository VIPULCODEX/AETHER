package com.aether.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.android.alarm.AlarmPreferences
import com.aether.android.alarm.AlarmScheduler
import com.aether.android.data.ApiKeyStore
import com.aether.core.data.AetherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val alarmPreferences: AlarmPreferences,
    private val alarmScheduler: AlarmScheduler,
    private val repository: AetherRepository
) : ViewModel() {

    private val _apiKey = MutableStateFlow(apiKeyStore.getGroqKey() ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _alarmsEnabled = MutableStateFlow(alarmPreferences.isEnabled())
    val alarmsEnabled: StateFlow<Boolean> = _alarmsEnabled.asStateFlow()

    fun save(key: String) {
        apiKeyStore.setGroqKey(key)
        _apiKey.value = apiKeyStore.getGroqKey() ?: ""
    }

    fun clear() {
        apiKeyStore.setGroqKey(null)
        _apiKey.value = ""
    }

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExact()

    /** Turning this on (or off) immediately reschedules against whatever timetable currently exists. */
    fun setAlarmsEnabled(enabled: Boolean) {
        alarmPreferences.setEnabled(enabled)
        _alarmsEnabled.value = enabled
        viewModelScope.launch {
            val slots = repository.observeScheduleSlots().first()
            alarmScheduler.rescheduleAll(slots)
        }
    }
}
