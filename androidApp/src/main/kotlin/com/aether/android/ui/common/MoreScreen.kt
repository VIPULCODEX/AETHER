package com.aether.android.ui.common

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherCyan
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherIndigo
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherRose
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSlate

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    AetherScaffold(title = "More") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AccentCard(
                    accentColor = AetherEmerald,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("routines") }
                ) {
                    Text("Routines", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Daily work, commitments, auto-planned timetable.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
            item {
                AccentCard(
                    accentColor = AetherRose,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("lifemap") }
                ) {
                    Text("Life Map", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Vision to goals to tasks, as one map.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
            item {
                AccentCard(
                    accentColor = AetherCyan,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("study") }
                ) {
                    Text("Study Hub", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Sources, cited Q&A, summaries, flashcards, quizzes.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
            item {
                AccentCard(
                    accentColor = AetherIndigo,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("personalize") }
                ) {
                    Text("Personalize", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Persona, tone, modules, and what AETHER has learned.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
            item {
                AccentCard(
                    accentColor = AetherSky,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("research") }
                ) {
                    Text("Research OS", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Papers, notes, ideas.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
            item {
                AccentCard(
                    accentColor = AetherSlate,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("coming_soon/gate") }
                ) {
                    Text("GATE Prep", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Not built yet.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
            item {
                AccentCard(
                    accentColor = AetherSlate,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("settings") }
                ) {
                    Text("Settings", style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                    Spacer(Modifier.height(4.dp))
                    Text("Groq API key.", style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
