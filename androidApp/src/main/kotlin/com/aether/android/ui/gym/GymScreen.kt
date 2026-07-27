package com.aether.android.ui.gym

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
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.BlockCard
import com.aether.android.ui.theme.AetherInk
import com.aether.android.ui.theme.AetherLime
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.engine.ActivityLevel
import com.aether.core.engine.BodyGoal

@Composable
fun GymScreen(
    viewModel: GymViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AetherScaffold(title = "Gym", currentRoute = "gym", onNavigate = onNavigate) {
        if (!state.gymIsFocusArea) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Select \"Gym\" as a focus area in Goals to unlock training and nutrition here.",
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

        var heightText by remember { mutableStateOf(state.profile?.heightCm?.toString() ?: "") }
        var weightText by remember { mutableStateOf(state.profile?.weightKg?.toString() ?: "") }
        var ageText by remember { mutableStateOf(state.profile?.age?.toString() ?: "") }
        var isMale by remember { mutableStateOf(state.profile?.isMale ?: true) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Your profile", style = MaterialTheme.typography.titleMedium) }

            item {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Height (cm)") }
                )
            }
            item {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Weight (kg)") }
                )
            }
            item {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Age") }
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(true, false)) { male ->
                        FilterChip(
                            selected = isMale == male,
                            onClick = { isMale = male },
                            label = { Text(if (male) "Male" else "Female") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AetherSky,
                                selectedLabelColor = AetherInk
                            )
                        )
                    }
                }
            }

            item { Text("Activity level", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ActivityLevel.values().toList()) { level ->
                        FilterChip(
                            selected = state.activityLevel == level,
                            onClick = { viewModel.updateActivityLevel(level) },
                            label = { Text(level.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AetherSky,
                                selectedLabelColor = AetherInk
                            )
                        )
                    }
                }
            }

            item { Text("Goal", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BodyGoal.values().toList()) { goal ->
                        FilterChip(
                            selected = state.bodyGoal == goal,
                            onClick = { viewModel.updateBodyGoal(goal) },
                            label = { Text(goal.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AetherSky,
                                selectedLabelColor = AetherInk
                            )
                        )
                    }
                }
            }

            item {
                Button(onClick = {
                    val height = heightText.toDoubleOrNull()
                    val weight = weightText.toDoubleOrNull()
                    val age = ageText.toIntOrNull()
                    if (height != null && weight != null && age != null) {
                        viewModel.saveProfile(height, weight, age, isMale)
                    }
                }) {
                    Text("Calculate")
                }
            }

            state.plan?.let { plan ->
                item {
                    BlockCard(accentColor = AetherLime, modifier = Modifier.fillMaxWidth()) {
                        Text("Maintenance (TDEE)", style = MaterialTheme.typography.labelSmall, color = AetherInk)
                        Text("${plan.tdee} kcal/day", style = MaterialTheme.typography.titleMedium, color = AetherInk)
                        Spacer(Modifier.height(8.dp))
                        Text("Target intake", style = MaterialTheme.typography.labelSmall, color = AetherInk)
                        Text("${plan.targetCalories} kcal/day", style = MaterialTheme.typography.titleMedium, color = AetherInk)
                        Spacer(Modifier.height(8.dp))
                        Text("Protein target", style = MaterialTheme.typography.labelSmall, color = AetherInk)
                        Text("${plan.proteinGrams} g/day", style = MaterialTheme.typography.titleMedium, color = AetherInk)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "BMR: ${plan.bmr} kcal · general fitness guidance, not medical advice.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherInk
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { Text("4-day split", style = MaterialTheme.typography.titleMedium) }
            item {
                Text(
                    "Text-based form cues for now — reference images/videos aren't wired up yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherTextSecondary
                )
            }

            items(FOUR_DAY_SPLIT) { day ->
                BlockCard(accentColor = AetherLime, modifier = Modifier.fillMaxWidth()) {
                    Text(day.title, style = MaterialTheme.typography.titleMedium, color = AetherInk)
                    Spacer(Modifier.height(8.dp))
                    day.exercises.forEach { exercise ->
                        Text(exercise.name, style = MaterialTheme.typography.bodyLarge, color = AetherInk)
                        Text(exercise.postureCue, style = MaterialTheme.typography.bodyMedium, color = AetherInk)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
