package com.aether.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherOutline

/**
 * A bold, flat, solid-color block with a thick outline — the retro-blocky
 * replacement for the old translucent glass card. Solid color reads as
 * "tappable" far more clearly than a faint glass surface did, so every
 * card that has real behavior takes an [onClick]; ones passed null render
 * as plain (non-interactive) info blocks with no ripple.
 */
@Composable
fun BlockCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Column(
        modifier = modifier
            .then(clickable)
            .background(color = accentColor, shape = RoundedCornerShape(18.dp))
            .border(width = 3.dp, color = AetherOutline, shape = RoundedCornerShape(18.dp))
            .padding(contentPadding),
        content = content
    )
}
