package com.aether.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherTextSecondary

private val TITLES = mapOf(
    "gym" to "Gym",
    "research" to "Research OS",
    "gate" to "GATE Prep"
)

@Composable
fun ComingSoonScreen(
    slug: String,
    onNavigate: (String) -> Unit
) {
    val title = TITLES[slug] ?: "Coming Soon"

    AetherScaffold(title = title, currentRoute = "coming_soon/$slug", onNavigate = onNavigate) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$title isn't built yet.",
                style = MaterialTheme.typography.titleMedium,
                color = AetherTextSecondary
            )
        }
    }
}
