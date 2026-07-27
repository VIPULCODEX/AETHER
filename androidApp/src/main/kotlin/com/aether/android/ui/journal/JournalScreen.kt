package com.aether.android.ui.journal

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
import com.aether.android.ui.theme.AetherTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigate: (String) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    var draft by remember { mutableStateOf("") }

    AetherScaffold(title = "Journal", currentRoute = "journal", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What's true today?") }
                )
            }
            item {
                Button(onClick = {
                    viewModel.addEntry(draft)
                    draft = ""
                }) {
                    Text("Save")
                }
            }
            item { Spacer(Modifier.height(12.dp)) }

            items(entries) { entry ->
                BlockCard(accentColor = AetherTeal, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                            .format(Date(entry.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherInk
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(entry.content, style = MaterialTheme.typography.bodyLarge, color = AetherInk)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
