package com.aether.android.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.GlassCard
import com.aether.android.ui.theme.AetherBackground
import com.aether.android.ui.theme.AetherTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBackground)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "← Back",
                color = AetherTextSecondary,
                modifier = Modifier.clickable { onBack() }
            )
        }
        Spacer(Modifier.height(16.dp))

        Text("Journal", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What's true today?") }
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            viewModel.addEntry(draft)
            draft = ""
        }) {
            Text("Save")
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entries) { entry ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                            .format(Date(entry.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherTextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(entry.content, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
