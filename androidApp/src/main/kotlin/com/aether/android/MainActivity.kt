package com.aether.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aether.android.navigation.AetherNavHost
import com.aether.android.ui.theme.AetherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AetherApplication

        setContent {
            AetherTheme {
                AetherNavHost(
                    repository = app.repository,
                    goalsRepository = app.goalsRepository,
                    scoringEngine = app.scoringEngine,
                    contextEngine = app.contextEngine,
                    apiKeyStore = app.apiKeyStore,
                    groqScheduleClient = app.groqScheduleClient
                )
            }
        }
    }
}
