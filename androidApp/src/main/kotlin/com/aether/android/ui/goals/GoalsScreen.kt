package com.aether.android.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.BlockCard
import com.aether.android.ui.theme.AetherInk
import com.aether.android.ui.theme.AetherMagenta
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.model.ScheduleSlot

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var titleDraft by remember { mutableStateOf("") }
    var domainDraft by remember { mutableStateOf("") }
    var editingSlot by remember { mutableStateOf<ScheduleSlot?>(null) }
    var editText by remember { mutableStateOf("") }
    var aiDescription by remember { mutableStateOf("") }

    AetherScaffold(title = "Goals", currentRoute = "goals", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "What are you focusing on?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(STANDARD_FOCUS_AREAS) { area ->
                        val selected = state.focusAreas.contains(area)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.toggleFocusArea(area) },
                            label = { Text(area) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AetherSky,
                                selectedLabelColor = AetherInk
                            )
                        )
                    }
                }
            }

            if (state.focusAreas.isNotEmpty()) {
                item {
                    Button(onClick = { viewModel.regenerateSchedule() }) {
                        Text(if (state.scheduleSlots.isEmpty()) "Generate timetable" else "Regenerate timetable")
                    }
                }
                item {
                    Text(
                        "Basic auto-generated split above. Tap any slot to edit it — or describe your " +
                            "actual routine below and let AI build a better one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherTextSecondary
                    )
                }
                item {
                    OutlinedTextField(
                        value = aiDescription,
                        onValueChange = { aiDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. College 9-5 on weekdays, sleep by 11pm, prefer gym in the evening") }
                    )
                }
                item {
                    Button(
                        onClick = { viewModel.generateWithAi(aiDescription) },
                        enabled = !state.isGeneratingWithAi
                    ) {
                        if (state.isGeneratingWithAi) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text("Generate with AI")
                        }
                    }
                }
                state.aiErrorMessage?.let { error ->
                    item {
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                    }
                }
            }

            if (state.scheduleSlots.isNotEmpty()) {
                for (day in 0..6) {
                    val daySlots = state.scheduleSlots.filter { it.dayOfWeek == day }
                    if (daySlots.isNotEmpty()) {
                        item {
                            Text(
                                DAY_NAMES[day],
                                style = MaterialTheme.typography.labelSmall,
                                color = AetherTextSecondary
                            )
                        }
                        items(daySlots) { slot ->
                            BlockCard(
                                accentColor = AetherSky,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    editingSlot = slot
                                    editText = slot.activityLabel
                                }
                            ) {
                                Text(slot.timeLabel, style = MaterialTheme.typography.labelSmall, color = AetherInk)
                                Spacer(Modifier.height(4.dp))
                                Text(slot.activityLabel, style = MaterialTheme.typography.titleMedium, color = AetherInk)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text("Add a specific goal", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What are you working toward?") }
                )
            }
            item {
                OutlinedTextField(
                    value = domainDraft,
                    onValueChange = { domainDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Domain (e.g. GATE, Research, Gym)") }
                )
            }
            item {
                Button(onClick = {
                    viewModel.addGoal(titleDraft, domainDraft)
                    titleDraft = ""
                    domainDraft = ""
                }) {
                    Text("Add Goal")
                }
            }

            items(state.goals) { goal ->
                BlockCard(accentColor = AetherMagenta, modifier = Modifier.fillMaxWidth()) {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, color = AetherInk)
                    Spacer(Modifier.height(4.dp))
                    Text(goal.domain, style = MaterialTheme.typography.bodyMedium, color = AetherInk)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editingSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("Edit ${DAY_NAMES[slot.dayOfWeek]} · ${slot.timeLabel}") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editSlot(slot.id, editText)
                    editingSlot = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) { Text("Cancel") }
            }
        )
    }
}
