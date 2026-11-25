package com.aether.android.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.data.MyPlan
import com.aether.core.engine.ImportedBlock
import com.aether.core.engine.TimetableImporter

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Paste, check what was understood, then apply. Nothing is saved before the last button. */
@Composable
fun ImportTimetableScreen(viewModel: RoutinesViewModel, onDone: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val result = remember(text) { TimetableImporter.parse(text) }
    var table by remember(text) { mutableIntStateOf(0) }
    var week by remember(text) { mutableIntStateOf(0) }
    var excluded by remember(text, table) { mutableStateOf(emptySet<Int>()) }
    var keepRules by remember(text) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    val chosen = result.timetables.getOrNull(table)
    val blocks: List<ImportedBlock> = chosen?.blocks?.filterIndexed { i, _ -> i !in excluded }.orEmpty()
    val focus = result.weeks.getOrNull(week)?.byDay.orEmpty()
    val rules = if (keepRules) result.rules else emptyList()

    AetherScaffold(title = "My timetable") {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    "Bring your own plan",
                    "Paste the timetable you already made: time and activity on each line, or a table. It is read on this phone only, and your times are kept as written."
                )
            }
            item {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
                    minLines = 5, maxLines = 10, colors = aetherTextFieldColors(),
                    placeholder = { Text("7:30 AM Wake up\n9:00-10:45 NET Paper 2\n5:00-6:30 PM Badminton\n11:30 PM-7:30 AM Sleep") }
                )
            }
            if (text.isNotBlank() && result.isEmpty) {
                item { Text("No timetable found yet. Each line needs a time (like 9:00-10:45) or a table with durations.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary) }
            }

            if (result.timetables.size > 1) {
                item {
                    AetherCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Which one do you follow now?", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                        result.timetables.forEachIndexed { i, t ->
                            Row(Modifier.fillMaxWidth().clickable { table = i }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = table == i, onClick = { table = i })
                                Text("${t.title} (${t.blocks.size} blocks)", style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                            }
                        }
                    }
                }
            }

            chosen?.let { t ->
                item {
                    AetherCard(modifier = Modifier.fillMaxWidth()) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                        Text("Untick anything you don't want. Breaks and meals are shown on the timetable without alarms.", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                        Spacer(Modifier.height(6.dp))
                        t.blocks.forEachIndexed { i, b ->
                            val toggle = { excluded = if (i in excluded) excluded - i else excluded + i }
                            Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = i !in excluded, onCheckedChange = { toggle() })
                                Column(Modifier.weight(1f)) {
                                    Text(b.title, style = MaterialTheme.typography.bodyLarge, color = AetherTextPrimary)
                                    val time = b.startMinute?.let { "${clock(it)} - ${clock(it + b.minutes)}" } ?: "${b.minutes} min, AETHER picks the time"
                                    Text("$time · ${b.domain}", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                                }
                            }
                        }
                        if (t.wakeMinute != null || t.sleepMinute != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                listOfNotNull(t.wakeMinute?.let { "Wake ${clock(it)}" }, t.sleepMinute?.let { "Sleep ${clock(it)}" }).joinToString(" · "),
                                style = MaterialTheme.typography.labelLarge, color = AetherTextSecondary
                            )
                        }
                    }
                }
            }

            if (result.weeks.isNotEmpty()) {
                item {
                    AetherCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Weekly focus", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                        if (result.weeks.size > 1) {
                            result.weeks.forEachIndexed { i, w ->
                                Row(Modifier.fillMaxWidth().clickable { week = i }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = week == i, onClick = { week = i })
                                    Text(w.title, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                                }
                            }
                        }
                        focus.toSortedMap().forEach { (day, what) ->
                            Text("${WEEKDAYS[day]}: $what", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                        }
                    }
                }
            }

            if (result.rules.isNotEmpty()) {
                item {
                    AetherCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().clickable { keepRules = !keepRules }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = keepRules, onCheckedChange = { keepRules = it })
                            Text("Show my rules on the home screen", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                        }
                        result.rules.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary) }
                    }
                }
            }

            if (blocks.isNotEmpty() || focus.isNotEmpty() || rules.isNotEmpty()) {
                item {
                    Column {
                        AetherButton("Use my timetable", modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = {
                            saving = true
                            viewModel.importTimetable(blocks, chosen?.wakeMinute, chosen?.sleepMinute, MyPlan(focus, rules), onDone)
                        })
                        if (blocks.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("This replaces your current routines and timetable. Past ticks and streaks are kept.", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

private fun clock(minute: Int): String {
    val m = minute % 1440
    val h = m / 60
    return "${(h + 11) % 12 + 1}:${(m % 60).toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}
