package com.aether.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary

/** A considered "nothing here yet" state — replaces bare gray placeholder lines. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineMedium)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = AetherTextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AetherTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
