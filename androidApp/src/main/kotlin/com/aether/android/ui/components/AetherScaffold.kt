package com.aether.android.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBackground
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface1
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem("Home", "dashboard", Icons.Outlined.Home, Icons.Filled.Home),
    BottomNavItem("Goals", "goals", Icons.Outlined.Flag, Icons.Filled.Flag),
    BottomNavItem("Gym", "gym", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    BottomNavItem("Journal", "journal", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    BottomNavItem("More", "more", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz)
)

private val MORE_ROUTES = setOf("more", "research", "settings", "coming_soon/gate")

/**
 * Space every screen's own scrollable content should reserve at the top/bottom
 * so real content — not just empty margin — scrolls underneath the floating
 * glass bars, which is what makes the blur/refraction actually read as "glass"
 * over something instead of a tinted rectangle.
 */
object AetherBars {
    val TopContentPadding = 96.dp
    val BottomContentPadding = 108.dp
}

/**
 * Edge-to-edge liquid-glass chrome (see github.com/Kyant0/AndroidLiquidGlass):
 * content renders full-bleed behind a frosted top title bar and a floating
 * capsule bottom nav bar, both of which live-sample and blur/refract the
 * actual content scrolling underneath via a single shared `LayerBackdrop`,
 * replacing the old opaque Material3 Scaffold bars (which reserved their own
 * space and never showed any content behind them) and the emoji tab icons.
 */
@Composable
fun AetherScaffold(
    title: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val backdrop = rememberLayerBackdrop()

    Box(
        Modifier
            .fillMaxSize()
            .background(AetherBackground)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            content()
        }

        GlassTopBar(
            title = title,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        GlassBottomNavBar(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GlassTopBar(
    title: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    vibrancy()
                    blur(14.dp.toPx())
                },
                onDrawSurface = { drawRect(AetherBackground.copy(alpha = 0.55f)) }
            )
    ) {
        Text(
            title,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = AetherTextPrimary
        )
    }
}

@Composable
private fun GlassBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val selectedIndex = remember(currentRoute) {
        BOTTOM_NAV_ITEMS.indexOfFirst { item ->
            if (item.route == "more") currentRoute in MORE_ROUTES else currentRoute == item.route
        }.coerceAtLeast(0)
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        val tabWidth = maxWidth / BOTTOM_NAV_ITEMS.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
            label = "navIndicator"
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(50) },
                    effects = {
                        vibrancy()
                        blur(10.dp.toPx())
                        lens(20.dp.toPx(), 20.dp.toPx())
                    },
                    onDrawSurface = { drawRect(AetherSurface1.copy(alpha = 0.45f)) }
                )
        ) {
            Box(
                Modifier
                    .padding(6.dp)
                    .offset(x = indicatorOffset)
                    .size(width = tabWidth - 12.dp, height = 52.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(50) },
                        effects = {
                            lens(10.dp.toPx(), 14.dp.toPx(), chromaticAberration = true)
                        },
                        onDrawSurface = { drawRect(AetherSky.copy(alpha = 0.28f)) }
                    )
            )

            Row(Modifier.fillMaxSize()) {
                BOTTOM_NAV_ITEMS.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onNavigate(item.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        val tint = if (selected) AetherSky else AetherTextSecondary
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint
                            )
                        }
                    }
                }
            }
        }
    }
}
