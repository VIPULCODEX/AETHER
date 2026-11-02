package com.aether.android.ui.study

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface2
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.engine.SubtitleParser
import com.aether.core.model.StudySource
import kotlinx.coroutines.delay

/**
 * Plays the user's own video file and follows along in its transcript: the line being spoken is highlighted and
 * tapping any line jumps the video there. Everything is local; nothing is streamed or uploaded.
 */
@Composable
fun VideoPlayerCard(source: StudySource, onClose: () -> Unit) {
    val context = LocalContext.current
    val cues = remember(source.transcript) { SubtitleParser.decode(source.transcript) }
    val exo = remember(source.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(source.mediaUri)))
            prepare()
            playWhenReady = false
        }
    }
    var error by remember { mutableStateOf<String?>(null) }
    var position by remember { mutableLongStateOf(0L) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                error = "Couldn't play this video (${e.errorCodeName}). The file may have moved, or the format isn't supported."
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }
    LaunchedEffect(exo) {
        while (true) {
            position = exo.currentPosition
            delay(400)
        }
    }

    val current = cues.indexOfLast { it.startMs <= position }.coerceAtLeast(0)

    AetherCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(source.title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            AetherOutlinedButton("Close", onClick = onClose)
        }
        Spacer(Modifier.height(10.dp))
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exo; useController = true } },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AetherCoral)
        }
        if (cues.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Transcript. Tap a line to jump there.", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val from = (current - 2).coerceAtLeast(0)
                for (i in from until minOf(cues.size, from + 6)) {
                    val cue = cues[i]
                    Text(
                        "${clock(cue.startMs)}  ${cue.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (i == current) AetherTextPrimary else AetherTextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (i == current) AetherSurface2 else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { exo.seekTo(cue.startMs); exo.play() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun clock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
