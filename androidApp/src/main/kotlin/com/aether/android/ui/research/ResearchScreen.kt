package com.aether.android.ui.research

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextSecondary

@Composable
fun ResearchScreen(
    viewModel: ResearchViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AetherScaffold(title = "Research", currentRoute = "research", onNavigate = onNavigate) {
        if (!state.researchIsFocusArea) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Select \"Research\" as a focus area in Goals to unlock this.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                item {
                    Button(onClick = { onNavigate("goals") }) {
                        Text("Go to Goals")
                    }
                }
            }
            return@AetherScaffold
        }

        var titleDraft by remember { mutableStateOf("") }
        var noteDraft by remember { mutableStateOf("") }
        var statusDraft by remember { mutableStateOf(RESEARCH_STATUSES.first()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Log a paper, idea, or note", style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paper title or idea") }
                )
            }
            item {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Notes, summary, next step") }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RESEARCH_STATUSES) { status ->
                        FilterChip(
                            selected = statusDraft == status,
                            onClick = { statusDraft = status },
                            label = { Text(status) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AetherSky,
                                selectedLabelColor = AetherOnAccent
                            )
                        )
                    }
                }
            }
            item {
                Button(onClick = {
                    viewModel.addNote(titleDraft, noteDraft, statusDraft)
                    titleDraft = ""
                    noteDraft = ""
                }) {
                    Text("Add")
                }
            }

            if (state.notes.isEmpty()) {
                item {
                    Text(
                        "Nothing logged yet — every paper you read or idea you have starts here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherTextSecondary
                    )
                }
            }

            items(state.notes) { note ->
                AccentCard(accentColor = AetherSky, modifier = Modifier.fillMaxWidth()) {
                    Text(note.status, style = MaterialTheme.typography.labelSmall, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text(note.title, style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    if (note.note.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(note.note, style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
