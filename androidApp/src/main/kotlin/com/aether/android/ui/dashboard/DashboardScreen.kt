package com.aether.android.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.GlassCard
import com.aether.android.ui.theme.AetherAccent
import com.aether.android.ui.theme.AetherTextSecondary

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenJournal: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Good to see you.",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Life Score", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${state.lifeScore.lifeScore}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AetherAccent
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Consistency ${state.lifeScore.consistencyScore} · " +
                        "Execution ${state.lifeScore.executionScore} · " +
                        "Goals ${state.lifeScore.goalCompletionPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherTextSecondary
                )
            }
        }

        state.suggestion?.let { suggestion ->
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Today's Mission", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text(suggestion.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenJournal() }
            ) {
                Text("Journal", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Reflect on today.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
            }
        }

        if (state.activeGoals.isNotEmpty()) {
            item {
                Text("Active goals", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
            }
            items(state.activeGoals) { goal ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(goal.title, style = MaterialTheme.typography.bodyLarge)
                    Text(goal.domain, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                }
            }
        }
    }
    }
}
