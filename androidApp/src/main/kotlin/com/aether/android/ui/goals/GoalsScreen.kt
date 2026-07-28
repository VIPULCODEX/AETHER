package com.aether.android.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.EmptyState
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.theme.AetherBorder
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherRose
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.model.GoalType
import com.aether.core.model.ScheduleSlot
import com.aether.core.model.Task
import com.aether.core.model.nextChildType

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

    AetherScaffoldGoals(onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BreadcrumbChip(
                        label = "Home",
                        selected = state.selectedGoalId == null,
                        onClick = { viewModel.selectGoal(null) }
                    )
                    state.breadcrumb.forEach { crumb ->
                        BreadcrumbChip(
                            label = crumb.title,
                            selected = crumb.id == state.selectedGoalId,
                            onClick = { viewModel.selectGoal(crumb.id) }
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    if (state.selectedGoal == null) "Life Vision & Long Term Goals" else "Under \"${state.selectedGoal!!.title}\""
                )
            }

            if (state.visibleGoals.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🎯",
                        title = if (state.selectedGoalId == null) "No goals yet" else "Nothing here yet",
                        subtitle = if (state.selectedGoalId == null) "Start with your Life Vision below."
                        else "Break this down below, or log a Today's Action."
                    )
                }
            }

            items(state.visibleGoals) { goal ->
                AetherCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.selectGoal(goal.id) }
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = AetherRose, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            GOAL_TYPE_LABELS[goal.goalType] ?: goal.goalType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherTextSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(goal.domain, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                    Spacer(Modifier.height(10.dp))
                    val progress = (state.progressByGoalId[goal.id] ?: 0)
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = AetherRose,
                        trackColor = AetherBorder
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$progress% toward this", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                }
            }

            state.selectedGoal?.let {
                item { Spacer(Modifier.height(4.dp)) }
                item { SectionHeader("Today's Actions") }

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
                    AetherCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = taskDraft,
                            onValueChange = { taskDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Add a today's action") },
                            colors = aetherTextFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        AetherButton(
                            text = "Add Action",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.addTask(taskDraft)
                                taskDraft = ""
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item {
                SectionHeader(
                    if (state.selectedGoal == null) "Add a Life Vision or Long Term goal" else "Break \"${state.selectedGoal!!.title}\" down further"
                )
            }
            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
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
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("What are you working toward?") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = domainDraft,
                        onValueChange = { domainDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Domain (e.g. GATE, Research, Gym)") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    AetherButton(
                        text = "Add Goal",
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = AetherRose,
                        onClick = {
                            viewModel.addGoal(titleDraft, goalTypeDraft, domainDraft)
                            titleDraft = ""
                            domainDraft = ""
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { SectionHeader("What are you focusing on?", "Selecting an area unlocks its module and lets you generate a timetable.") }
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
                    AetherCard(modifier = Modifier.fillMaxWidth()) {
                        AetherButton(
                            text = if (state.scheduleSlots.isEmpty()) "Generate timetable" else "Regenerate timetable",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.regenerateSchedule() }
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Basic auto-generated split above. Tap any slot to edit it — or describe your " +
                                "actual routine below and let AI build a better one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherTextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = aiDescription,
                            onValueChange = { aiDescription = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. College 9-5 on weekdays, sleep by 11pm, prefer gym in the evening") },
                            colors = aetherTextFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        AetherButton(
                            text = if (state.isGeneratingWithAi) "Generating…" else "Generate with AI",
                            onClick = { viewModel.generateWithAi(aiDescription) },
                            enabled = !state.isGeneratingWithAi,
                            modifier = Modifier.fillMaxWidth()
                        )
                        state.aiErrorMessage?.let { error ->
                            Spacer(Modifier.height(8.dp))
                            Text(error, style = MaterialTheme.typography.bodyMedium, color = AetherCoral)
                        }
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
                                color = AetherTextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(daySlots) { slot ->
                            AetherCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    editingSlot = slot
                                    editText = slot.activityLabel
                                }
                            ) {
                                Text(slot.timeLabel, style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                                Spacer(Modifier.height(4.dp))
                                Text(slot.activityLabel, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = aetherTextFieldColors()
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
private fun AetherScaffoldGoals(onNavigate: (String) -> Unit, content: @Composable () -> Unit) {
    com.aether.android.ui.components.AetherScaffold(title = "Goals", currentRoute = "goals", onNavigate = onNavigate, content = content)
}

@Composable
private fun BreadcrumbChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (selected) AetherSky else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AetherOnAccent else AetherTextSecondary
        )
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit) {
    val accent = if (task.isDone) AetherEmerald else AetherCoral
    AetherCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = accent, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.isDone) AetherTextSecondary else AetherTextPrimary,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
