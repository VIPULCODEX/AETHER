package com.aether.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBorder
import com.aether.android.ui.theme.AetherSurface1

/**
 * Neutral surface tile — the default for lists and everyday content (goal
 * rows, schedule slots, journal entries, research notes, body-log rows).
 * [AccentCard]'s saturated gradient is reserved for a handful of true hero
 * moments (Life Score, Today's Mission, Nutrition plan); using it for every
 * single row is what made earlier versions read as loud/flat rather than
 * hierarchical.
 */
@Composable
fun AetherCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .then(clickable)
            .background(color = AetherSurface1, shape = RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, AetherBorder), shape = RoundedCornerShape(20.dp))
            .padding(contentPadding),
        content = content
    )
}
