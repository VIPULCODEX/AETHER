package com.aether.android.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.android.ui.theme.AetherTheme

/**
 * Shown over the lock screen when a scheduled slot fires — rings and
 * vibrates until dismissed, like the stock Clock app's alarm screen.
 * `minSdk` is 26, below the API 27 `setShowWhenLocked`/`setTurnScreenOn`
 * methods, so the equivalent window flags are used for broad compatibility.
 */
class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val slotId = intent.getStringExtra(EXTRA_SLOT_ID) ?: ""
        val activityLabel = intent.getStringExtra(EXTRA_ACTIVITY_LABEL) ?: "Scheduled block"

        startRinging()

        setContent {
            AetherTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AlarmScreen(
                        activityLabel = activityLabel,
                        onDismiss = { stopRingingAndFinish(slotId) },
                        onSnooze = {
                            AlarmScheduler(this).scheduleSnooze(slotId, activityLabel)
                            stopRingingAndFinish(slotId)
                        }
                    )
                }
            }
        }
    }

    private fun startRinging() {
        val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            runCatching {
                setDataSource(this@AlarmActivity, ringtoneUri)
                prepare()
                start()
            }
        }

        val pattern = longArrayOf(0, 800, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopRingingAndFinish(slotId: String) {
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(90_000 + slotId.hashCode())

        finish()
    }

    override fun onDestroy() {
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        vibrator?.cancel()
        super.onDestroy()
    }
}

@Composable
private fun AlarmScreen(
    activityLabel: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Time for", style = MaterialTheme.typography.bodyLarge, color = AetherTextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(activityLabel, style = MaterialTheme.typography.headlineLarge, color = AetherCoral)
        Spacer(Modifier.height(48.dp))
        AetherButton(text = "Dismiss", onClick = onDismiss, accentColor = AetherCoral)
        Spacer(Modifier.height(12.dp))
        AetherOutlinedButton(text = "Snooze 10 min", onClick = onSnooze)
    }
}
