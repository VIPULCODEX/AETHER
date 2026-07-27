package com.aether.android.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherCyan
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextSecondary
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private val MOOD_OPTIONS = listOf(
    5 to "😊 Great",
    4 to "🙂 Good",
    3 to "😐 Okay",
    2 to "😕 Low",
    1 to "😔 Rough"
)

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigate: (String) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    var draft by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<Int?>(null) }

    val thisMonth = YearMonth.now()
    val entriesThisMonth = entries.count { entry ->
        val entryMonth = YearMonth.from(Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()))
        entryMonth == thisMonth
    }

    AetherScaffold(title = "Journal", currentRoute = "journal", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "$entriesThisMonth ${if (entriesThisMonth == 1) "entry" else "entries"} this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherTextSecondary
                )
            }
            item {
                Text("How are you feeling?", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MOOD_OPTIONS) { (value, label) ->
                        FilterChip(
                            selected = selectedMood == value,
                            onClick = { selectedMood = if (selectedMood == value) null else value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AetherSky,
                                selectedLabelColor = AetherOnAccent
                            )
                        )
                    }
                }
            }
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
                    viewModel.addEntry(draft, selectedMood)
                    draft = ""
                    selectedMood = null
                }) {
                    Text("Save")
                }
            }
            item { Spacer(Modifier.height(12.dp)) }

            items(entries) { entry ->
                AccentCard(accentColor = AetherCyan, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                            .format(Date(entry.createdAt)) +
                            (MOOD_OPTIONS.find { it.first == entry.mood }?.second?.let { "  ·  $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherOnAccent
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(entry.content, style = MaterialTheme.typography.bodyLarge, color = AetherOnAccent)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
