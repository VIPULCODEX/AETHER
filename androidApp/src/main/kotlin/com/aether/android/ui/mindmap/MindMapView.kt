package com.aether.android.ui.mindmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBorder
import com.aether.android.ui.theme.AetherCyan
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherIndigo
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface1
import com.aether.android.ui.theme.AetherSurface2
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.engine.MindMapEngine
import com.aether.core.model.MindNode
import com.aether.core.model.MindNodeKind
import com.aether.core.model.PositionedNode

private const val MIN_ZOOM = 0.35f
private const val MAX_ZOOM = 2.5f
private val CANVAS_PADDING = 120.dp

/**
 * Pan (drag) and zoom (pinch) canvas for a derived mind map. Tap a node to
 * read its full text; tap a selected node again to collapse or expand its
 * branch. The layout is recomputed from [nodes] each time, so the map always
 * mirrors the underlying notes/goals.
 */
@Composable
fun MindMapView(
    nodes: List<MindNode>,
    engine: MindMapEngine,
    modifier: Modifier = Modifier
) {
    var collapsed by remember { mutableStateOf(emptySet<String>()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(0.9f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val layout = remember(nodes, collapsed) { engine.layout(nodes, collapsed) }
    val density = LocalDensity.current
    val extent = layout.extent.dp
    val canvasSize = extent * 2 + CANVAS_PADDING * 2
    val half = canvasSize / 2
    val byId = remember(layout) { layout.nodes.associateBy { it.node.id } }
    val selected = selectedId?.let { byId[it] }

    Box(
        modifier
            .background(AetherSurface1, RoundedCornerShape(20.dp))
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoom, _ ->
                    scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    pan += panChange
                }
            }
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .requiredSize(canvasSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val d = density.density
                layout.edges.forEach { (from, to) ->
                    val a = byId[from] ?: return@forEach
                    val b = byId[to] ?: return@forEach
                    drawLine(
                        color = AetherBorder,
                        start = Offset(cx + a.x * d, cy + a.y * d),
                        end = Offset(cx + b.x * d, cy + b.y * d),
                        strokeWidth = 2f * d
                    )
                }
            }
            layout.nodes.forEach { positioned ->
                Box(
                    Modifier
                        .offset(half + positioned.x.dp, half + positioned.y.dp)
                        .size(0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NodeChip(
                        positioned = positioned,
                        selected = positioned.node.id == selectedId,
                        onClick = {
                            val id = positioned.node.id
                            if (selectedId == id && positioned.hasChildren) {
                                collapsed = if (id in collapsed) collapsed - id else collapsed + id
                            }
                            selectedId = id
                        }
                    )
                }
            }
        }

        Text(
            "Reset view",
            style = MaterialTheme.typography.labelSmall,
            color = AetherTextSecondary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { scale = 0.9f; pan = Offset.Zero }
                .padding(12.dp)
        )

        selected?.let {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .background(AetherSurface2, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(it.node.detail, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                if (it.hasChildren) {
                    Text(
                        if (it.collapsed) "Tap again to expand" else "Tap again to collapse",
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeChip(positioned: PositionedNode, selected: Boolean, onClick: () -> Unit) {
    val (fill, text) = when (positioned.node.kind) {
        MindNodeKind.ROOT -> AetherIndigo to AetherOnAccent
        MindNodeKind.BRANCH -> (if (positioned.depth % 2 == 1) AetherSky else AetherCyan) to AetherOnAccent
        MindNodeKind.DONE -> AetherEmerald to AetherOnAccent
        MindNodeKind.LEAF -> AetherSurface2 to AetherTextPrimary
    }
    val label = positioned.node.label + if (positioned.collapsed) "  +" else ""
    Text(
        label,
        style = if (positioned.node.kind == MindNodeKind.LEAF) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
        color = text,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .widthIn(max = 124.dp)
            .background(fill, RoundedCornerShape(14.dp))
            .then(if (selected) Modifier.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
