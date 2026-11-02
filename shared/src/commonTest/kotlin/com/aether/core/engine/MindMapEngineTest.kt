package com.aether.core.engine

import com.aether.core.model.MindNodeKind
import com.aether.core.model.StudySource
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindMapEngineTest {
    private val engine = MindMapEngine()
    private val text = "The Calvin cycle uses ATP and NADPH to fix carbon dioxide into sugars. NADPH is produced in the light reactions of photosynthesis. Chlorophyll absorbs light for photosynthesis in the thylakoid membranes. Photosynthesis happens in chloroplasts of plant cells."

    @Test fun layoutPlacesEveryNodeWithoutOverlap() {
        val nodes = engine.fromSources("Bio", listOf(StudySource("s", "n", "Notes", text, 0)))
        val layout = engine.layout(nodes)
        assertEquals(nodes.size, layout.nodes.size)
        assertEquals(nodes.size - 1, layout.edges.size)
        for (a in layout.nodes) for (b in layout.nodes) if (a !== b) assertTrue(hypot(a.x - b.x, a.y - b.y) > 20f)
    }

    @Test fun noEmptyBranches() {
        val nodes = engine.fromSources("Bio", listOf(StudySource("s", "n", "Notes", text, 0)))
        nodes.filter { it.kind == MindNodeKind.BRANCH }.forEach { b -> assertTrue(nodes.any { it.parentId == b.id }) }
    }

    @Test fun collapsingHidesDescendants() {
        val nodes = engine.fromSources("Bio", listOf(StudySource("s", "n", "Notes", text, 0)))
        val branch = nodes.first { it.kind == MindNodeKind.BRANCH }
        assertTrue(engine.layout(nodes, setOf(branch.id)).nodes.size < engine.layout(nodes).nodes.size)
    }

    @Test fun emptyInputYieldsEmptyLayout() {
        assertTrue(engine.layout(emptyList()).nodes.isEmpty())
    }
}
