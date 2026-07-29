package com.aether.android.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit
) {
    val savedKey by viewModel.apiKey.collectAsState()
    val alarmsEnabled by viewModel.alarmsEnabled.collectAsState()
    var draft by remember { mutableStateOf(savedKey) }
    val context = LocalContext.current

    LaunchedEffect(savedKey) { draft = savedKey }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result observed indirectly via canScheduleExactAlarms()/system state on next recomposition. */ }

    AetherScaffold(title = "Settings") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionHeader("Wake-up alarms", "Off by default. Rings and vibrates at each timetable slot, like the Clock app.") }
            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable alarms", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                            Text(
                                "Needs the \"Alarms & reminders\" system permission.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AetherTextSecondary
                            )
                        }
                        Switch(
                            checked = alarmsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                        != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    if (!viewModel.canScheduleExactAlarms()) {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                        }
                                    }
                                }
                                viewModel.setAlarmsEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = AetherSky, checkedThumbColor = AetherOnAccent)
                        )
                    }
                    if (alarmsEnabled && !viewModel.canScheduleExactAlarms()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Alarms & reminders permission is still off, so alarms won't fire yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherCoral
                        )
                        Spacer(Modifier.height(8.dp))
                        AetherOutlinedButton(
                            text = "Open system settings",
                            onClick = {
                                runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
                            }
                        )
                    }
                }
            }

            item { SectionHeader("Groq API key", "Used only for AI-generated schedules in Goals.") }
            item {
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Get a free key at console.groq.com/keys — it's your own key, stored only on " +
                            "this device, and calls go directly from your phone to Groq. Nothing else " +
                            "in AETHER ever leaves your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gsk_...") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = aetherTextFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    AetherButton(text = "Save", modifier = Modifier.fillMaxWidth(), onClick = { viewModel.save(draft) })
                    if (savedKey.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        AetherOutlinedButton(
                            text = "Remove key",
                            onClick = {
                                viewModel.clear()
                                draft = ""
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("A Groq key is saved.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
