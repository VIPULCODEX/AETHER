package com.aether.android.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.EmptyState
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.components.rememberAttachmentPicker
import com.aether.android.ui.theme.AetherFlame
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.engine.ActivityLevel
import com.aether.core.engine.BodyGoal
import com.aether.core.model.BodyLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GymScreen(
    viewModel: GymViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AetherScaffold(title = "Gym") {
        if (!state.gymIsFocusArea) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EmptyState(
                        emoji = "💪",
                        title = "Gym isn't active yet",
                        subtitle = "Select \"Gym\" as a focus area in Goals to unlock training, nutrition and progress tracking here."
                    )
                }
                item {
                    AetherButton(text = "Go to Goals", onClick = { onNavigate("goals") }, modifier = Modifier.fillMaxWidth())
                }
            }
            return@AetherScaffold
        }

        var heightText by remember { mutableStateOf(state.profile?.heightCm?.toString() ?: "") }
        var weightText by remember { mutableStateOf(state.profile?.weightKg?.toString() ?: "") }
        var ageText by remember { mutableStateOf(state.profile?.age?.toString() ?: "") }
        var isMale by remember { mutableStateOf(state.profile?.isMale ?: true) }

        var checkInWeight by remember { mutableStateOf("") }
        var checkInNote by remember { mutableStateOf("") }
        var checkInPhotoUri by remember { mutableStateOf<String?>(null) }
        val pickPhoto = rememberAttachmentPicker(arrayOf("image/*")) { uri -> checkInPhotoUri = uri.toString() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionHeader("Your profile", "Used only to calculate your targets below.") }

            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Height (cm)") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Weight (kg)") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Age") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(true, false)) { male ->
                            FilterChip(
                                selected = isMale == male,
                                onClick = { isMale = male },
                                label = { Text(if (male) "Male" else "Female") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AetherSky,
                                    selectedLabelColor = AetherOnAccent
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Activity level", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ActivityLevel.values().toList()) { level ->
                            FilterChip(
                                selected = state.activityLevel == level,
                                onClick = { viewModel.updateActivityLevel(level) },
                                label = { Text(level.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AetherSky,
                                    selectedLabelColor = AetherOnAccent
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Goal", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BodyGoal.values().toList()) { goal ->
                            FilterChip(
                                selected = state.bodyGoal == goal,
                                onClick = { viewModel.updateBodyGoal(goal) },
                                label = { Text(goal.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AetherSky,
                                    selectedLabelColor = AetherOnAccent
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    AetherButton(
                        text = "Calculate",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val height = heightText.toDoubleOrNull()
                            val weight = weightText.toDoubleOrNull()
                            val age = ageText.toIntOrNull()
                            if (height != null && weight != null && age != null) {
                                viewModel.saveProfile(height, weight, age, isMale)
                            }
                        }
                    )
                }
            }

            state.plan?.let { plan ->
                item {
                    AccentCard(accentColor = AetherFlame, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            NutritionStat("Maintenance", "${plan.tdee} kcal", Modifier.weight(1f))
                            NutritionStat("Target intake", "${plan.targetCalories} kcal", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            NutritionStat("Protein", "${plan.proteinGrams} g/day", Modifier.weight(1f))
                            NutritionStat("BMR", "${plan.bmr} kcal", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "General fitness guidance from the Mifflin-St Jeor formula — not medical advice.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherOnAccent.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            item { SectionHeader("Progress", "Log weight and photos to see your trend over time.") }

            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = checkInWeight,
                        onValueChange = { checkInWeight = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Today's weight (kg) — optional") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = checkInNote,
                        onValueChange = { checkInNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Note — optional") },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AetherOutlinedButton(
                            text = if (checkInPhotoUri == null) "Add progress photo" else "Photo attached",
                            onClick = pickPhoto
                        )
                        checkInPhotoUri?.let {
                            Spacer(Modifier.width(10.dp))
                            AsyncImage(
                                model = it,
                                contentDescription = "Selected progress photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    AetherButton(
                        text = "Log check-in",
                        accentColor = AetherFlame,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.addCheckIn(checkInWeight.toDoubleOrNull(), checkInPhotoUri, checkInNote)
                            checkInWeight = ""
                            checkInNote = ""
                            checkInPhotoUri = null
                        }
                    )
                }
            }

            state.weightDeltaKg?.let { delta ->
                item {
                    val direction = if (delta <= 0) "Down" else "Up"
                    Text(
                        "$direction ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(delta))} kg since your last check-in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherTextSecondary
                    )
                }
            }

            if (state.bodyLogs.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📈",
                        title = "No check-ins yet",
                        subtitle = "Your first one starts the trend line."
                    )
                }
            } else {
                items(state.bodyLogs) { log -> BodyLogRow(log) }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("4-day split", "Push / Pull / Legs / Full Body — sets × reps and posture cues below.") }

            items(FOUR_DAY_SPLIT) { day ->
                Column {
                    AccentCard(accentColor = AetherFlame, modifier = Modifier.fillMaxWidth()) {
                        Text(day.title, style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                        Spacer(Modifier.height(2.dp))
                        Text(day.subtitle, style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent.copy(alpha = 0.85f))
                    }
                    Spacer(Modifier.height(8.dp))
                    day.exercises.forEach { exercise ->
                        AetherCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(exercise.name, style = MaterialTheme.typography.bodyLarge, color = AetherTextPrimary, modifier = Modifier.weight(1f))
                                Text(exercise.setsReps, style = MaterialTheme.typography.titleMedium, color = AetherFlame)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(exercise.postureCue, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NutritionStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AetherOnAccent.copy(alpha = 0.75f))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
    }
}

@Composable
private fun BodyLogRow(log: BodyLog) {
    val context = LocalContext.current
    AetherCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = log.photoUri?.let { uri -> { com.aether.android.ui.components.openAttachment(context, uri) } }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            log.photoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Progress photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(log.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = AetherTextSecondary
                )
                Text(
                    log.weightKg?.let { "$it kg" } ?: "No weight logged",
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherTextPrimary
                )
                log.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                }
            }
        }
    }
}
