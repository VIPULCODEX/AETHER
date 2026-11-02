package com.aether.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import com.aether.android.data.ShareInbox
import com.aether.android.navigation.AetherNavHost
import com.aether.android.ui.theme.AetherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AetherApplication
        handleShare(intent)

        setContent {
            AetherTheme {
                AetherNavHost(
                    repository = app.repository,
                    goalsRepository = app.goalsRepository,
                    scoringEngine = app.scoringEngine,
                    contextEngine = app.contextEngine,
                    personalizationRepository = app.personalizationRepository,
                    personalizationEngine = app.personalizationEngine,
                    studyRepository = app.studyRepository,
                    routineRepository = app.routineRepository,
                    studyEngine = app.studyEngine,
                    mindMapEngine = app.mindMapEngine,
                    onDeviceLlm = app.onDeviceLlm,
                    digestEngine = app.digestEngine,
                    examEngine = app.examEngine,
                    apiKeyStore = app.apiKeyStore,
                    groqScheduleClient = app.groqScheduleClient,
                    claudeNotesClient = app.claudeNotesClient,
                    alarmPreferences = app.alarmPreferences,
                    alarmScheduler = app.alarmScheduler
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** A link shared from the browser opens the Study Hub reader. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            ShareInbox.pending.value = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }
}
