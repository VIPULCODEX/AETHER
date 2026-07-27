package com.aether.android.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherRose
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.model.GoalType
import com.aether.core.model.Task
import com.aether.core.model.nextChildType
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
    var taskDraft by remember { mutableStateOf("") }
    var goalTypeDraft by remember(state.selectedGoalId) {
        mutableStateOf(state.selectedGoal?.goalType.nextChildType())
    }
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
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Text(
                        "Home",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.selectedGoalId == null) AetherSky else AetherTextSecondary,
                        modifier = Modifier.clickable { viewModel.selectGoal(null) }
                    )
                    state.breadcrumb.forEach { crumb ->
                        Text(
                            "  ›  ${crumb.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (crumb.id == state.selectedGoalId) AetherSky else AetherTextSecondary,
                            modifier = Modifier.clickable { viewModel.selectGoal(crumb.id) }
                        )
                    }
                }
            }

            item {
                Text(
                    if (state.selectedGoal == null) "Life Vision & Long Term Goals" else "Under \"${state.selectedGoal!!.title}\"",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.visibleGoals.isEmpty()) {
                item {
                    Text(
                        if (state.selectedGoalId == null) "No goals yet — start with your Life Vision below."
                        else "Nothing under this goal yet — break it down below, or log a Today's Action.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherTextSecondary
                    )
                }
            }

            items(state.visibleGoals) { goal ->
                AccentCard(
                    accentColor = AetherRose,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.selectGoal(goal.id) }
                ) {
                    Text(
                        GOAL_TYPE_LABELS[goal.goalType] ?: goal.goalType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherOnAccent.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${state.progressByGoalId[goal.id] ?: 0}% toward this · ${goal.domain}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherOnAccent
                    )
                }
            }

            state.selectedGoal?.let {
                item { Spacer(Modifier.height(8.dp)) }
                item { Text("Today's Actions", style = MaterialTheme.typography.titleMedium) }

                if (state.tasksForSelectedGoal.isEmpty()) {
                    item {
                        Text(
                            "No actions logged yet for this goal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherTextSecondary
                        )
                    }
                }

                items(state.tasksForSelectedGoal) { task ->
                    TaskRow(task = task, onToggle = { viewModel.toggleTaskDone(task) })
                }

                item {
                    OutlinedTextField(
                        value = taskDraft,
                        onValueChange = { taskDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a today's action") }
                    )
                }
                item {
                    Button(onClick = {
                        viewModel.addTask(taskDraft)
                        taskDraft = ""
                    }) {
                        Text("Add Action")
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    if (state.selectedGoal == null) "Add a Life Vision or Long Term goal" else "Break \"${state.selectedGoal!!.title}\" down further",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GoalType.entries) { type ->
                        FilterChip(
                            selected = goalTypeDraft == type,
                            onClick = { goalTypeDraft = type },
                            label = { Text(GOAL_TYPE_LABELS[type] ?: type.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AetherSky,
                                selectedLabelColor = AetherOnAccent
                            )
                        )
                    }
                }
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
                    viewModel.addGoal(titleDraft, goalTypeDraft, domainDraft)
                    titleDraft = ""
                    domainDraft = ""
                }) {
                    Text("Add Goal")
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
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
                                selectedLabelColor = AetherOnAccent
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
                            AccentCard(
                                accentColor = AetherSky,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    editingSlot = slot
                                    editText = slot.activityLabel
                                }
                            ) {
                                Text(slot.timeLabel, style = MaterialTheme.typography.labelSmall, color = AetherOnAccent)
                                Spacer(Modifier.height(4.dp))
                                Text(slot.activityLabel, style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                            }
                        }
                    }
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

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit) {
    AccentCard(
        accentColor = if (task.isDone) AetherEmerald else AetherCoral,
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Text(
            task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = AetherOnAccent,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

