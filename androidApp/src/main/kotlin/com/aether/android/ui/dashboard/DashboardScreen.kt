package com.aether.android.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherCyan
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherFlame
import com.aether.android.ui.theme.AetherIndigo
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherRose
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import java.time.LocalTime

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val greeting = greetingForHour(LocalTime.now().hour)

    AetherScaffold(title = greeting, currentRoute = "dashboard", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AccentCard(accentColor = AetherIndigo, modifier = Modifier.fillMaxWidth()) {
                    Text("Life Score", style = MaterialTheme.typography.labelSmall, color = AetherOnAccent.copy(alpha = 0.8f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${state.lifeScore.lifeScore}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AetherOnAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(trendCaption(state.weekOverWeekDelta), style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent.copy(alpha = 0.85f))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.weekStrip.forEachIndexed { index, done ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = if (done) AetherOnAccent else AetherOnAccent.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    DAY_LETTERS[index],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AetherOnAccent.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            state.lifeVisionProgress?.let { progress ->
                item {
                    AccentCard(
                        accentColor = AetherIndigo,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate("goals") }
                    ) {
                        Text("Life Vision", style = MaterialTheme.typography.labelSmall, color = AetherOnAccent.copy(alpha = 0.7f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${progress.progressPercent}% closer to: ${progress.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AetherOnAccent
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = AetherOnAccent,
                            trackColor = AetherOnAccent.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            state.suggestion?.let { suggestion ->
                item {
                    val missionColor = if (state.missionDoneToday) AetherEmerald else AetherCoral
                    AccentCard(
                        accentColor = missionColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.toggleMissionDone() }
                    ) {
                        Text(
                            if (state.missionDoneToday) "Today's Mission · Done" else "Today's Mission",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherOnAccent
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(suggestion.title, style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                        Spacer(Modifier.height(4.dp))
                        Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.missionDoneToday) "Tap to undo" else "Tap when you've done it",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherOnAccent
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(2.dp)) }

            item {
                ModuleRow(
                    emoji = "🎯",
                    dotColor = AetherRose,
                    title = "Goals",
                    subtitle = if (state.activeGoals.isEmpty()) "No active goals — tap to add one." else "${state.activeGoals.size} active, rolling up to your Life Vision",
                    onClick = { onNavigate("goals") }
                )
            }

            item {
                ModuleRow(
                    emoji = "📔",
                    dotColor = AetherCyan,
                    title = "Journal",
                    subtitle = if (state.journalEntriesThisWeek == 0) "Nothing written this week — reflect on today." else "${state.journalEntriesThisWeek} ${if (state.journalEntriesThisWeek == 1) "entry" else "entries"} this week",
                    onClick = { onNavigate("journal") }
                )
            }

            if (state.focusAreas.contains("Gym")) {
                item {
                    val log = state.latestBodyLog
                    ModuleRow(
                        emoji = "💪",
                        dotColor = AetherFlame,
                        title = "Gym",
                        subtitle = log?.weightKg?.let { "Last check-in: $it kg" }
                            ?: "No check-ins yet — log today's weight or a photo.",
                        onClick = { onNavigate("gym") }
                    )
                }
            }

            if (state.focusAreas.contains("Research")) {
                item {
                    ModuleRow(
                        emoji = "🔬",
                        dotColor = AetherSky,
                        title = "Research",
                        subtitle = if (state.researchNoteCount == 0) "Nothing logged yet." else "${state.researchNoteCount} ${if (state.researchNoteCount == 1) "note" else "notes"} · latest: ${state.latestResearchNote?.title}",
                        onClick = { onNavigate("research") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleRow(
    emoji: String,
    dotColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    AetherCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = dotColor.copy(alpha = 0.18f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Text("›", style = MaterialTheme.typography.titleMedium, color = AetherTextSecondary)
        }
    }
}

private fun trendCaption(delta: Int): String = when {
    delta > 0 -> "More consistent than last week."
    delta < 0 -> "Quieter than last week — today is a clean start."
    else -> "Steady with last week."
}
