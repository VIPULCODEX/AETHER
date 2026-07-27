package com.aether.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// AETHER is dark-mode only, by design — this is not a light/dark toggle app.
private val AetherColorScheme = darkColorScheme(
    background = AetherBackground,
    surface = AetherSurface1,
    surfaceVariant = AetherSurface2,
    primary = AetherAccent,
    onBackground = AetherTextPrimary,
    onSurface = AetherTextPrimary,
    onPrimary = AetherTextPrimary,
    secondary = AetherTextSecondary
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AetherColorScheme,
        typography = AetherTypography,
        content = content
    )
}
