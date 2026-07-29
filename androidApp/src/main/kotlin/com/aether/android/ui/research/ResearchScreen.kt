package com.aether.android.ui.research

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.EmptyState
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.components.openAttachment
import com.aether.android.ui.components.rememberAttachmentPicker
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.model.ResearchNote

@Composable
fun ResearchScreen(
    viewModel: ResearchViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AetherScaffold(title = "Research") {
        if (!state.researchIsFocusArea) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EmptyState(
                        emoji = "🔬",
                        title = "Research isn't active yet",
                        subtitle = "Select \"Research\" as a focus area in Goals to unlock papers, notes and ideas here."
                    )
                }
                item {
                    AetherButton(text = "Go to Goals", onClick = { onNavigate("goals") }, modifier = Modifier.fillMaxWidth())
                }
            }
            return@AetherScaffold
        }

        var titleDraft by remember { mutableStateOf("") }
        var noteDraft by remember { mutableStateOf("") }
        var statusDraft by remember { mutableStateOf(RESEARCH_STATUSES.first()) }
        var attachmentUri by remember { mutableStateOf<String?>(null) }
        val pickPdf = rememberAttachmentPicker(arrayOf("application/pdf")) { uri -> attachmentUri = uri.toString() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Log a paper, idea, or note", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paper title or idea") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Notes, summary, next step") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
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
                    Spacer(Modifier.height(10.dp))
                    AetherOutlinedButton(
                        text = if (attachmentUri == null) "Attach the PDF" else "PDF attached",
                        onClick = pickPdf
                    )
                    Spacer(Modifier.height(14.dp))
                    AetherButton(
                        text = "Add",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.addNote(titleDraft, noteDraft, statusDraft, attachmentUri)
                            titleDraft = ""
                            noteDraft = ""
                            attachmentUri = null
                        }
                    )
                }
            }

            if (state.notes.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📄",
                        title = "Nothing logged yet",
                        subtitle = "Every paper you read or idea you have starts here."
                    )
                }
            } else {
                RESEARCH_STATUSES.forEach { status ->
                    val notesForStatus = state.notes.filter { it.status == status }
                    if (notesForStatus.isNotEmpty()) {
                        item { SectionHeader("$status  ·  ${notesForStatus.size}") }
                        items(notesForStatus) { note -> ResearchNoteRow(note) }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ResearchNoteRow(note: ResearchNote) {
    val context = LocalContext.current
    AetherCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = note.attachmentUri?.let { uri -> { openAttachment(context, uri) } }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                if (note.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(note.note, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                }
            }
            note.attachmentUri?.let {
                Spacer(Modifier.width(8.dp))
                Text("📄", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
