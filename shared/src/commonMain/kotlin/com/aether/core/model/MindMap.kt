package com.aether.core.model

enum class MindNodeKind { ROOT, BRANCH, LEAF, DONE }

/** One node of a derived mind map. Maps are computed from notes/goals on demand, never stored, so they can't go stale. */
data class MindNode(
    val id: String,
    val label: String,
    val detail: String,
    val parentId: String?,
    val kind: MindNodeKind
)

data class PositionedNode(
    val node: MindNode,
    val x: Float,
    val y: Float,
    val depth: Int,
    val hasChildren: Boolean,
    val collapsed: Boolean
)

data class MindMapLayout(
    val nodes: List<PositionedNode>,
    /** (parentId, childId) pairs for drawing connectors. */
    val edges: List<Pair<String, String>>,
    /** Distance from the root to the farthest node, so the view can size/center its canvas. */
    val extent: Float
)
