package com.aether.android.ui.personalize

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface2
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.model.Chronotype
import com.aether.core.model.CoachTone
import com.aether.core.model.Modules
import com.aether.core.model.Persona
import com.aether.core.model.PlanningStyle

@Composable
fun PersonalizeScreen(viewModel: PersonalizeViewModel) {
    val state by viewModel.uiState.collectAsState()
    val prefs = state.prefs
    val learned = state.learned

    AetherScaffold(title = "Personalize") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    "What describes you best?",
                    "A starting point only. Picking one sets sensible defaults for modules, tone and planning, which you can change below."
                )
            }
            item {
                Column {
                    ChipRow {
                        Persona.values().forEach { persona ->
                            Chip(persona.label, selected = prefs.persona == persona && prefs.onboarded) { viewModel.setPersona(persona) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(prefs.persona.blurb, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                }
            }

            item { SectionHeader("How should AETHER talk to you?") }
            item {
                ChipRow {
                    CoachTone.values().forEach { tone ->
                        Chip(tone.label, selected = prefs.tone == tone) { viewModel.setTone(tone) }
                    }
                }
            }

            item { SectionHeader("How do you plan?", "Structured favors deadline-driven order; flexible favors quick wins and momentum.") }
            item {
                ChipRow {
                    PlanningStyle.values().forEach { style ->
                        Chip(style.label, selected = prefs.planningStyle == style) { viewModel.setPlanningStyle(style) }
                    }
                }
            }

            item { SectionHeader("When do you do your best work?", "Auto learns it from when you actually finish tasks. Pick one to override what it observes.") }
            item {
                ChipRow {
                    Chip("Auto", selected = prefs.selfChronotype == null) { viewModel.setChronotype(null) }
                    Chronotype.values().forEach { type ->
                        Chip(type.label, selected = prefs.selfChronotype == type) { viewModel.setChronotype(type) }
                    }
                }
            }

            item { SectionHeader("Your day", "Outside these hours AETHER suggests winding down instead of pushing tasks.") }
            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    HourStepper("Wake up", prefs.wakeHour, onChange = viewModel::setWakeHour)
                    Spacer(Modifier.height(12.dp))
                    HourStepper("Wind down", prefs.sleepHour, onChange = viewModel::setSleepHour)
                }
            }

            item { SectionHeader("Modules", "Turn off what you don't use. It disappears from your Home and from suggestions.") }
            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    ModuleSwitch("Gym", Modules.GYM in prefs.enabledModules) { viewModel.toggleModule(Modules.GYM) }
                    Spacer(Modifier.height(10.dp))
                    ModuleSwitch("Research", Modules.RESEARCH in prefs.enabledModules) { viewModel.toggleModule(Modules.RESEARCH) }
                    Spacer(Modifier.height(10.dp))
                    ModuleSwitch("Journal", Modules.JOURNAL in prefs.enabledModules) { viewModel.toggleModule(Modules.JOURNAL) }
                }
            }

            item { SectionHeader("What AETHER has learned", "Computed on this device from your own history. Nothing is uploaded.") }
            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    ModuleSwitch("Learn from my behavior", prefs.learningEnabled) { viewModel.setLearningEnabled(!prefs.learningEnabled) }
                    if (prefs.learningEnabled) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Confidence",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AetherTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(learned.confidence * 100).toInt()}% · ${learned.observations} observations",
                                style = MaterialTheme.typography.labelLarge,
                                color = AetherTextPrimary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { learned.confidence.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = AetherSky,
                            trackColor = AetherSurface2
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "From: ${learned.routineCompletions} routine ticks, ${learned.taskCompletions} finished tasks, ${learned.feedbackEvents} suggestion responses.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherTextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        if (learned.insights.isEmpty()) {
                            Text(
                                if (learned.routineCompletions + learned.taskCompletions < 5)
                                    "Tick your routines on Home or finish tasks. After about 5 completions AETHER can spot your peak hours; log energy and sleep on Home to see how rest affects you."
                                else "Enough to start, but nothing stands out yet. Keep going.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AetherTextSecondary
                            )
                        } else {
                            learned.insights.forEach { insight ->
                                Row(Modifier.padding(vertical = 4.dp)) {
                                    Box(
                                        Modifier
                                            .padding(top = 7.dp)
                                            .size(6.dp)
                                            .background(AetherSky, CircleShape)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(insight, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        AetherOutlinedButton(text = "Forget what you've learned", onClick = viewModel::resetLearning)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        softWrap = false,
        color = if (selected) AetherOnAccent else AetherTextPrimary,
        modifier = Modifier
            .background(if (selected) AetherSky else AetherSurface2, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun ModuleSwitch(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = AetherSky, checkedThumbColor = AetherOnAccent)
        )
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary, modifier = Modifier.weight(1f))
        StepButton("−") { onChange(hour - 1) }
        Text(
            formatClock(hour),
            style = MaterialTheme.typography.labelLarge,
            color = AetherTextPrimary,
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        StepButton("+") { onChange(hour + 1) }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .background(AetherSurface2, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
    }
}

private fun formatClock(hour: Int): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "$h12 ${if (hour < 12) "AM" else "PM"}"
}
