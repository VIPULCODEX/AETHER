package com.aether.android.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.model.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: AetherRepository) : ViewModel() {

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeJournalEntries().collect { _entries.value = it }
        }
    }

    fun addEntry(content: String, mood: Int?) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addJournalEntry(content.trim(), mood)
        }
    }
}
