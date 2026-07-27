package com.aether.android.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.aether.android.ui.theme.AetherMagenta

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onNavigate: (String) -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    var titleDraft by remember { mutableStateOf("") }
    var domainDraft by remember { mutableStateOf("") }

    AetherScaffold(title = "Goals", currentRoute = "goals", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    viewModel.addGoal(titleDraft, domainDraft)
                    titleDraft = ""
                    domainDraft = ""
                }) {
                    Text("Add Goal")
                }
            }

            if (goals.isEmpty()) {
                item {
                    Text(
                        "No goals yet — add the first thing that actually matters this week.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(goals) { goal ->
                BlockCard(accentColor = AetherMagenta, modifier = Modifier.fillMaxWidth()) {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, color = AetherInk)
                    Spacer(Modifier.height(4.dp))
                    Text(goal.domain, style = MaterialTheme.typography.bodyMedium, color = AetherInk)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
