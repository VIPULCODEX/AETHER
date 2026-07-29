package com.aether.android.ui.journal

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.EmptyState
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.components.openAttachment
import com.aether.android.ui.components.rememberAttachmentPicker
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextPrimary
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

private val REFLECTION_PROMPTS = listOf(
    "What's true today?",
    "What moved you closer to who you're becoming?",
    "What drained you, and what would you change about it?",
    "What's one thing future-you would thank you for doing today?",
    "Where did you show up even when it was hard?"
)

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigate: (String) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    var draft by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var attachmentUri by remember { mutableStateOf<String?>(null) }
    val pickPhoto = rememberAttachmentPicker(arrayOf("image/*")) { uri -> attachmentUri = uri.toString() }

    val thisMonth = YearMonth.now()
    val entriesThisMonth = entries.count { entry ->
        val entryMonth = YearMonth.from(Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()))
        entryMonth == thisMonth
    }
    val prompt = remember { REFLECTION_PROMPTS[localDateOrdinal() % REFLECTION_PROMPTS.size] }

    AetherScaffold(title = "Journal") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "$entriesThisMonth ${if (entriesThisMonth == 1) "entry" else "entries"} this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherTextSecondary
                )
            }

            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    Text("How are you feeling?", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                    Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(prompt) },
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AetherOutlinedButton(
                            text = if (attachmentUri == null) "Attach a photo" else "Photo attached",
                            onClick = pickPhoto
                        )
                        attachmentUri?.let {
                            Spacer(Modifier.width(10.dp))
                            AsyncImage(
                                model = it,
                                contentDescription = "Attached photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    AetherButton(
                        text = "Save",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.addEntry(draft, selectedMood, attachmentUri)
                            draft = ""
                            selectedMood = null
                            attachmentUri = null
                        }
                    )
                }
            }

            if (entries.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📔",
                        title = "Nothing written yet",
                        subtitle = "Your first entry starts the record of who you're becoming."
                    )
                }
            } else {
                item { SectionHeader("Entries") }
                items(entries) { entry -> JournalEntryRow(entry) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun JournalEntryRow(entry: com.aether.core.model.JournalEntry) {
    val context = LocalContext.current
    AetherCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = entry.attachmentUri?.let { uri -> { openAttachment(context, uri) } }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            entry.attachmentUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Attached photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                        .format(Date(entry.createdAt)) +
                        (MOOD_OPTIONS.find { it.first == entry.mood }?.second?.let { "  ·  $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = AetherTextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Text(entry.content, style = MaterialTheme.typography.bodyLarge, color = AetherTextPrimary)
            }
        }
    }
}

private fun localDateOrdinal(): Int = java.time.LocalDate.now().toEpochDay().toInt()
