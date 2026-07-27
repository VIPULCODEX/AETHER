package com.aether.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.BlockCard
import com.aether.android.ui.theme.AetherAmber
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherInk
import com.aether.android.ui.theme.AetherMagenta
import com.aether.android.ui.theme.AetherTeal

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
                BlockCard(accentColor = AetherAmber, modifier = Modifier.fillMaxWidth()) {
                    Text("Life Score", style = MaterialTheme.typography.labelSmall, color = AetherInk)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${state.lifeScore.lifeScore}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AetherInk
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Consistency ${state.lifeScore.consistencyScore} · " +
                            "Execution ${state.lifeScore.executionScore} · " +
                            "Goals ${state.lifeScore.goalCompletionPercent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherInk
                    )
                }
            }

            state.suggestion?.let { suggestion ->
                item {
                    val missionColor = if (state.missionDoneToday) AetherTeal else AetherCoral
                    BlockCard(
                        accentColor = missionColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.toggleMissionDone() }
                    ) {
                        Text(
                            if (state.missionDoneToday) "Today's Mission · Done" else "Today's Mission",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherInk
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(suggestion.title, style = MaterialTheme.typography.titleMedium, color = AetherInk)
                        Spacer(Modifier.height(4.dp))
                        Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium, color = AetherInk)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.missionDoneToday) "Tap to undo" else "Tap when you've done it",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherInk
                        )
                    }
                }
            }

            item {
                BlockCard(
                    accentColor = AetherMagenta,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("goals") }
                ) {
                    Text("Goals", style = MaterialTheme.typography.titleMedium, color = AetherInk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.activeGoals.isEmpty()) "No active goals — tap to add one." else "${state.activeGoals.size} active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherInk
                    )
                }
            }

            item {
                BlockCard(
                    accentColor = AetherTeal,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("journal") }
                ) {
                    Text("Journal", style = MaterialTheme.typography.titleMedium, color = AetherInk)
                    Spacer(Modifier.height(4.dp))
                    Text("Reflect on today.", style = MaterialTheme.typography.bodyMedium, color = AetherInk)
                }
            }
        }
    }
}
