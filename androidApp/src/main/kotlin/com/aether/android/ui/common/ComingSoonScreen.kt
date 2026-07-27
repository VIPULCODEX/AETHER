package com.aether.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.EmptyState

private data class ComingSoonInfo(val title: String, val emoji: String, val subtitle: String)

private val INFO = mapOf(
    "gate" to ComingSoonInfo(
        title = "GATE Prep",
        emoji = "📘",
        subtitle = "Subject-wise syllabus tracking, PYQ practice logs and mock-test trends are planned here next."
    )
)

@Composable
fun ComingSoonScreen(
    slug: String,
    onNavigate: (String) -> Unit
) {
    val info = INFO[slug] ?: ComingSoonInfo("Coming Soon", "🚧", "This module isn't built yet.")

    AetherScaffold(title = info.title, currentRoute = "coming_soon/$slug", onNavigate = onNavigate) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyState(emoji = info.emoji, title = "${info.title} isn't built yet", subtitle = info.subtitle)
        }
    }
}
