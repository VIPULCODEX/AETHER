package com.aether.android.ui.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.goals.STANDARD_FOCUS_AREAS
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface2
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.model.ALL_DAYS

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel) {
    val routines by viewModel.routines.collectAsState()
    val message by viewModel.message.collectAsState()

    AetherScaffold(title = "Routines") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    "Your daily work",
                    "Add what you do every day, plus fixed commitments like college or office. AETHER fits the rest around them and keeps the timetable and alarms in sync."
                )
            }
            item { AddRoutineCard(viewModel) }

            if (routines.isNotEmpty()) {
                item {
                    Column {
                        AetherButton("Rebuild my timetable", modifier = Modifier.fillMaxWidth(), onClick = viewModel::rebuildTimetable)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Replaces the current weekly timetable (and its alarms) with one planned from these routines.",
                            style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary
                        )
                    }
                }
            }
            message?.let {
                item {
                    AetherCard(modifier = Modifier.fillMaxWidth(), onClick = viewModel::dismissMessage) {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                    }
                }
            }

            items(routines, key = { it.id }) { r ->
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    val days = if (r.daysMask == ALL_DAYS) "Every day" else DAY_LABELS.filterIndexed { i, _ -> r.occursOn(i) }.joinToString(" ")
                    val when_ = r.fixedStartMinute?.let { "fixed ${clock(it)}-${clock(it + r.minutes)}" } ?: "${durationLabel(r.minutes)}, flexible"
                    Text("${r.domain} · $when_ · $days", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Remove", style = MaterialTheme.typography.labelLarge, color = AetherCoral, modifier = Modifier.clickable { viewModel.removeRoutine(r.id) })
                }
            }
        }
    }
}

@Composable
private fun AddRoutineCard(vm: RoutinesViewModel) {
    var title by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("GATE") }
    var minutes by remember { mutableIntStateOf(60) }
    var days by remember { mutableIntStateOf(ALL_DAYS) }
    var fixed by remember { mutableStateOf(false) }
    var startMinute by remember { mutableIntStateOf(9 * 60) }

    AetherCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("e.g. GATE revision, Gym, College") }, colors = aetherTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (STANDARD_FOCUS_AREAS + "General").forEach { area -> Chip(area, domain == area) { domain = area } }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DAY_LABELS.forEachIndexed { i, label -> Chip(label, (days shr i) and 1 == 1) { days = days xor (1 shl i) } }
        }
        Spacer(Modifier.height(12.dp))
        Stepper("Duration", durationLabel(minutes), onMinus = { minutes = (minutes - 15).coerceAtLeast(15) }, onPlus = { minutes = (minutes + 15).coerceAtMost(720) })
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Fixed time", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                Text("On: it happens at exactly this time (class, office). Off: AETHER finds a slot.", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
            }
            Switch(checked = fixed, onCheckedChange = { fixed = it }, colors = SwitchDefaults.colors(checkedTrackColor = AetherSky, checkedThumbColor = AetherOnAccent))
        }
        if (fixed) {
            Spacer(Modifier.height(10.dp))
            Stepper("Starts at", clock(startMinute), onMinus = { startMinute = (startMinute - 30).coerceAtLeast(0) }, onPlus = { startMinute = (startMinute + 30).coerceAtMost(23 * 60 + 30) })
        }
        Spacer(Modifier.height(12.dp))
        AetherButton("Add routine", modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank() && days != 0, onClick = {
            vm.addRoutine(title, domain, minutes, days, if (fixed) startMinute else null)
            title = ""
        })
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false,
        color = if (selected) AetherOnAccent else AetherTextPrimary,
        modifier = Modifier
            .background(if (selected) AetherSky else AetherSurface2, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary, modifier = Modifier.weight(1f))
        StepButton("−", onMinus)
        Text(value, style = MaterialTheme.typography.labelLarge, color = AetherTextPrimary, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.width(84.dp))
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).background(AetherSurface2, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(symbol, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
    }
}

private fun clock(minute: Int): String {
    val h24 = (minute / 60) % 24
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return "$h12:${(minute % 60).toString().padStart(2, '0')} ${if (h24 < 12) "AM" else "PM"}"
}

private fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
