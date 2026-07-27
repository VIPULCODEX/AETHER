package com.aether.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherHairline
import com.aether.android.ui.theme.AetherSurface2

/**
 * Approximated glass: translucent surface + hairline border. True
 * backdrop blur (Modifier.blur / RenderEffect) needs API 31+ with a
 * fallback below that — left as a follow-up polish pass, not faked here.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = AetherSurface2.copy(alpha = 0.72f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = AetherHairline,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(contentPadding),
        content = content
    )
}
