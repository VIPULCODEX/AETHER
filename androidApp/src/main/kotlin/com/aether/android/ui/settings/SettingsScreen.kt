package com.aether.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherTextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit
) {
    val savedKey by viewModel.apiKey.collectAsState()
    var draft by remember { mutableStateOf(savedKey) }

    LaunchedEffect(savedKey) { draft = savedKey }

    AetherScaffold(title = "Settings", currentRoute = "settings", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Groq API Key", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Text(
                    "Used only for AI-generated schedules in Goals. Get a free key at " +
                        "console.groq.com/keys — it's your own key, stored only on this " +
                        "device, and calls go directly from your phone to Groq. Nothing else " +
                        "in AETHER ever leaves your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherTextSecondary
                )
            }
            item {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("gsk_...") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            item {
                Button(onClick = { viewModel.save(draft) }) {
                    Text("Save")
                }
            }
            if (savedKey.isNotBlank()) {
                item {
                    TextButton(onClick = {
                        viewModel.clear()
                        draft = ""
                    }) {
                        Text("Remove key")
                    }
                }
                item {
                    Text("A Groq key is saved.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                }
            }
        }
    }
}
