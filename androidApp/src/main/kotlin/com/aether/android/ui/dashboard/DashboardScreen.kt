package com.aether.android.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AccentCard
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherCyan
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherFlame
import com.aether.android.ui.theme.AetherIndigo
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherRose
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface2
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.engine.LifeScoreBreakdown
import java.time.LocalTime

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val greeting = greetingForHour(LocalTime.now().hour)

    AetherScaffold(title = greeting) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LifeScoreCard(
                    lifeScore = state.lifeScore,
                    trend = trendCaption(state.weekOverWeekDelta),
                    weekStrip = state.weekStrip,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.lifeVisionProgress?.let { progress ->
                item {
                    AccentCard(
                        accentColor = AetherIndigo,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate("goals") }
                    ) {
                        Text("Life Vision", style = MaterialTheme.typography.labelSmall, color = AetherOnAccent.copy(alpha = 0.7f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${progress.progressPercent}% closer to: ${progress.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AetherOnAccent
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = AetherOnAccent,
                            trackColor = AetherOnAccent.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            state.suggestion?.let { suggestion ->
                item {
                    val missionColor = if (state.missionDoneToday) AetherEmerald else AetherCoral
                    AccentCard(
                        accentColor = missionColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.toggleMissionDone() }
                    ) {
                        Text(
                            if (state.missionDoneToday) "Today's Mission · Done" else "Today's Mission",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherOnAccent
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(suggestion.title, style = MaterialTheme.typography.titleMedium, color = AetherOnAccent)
                        Spacer(Modifier.height(4.dp))
                        Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium, color = AetherOnAccent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.missionDoneToday) "Tap to undo" else "Tap when you've done it",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherOnAccent
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(2.dp)) }

            item {
                ModuleRow(
                    icon = Icons.Filled.Flag,
                    dotColor = AetherRose,
                    title = "Goals",
                    subtitle = if (state.activeGoals.isEmpty()) "No active goals — tap to add one." else "${state.activeGoals.size} active, rolling up to your Life Vision",
                    onClick = { onNavigate("goals") }
                )
            }

            item {
                ModuleRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    dotColor = AetherCyan,
                    title = "Journal",
                    subtitle = if (state.journalEntriesThisWeek == 0) "Nothing written this week — reflect on today." else "${state.journalEntriesThisWeek} ${if (state.journalEntriesThisWeek == 1) "entry" else "entries"} this week",
                    onClick = { onNavigate("journal") }
                )
            }

            if (state.focusAreas.contains("Gym")) {
                item {
                    val log = state.latestBodyLog
                    ModuleRow(
                        icon = Icons.Filled.FitnessCenter,
                        dotColor = AetherFlame,
                        title = "Gym",
                        subtitle = log?.weightKg?.let { "Last check-in: $it kg" }
                            ?: "No check-ins yet — log today's weight or a photo.",
                        onClick = { onNavigate("gym") }
                    )
                }
            }

            if (state.focusAreas.contains("Research")) {
                item {
                    ModuleRow(
                        icon = Icons.Filled.Science,
                        dotColor = AetherSky,
                        title = "Research",
                        subtitle = if (state.researchNoteCount == 0) "Nothing logged yet." else "${state.researchNoteCount} ${if (state.researchNoteCount == 1) "note" else "notes"} · latest: ${state.latestResearchNote?.title}",
                        onClick = { onNavigate("research") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleRow(
    icon: ImageVector,
    dotColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    AetherCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = dotColor.copy(alpha = 0.18f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = dotColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AetherTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Replaces the old flat "Life Score" number + tinted-dot week strip with a
 * progress ring for the overall score plus a breakdown of the three real
 * metrics that average into it (ScoringEngine.compute). Consistency/
 * Execution/Goal-completion are independent 0-100% figures, not slices of
 * one whole, so they're shown as their own labeled bars rather than forced
 * into pie-slice arc lengths that would misrepresent them as summing to
 * 100 -- the ring is reserved for the one number that actually is a single
 * percentage: the overall Life Score.
 */
@Composable
private fun LifeScoreCard(
    lifeScore: LifeScoreBreakdown,
    trend: String,
    weekStrip: List<Boolean>,
    modifier: Modifier = Modifier
) {
    AetherCard(modifier = modifier) {
        Text("Life Score", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LifeScoreRing(percent = lifeScore.lifeScore, modifier = Modifier.size(92.dp))
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreBreakdownRow("Consistency", lifeScore.consistencyScore, AetherSky)
                ScoreBreakdownRow("Execution", lifeScore.executionScore, AetherCoral)
                ScoreBreakdownRow("Goal completion", lifeScore.goalCompletionPercent, AetherEmerald)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(trend, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            weekStrip.forEachIndexed { index, done ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (done) AetherIndigo else AetherTextSecondary.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        DAY_LETTERS[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherTextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LifeScoreRing(percent: Int, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = percent.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "lifeScoreRing"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.14f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
                color = AetherSurface2,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = AetherIndigo,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text("$percent", style = MaterialTheme.typography.headlineSmall, color = AetherTextPrimary)
    }
}

@Composable
private fun ScoreBreakdownRow(label: String, percent: Int, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = AetherTextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text("$percent%", style = MaterialTheme.typography.labelLarge, color = AetherTextPrimary)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percent.coerceIn(0, 100) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = AetherSurface2
        )
    }
}

private fun trendCaption(delta: Int): String = when {
    delta > 0 -> "More consistent than last week."
    delta < 0 -> "Quieter than last week — today is a clean start."
    else -> "Steady with last week."
}
