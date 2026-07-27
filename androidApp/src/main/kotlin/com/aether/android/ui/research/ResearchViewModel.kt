package com.aether.android.ui.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.model.ResearchNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

val RESEARCH_STATUSES = listOf("Idea", "Reading", "Read", "Writing")

data class ResearchUiState(
    val researchIsFocusArea: Boolean = false,
    val notes: List<ResearchNote> = emptyList()
)

class ResearchViewModel(private val repository: AetherRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ResearchUiState())
    val uiState: StateFlow<ResearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeFocusAreas(),
                repository.observeResearchNotes()
            ) { focusAreas, notes -> focusAreas.contains("Research") to notes }
                .collect { (researchIsFocusArea, notes) ->
                    _uiState.value = ResearchUiState(researchIsFocusArea, notes)
                }
        }
    }

    fun addNote(title: String, note: String, status: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addResearchNote(title.trim(), note.trim(), status)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteResearchNote(id)
        }
    }
}
