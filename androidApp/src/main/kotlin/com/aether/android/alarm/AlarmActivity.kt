package com.aether.android.alarm

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.aether.android.ui.theme.AetherIndigo
import com.aether.android.ui.theme.AetherTextPrimary
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
import com.aether.android.R
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
        val signature = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/${R.raw.aether_alarm}")
        val systemFallback = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = playLooping(signature) ?: playLooping(systemFallback)
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

    /** AETHER's own alarm tune, looped; the system alarm ringtone is only a fallback if it can't be played. */
    private fun playLooping(uri: Uri): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            setDataSource(this@AlarmActivity, uri)
            prepare()
            start()
        }
    }.getOrNull()

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
    // Slow breathing glow behind the label: calm, but clearly alive.
    val pulse = rememberInfiniteTransition(label = "alarmPulse")
    val breathe by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alarmBreathe"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(220.dp)
                .graphicsLayer { scaleX = breathe; scaleY = breathe }
                .background(
                    Brush.radialGradient(listOf(AetherIndigo.copy(alpha = 0.55f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(activityLabel, style = MaterialTheme.typography.headlineMedium, color = AetherTextPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("It's time", style = MaterialTheme.typography.bodyLarge, color = AetherTextSecondary)
        Spacer(Modifier.height(48.dp))
        AetherButton(text = "Dismiss", onClick = onDismiss, accentColor = AetherCoral)
        Spacer(Modifier.height(12.dp))
        AetherOutlinedButton(text = "Snooze 10 min", onClick = onSnooze)
    }
}
