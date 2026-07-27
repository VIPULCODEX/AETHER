package com.aether.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherCyan
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherIndigo
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherRose

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AetherScaffold(title = "Good to see you.", currentRoute = "dashboard", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AccentCard(accentColor = AetherIndigo, modifier = Modifier.fillMaxWidth()) {
                    Text("Life Score", style = MaterialTheme.typography.labelSmall, color = AetherOnAccent)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${state.lifeScore.lifeScore}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AetherOnAccent
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("This week", style = MaterialTheme.typography.labelSmall, color = AetherOnAccent.copy(alpha = 0.7f))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.weekStrip.forEach { done ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (done) AetherOnAccent else AetherOnAccent.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                            )
                        }
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

            item {
                AccentCard(
                    accentColor = AetherRose,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("goals") }
                ) {
                    Text("Goals", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.activeGoals.isEmpty()) "No active goals — tap to add one." else "${state.activeGoals.size} active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherOnAccent
                    )
                }
            }

            item {
                AccentCard(
                    accentColor = AetherCyan,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("journal") }
                ) {
                    Text("Journal", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Reflect on today.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
        }
    }
}
