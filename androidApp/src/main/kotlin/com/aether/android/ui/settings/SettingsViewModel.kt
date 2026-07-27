package com.aether.android.ui.settings

import androidx.lifecycle.ViewModel
import com.aether.android.data.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val apiKeyStore: ApiKeyStore) : ViewModel() {

    private val _apiKey = MutableStateFlow(apiKeyStore.getGroqKey() ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun save(key: String) {
        apiKeyStore.setGroqKey(key)
        _apiKey.value = apiKeyStore.getGroqKey() ?: ""
    }

    fun clear() {
        apiKeyStore.setGroqKey(null)
        _apiKey.value = ""
    }
}
