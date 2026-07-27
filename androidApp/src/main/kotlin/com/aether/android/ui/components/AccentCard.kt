package com.aether.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Solid-color rounded tile with a subtle top-to-bottom gradient for depth —
 * the iOS/ColorOS/OxygenOS-style category card, replacing the old flat
 * "poster block with thick border" look. No border; depth comes from the
 * gradient instead, since a literal drop shadow barely reads on a near-black
 * background. Content is white (AetherOnAccent) since fills are deep/rich.
 */
@Composable
fun AccentCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val gradient = Brush.verticalGradient(
        colors = listOf(accentColor, accentColor.copy(alpha = 0.82f))
    )

    Column(
        modifier = modifier
            .then(clickable)
            .background(brush = gradient, shape = RoundedCornerShape(26.dp))
            .padding(contentPadding),
        content = content
    )
}
