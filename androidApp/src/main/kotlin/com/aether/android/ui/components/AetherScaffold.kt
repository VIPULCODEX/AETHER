package com.aether.android.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBackground
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface1
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
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

private val LocalTopBarHeight = compositionLocalOf { 96.dp }
private val LocalBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Space every screen's own scrollable content should reserve at the top/bottom
 * so real content — not just empty margin — scrolls underneath the floating
 * glass bars, which is what makes the blur/refraction actually read as "glass"
 * over something instead of a tinted rectangle.
 */
object AetherBars {
    // Backed by the top bar's actual measured height (status bar inset + text
    // vary by device/font scale, so a fixed guess drifts and lets content peek
    // out from behind the bar) rather than a fixed constant.
    val TopContentPadding: Dp
        @Composable get() = LocalTopBarHeight.current
    val BottomContentPadding = 108.dp
}

/**
 * Edge-to-edge liquid-glass chrome (see github.com/Kyant0/AndroidLiquidGlass),
 * wrapping the NavHost exactly once. Owns the shared `LayerBackdrop` and the
 * floating bottom nav bar so both survive screen navigation — every
 * `composable(route) { ... }` destination used to instantiate its own
 * AetherScaffold, which tore down and rebuilt the nav bar's remembered
 * selected-tab state (and thus its slide animation) on every tab switch,
 * since there was never a previous frame in the same composable instance to
 * animate from.
 */
@Composable
fun AetherAppChrome(
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
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            content()
        }

        GlassBottomNavBar(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Per-screen frosted title bar over full-bleed content. Must be called from
 * within [AetherAppChrome] — reads the shared backdrop it provides so the
 * title bar's blur/refraction samples the same layer as the persistent
 * bottom nav bar, rather than each screen recording its own.
 */
@Composable
fun AetherScaffold(
    title: String,
    content: @Composable () -> Unit
) {
    val backdrop = requireNotNull(LocalBackdrop.current) {
        "AetherScaffold must be called inside AetherAppChrome"
    }
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(96.dp) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            CompositionLocalProvider(LocalTopBarHeight provides topBarHeight) {
                content()
            }
        }

        GlassTopBar(
            title = title,
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { size -> topBarHeight = with(density) { size.height.toDp() } }
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

    // Non-null only while a finger is down on the bar. dragIndex is the
    // nearest tab (drives icon tint + which tab a release commits to);
    // dragOffsetPx is the pill's *continuous* raw finger-relative position,
    // so it glides pixel-by-pixel under the touch instead of hopping
    // between quantized per-tab stops while being dragged.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableStateOf<Float?>(null) }
    val displayedIndex = dragIndex ?: selectedIndex
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        val tabWidth = maxWidth / BOTTOM_NAV_ITEMS.size
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val isHeld = dragIndex != null
        val settledTargetPx = tabWidthPx * displayedIndex
        val indicatorOffset by animateDpAsState(
            targetValue = with(density) { (dragOffsetPx ?: settledTargetPx).toDp() },
            // Follow the finger instantly, pixel-for-pixel, while dragging;
            // only ease into place for a tap/release-triggered tab change.
            animationSpec = if (dragOffsetPx != null) snap() else tween(durationMillis = 420, easing = FastOutSlowInEasing),
            label = "navIndicator"
        )
        // Pill pops slightly larger the instant it's grabbed (quick spring),
        // then eases back to its resting size on release. Scales about its
        // own center (graphicsLayer's default transform origin) rather than
        // translating vertically, so it stays hovering centered in the bar
        // instead of growing into its top/bottom edge.
        val liftSpec: AnimationSpec<Float> = if (isHeld) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            tween(durationMillis = 320, easing = FastOutSlowInEasing)
        }
        val liftScale by animateFloatAsState(
            targetValue = if (isHeld) 1.1f else 1f,
            animationSpec = liftSpec,
            label = "navPillLiftScale"
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
                .pointerInput(tabWidthPx) {
                    val maxOffsetPx = tabWidthPx * (BOTTOM_NAV_ITEMS.size - 1)
                    fun trackTouch(x: Float) {
                        // Pill's left edge follows the finger directly (finger
                        // roughly centered over the pill), clamped so it never
                        // slides past the first/last tab.
                        dragOffsetPx = (x - tabWidthPx / 2f).coerceIn(0f, maxOffsetPx)
                        dragIndex = (x / tabWidthPx).toInt().coerceIn(0, BOTTOM_NAV_ITEMS.lastIndex)
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        trackTouch(down.position.x)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            change.consume()
                            trackTouch(change.position.x)
                        }
                        dragIndex?.let { index -> onNavigate(BOTTOM_NAV_ITEMS[index].route) }
                        dragIndex = null
                        dragOffsetPx = null
                    }
                }
        ) {
            Box(
                Modifier
                    .padding(6.dp)
                    .offset(x = indicatorOffset)
                    .graphicsLayer {
                        scaleX = liftScale
                        scaleY = liftScale
                    }
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
                    val selected = index == displayedIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize(),
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
