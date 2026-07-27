package com.aether.android.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBorder
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface1
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextTertiary

/**
 * Text field colors that actually match the theme — left at Material3
 * defaults these clashed against the near-black backdrop (default purple
 * focus/indicator tones). One shared definition so every form on every
 * screen looks like the same app.
 */
@Composable
fun aetherTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AetherTextPrimary,
    unfocusedTextColor = AetherTextPrimary,
    focusedContainerColor = AetherSurface1,
    unfocusedContainerColor = AetherSurface1,
    focusedBorderColor = AetherSky,
    unfocusedBorderColor = AetherBorder,
    focusedPlaceholderColor = AetherTextTertiary,
    unfocusedPlaceholderColor = AetherTextTertiary,
    cursorColor = AetherSky
)

/** Primary action button — solid accent fill, used consistently instead of Material's default purple. */
@Composable
fun AetherButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = AetherSky,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = AetherOnAccent,
            disabledContainerColor = accentColor.copy(alpha = 0.35f),
            disabledContentColor = AetherOnAccent.copy(alpha = 0.6f)
        ),
        content = { content() }
    )
}

@Composable
fun AetherButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = AetherSky
) {
    AetherButton(onClick = onClick, modifier = modifier, enabled = enabled, accentColor = accentColor) {
        Text(text)
    }
}

/** Secondary/tertiary action — outline only, for "cancel"/"remove" style actions. */
@Composable
fun AetherOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AetherTextPrimary)
    ) {
        Text(text)
    }
}
