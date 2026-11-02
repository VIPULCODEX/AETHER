package com.aether.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.DpOffset
import kotlin.math.abs
import kotlin.math.roundToInt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBackground
import com.aether.android.ui.theme.AetherBorder
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
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

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

private val MORE_ROUTES = setOf("more", "research", "settings", "personalize", "study", "lifemap", "coming_soon/gate")

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
                    blur(18.dp.toPx())
                },
                // A full-bleed bar needs neither a rim light nor a drop shadow; a single hairline reads cleaner.
                highlight = null,
                shadow = null,
                onDrawSurface = {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(AetherBackground.copy(alpha = 0.78f), AetherBackground.copy(alpha = 0.42f))
                        )
                    )
                    val line = 1.dp.toPx()
                    drawRect(AetherBorder.copy(alpha = 0.9f), topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                }
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
    // nearest tab (icon tint + which tab a release commits to); dragOffsetPx
    // is the finger-relative target the pill's spring chases.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableStateOf<Float?>(null) }
    val displayedIndex = dragIndex ?: selectedIndex
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

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

        // The pill is a spring chasing its target (the finger while held, the
        // selected tab otherwise). Retargeting keeps its velocity, so it
        // trails the finger slightly and overshoots a touch when it settles,
        // like a drop of liquid, instead of following a fixed-duration tween.
        val pillX = remember { Animatable(settledTargetPx) }
        val target = dragOffsetPx ?: settledTargetPx
        LaunchedEffect(target, isHeld) {
            pillX.animateTo(
                target,
                if (isHeld) spring(dampingRatio = 0.82f, stiffness = 650f)
                else spring(dampingRatio = 0.6f, stiffness = 300f)
            )
        }

        // Pops larger when grabbed, relaxes on release. Scales about its own centre.
        val lift by animateFloatAsState(
            targetValue = if (isHeld) 1.12f else 1f,
            animationSpec = if (isHeld) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium) else spring(0.7f, 260f),
            label = "navPillLift"
        )
        // Light glow that rises while held, so the grabbed pill reads as "lit".
        val glow by animateFloatAsState(if (isHeld) 1f else 0f, tween(220), label = "navPillGlow")

        Box(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(50) },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(22.dp.toPx(), 26.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight(width = 1.dp, alpha = 0.9f, style = HighlightStyle.Default(angle = 55f)) },
                    shadow = { Shadow(radius = 22.dp, offset = DpOffset(0.dp, 8.dp), color = Color.Black.copy(alpha = 0.28f)) },
                    innerShadow = { InnerShadow(radius = 10.dp, offset = DpOffset(0.dp, 3.dp), color = Color.White.copy(alpha = 0.10f)) },
                    onDrawSurface = {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(AetherSurface1.copy(alpha = 0.58f), AetherSurface1.copy(alpha = 0.36f))
                            )
                        )
                    }
                )
                .pointerInput(tabWidthPx) {
                    val maxOffsetPx = tabWidthPx * (BOTTOM_NAV_ITEMS.size - 1)
                    fun trackTouch(x: Float) {
                        dragOffsetPx = (x - tabWidthPx / 2f).coerceIn(0f, maxOffsetPx)
                        val index = (x / tabWidthPx).toInt().coerceIn(0, BOTTOM_NAV_ITEMS.lastIndex)
                        if (index != dragIndex) {
                            // A detent under the finger each time the pill crosses into a new tab.
                            if (dragIndex != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            dragIndex = index
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        trackTouch(down.position.x)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            change.consume()
                            trackTouch(change.position.x)
                        }
                        dragIndex?.let { index ->
                            if (index != selectedIndex) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onNavigate(BOTTOM_NAV_ITEMS[index].route)
                        }
                        dragIndex = null
                        dragOffsetPx = null
                    }
                }
        ) {
            Box(
                Modifier
                    .padding(6.dp)
                    .offset { IntOffset(pillX.value.roundToInt(), 0) }
                    .size(width = tabWidth - 12.dp, height = 54.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(50) },
                        effects = {
                            lens(12.dp.toPx(), 18.dp.toPx(), depthEffect = true, chromaticAberration = true)
                        },
                        highlight = { Highlight(width = 1.dp, alpha = 0.6f + 0.4f * glow, style = HighlightStyle.Default(angle = 60f)) },
                        shadow = { Shadow(radius = 12.dp, offset = DpOffset(0.dp, 4.dp), color = Color.Black.copy(alpha = 0.22f)) },
                        innerShadow = { InnerShadow(radius = 8.dp, offset = DpOffset(0.dp, 2.dp), color = Color.White.copy(alpha = 0.08f + 0.10f * glow)) },
                        layerBlock = {
                            // Squash and stretch along the direction of travel, proportional to speed.
                            val stretch = (abs(pillX.velocity) / (tabWidthPx * 9f)).coerceIn(0f, 0.22f)
                            scaleX = lift * (1f + stretch)
                            scaleY = lift * (1f - stretch * 0.45f)
                        },
                        onDrawSurface = {
                            drawRect(AetherSky.copy(alpha = 0.22f + 0.10f * glow))
                        }
                    )
            )

            Row(Modifier.fillMaxSize()) {
                BOTTOM_NAV_ITEMS.forEachIndexed { index, item ->
                    val selected = index == displayedIndex
                    val tint by animateColorAsState(
                        if (selected) AetherSky else AetherTextSecondary, tween(180), label = "navTint"
                    )
                    val iconScale by animateFloatAsState(
                        if (selected) 1.14f else 1f, spring(0.55f, 420f), label = "navIconScale"
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(21.dp)
                                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                            )
                            Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint)
                        }
                    }
                }
            }
        }
    }
}
