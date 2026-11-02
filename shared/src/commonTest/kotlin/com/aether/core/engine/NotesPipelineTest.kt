package com.aether.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val PAGES = listOf(
    PageText(1, "CACHE MEMORY\nCache memory is defined as a small fast memory between CPU and main memory. It reduces average access time.\nAMAT = hit time + miss rate * miss penalty\nNote that a higher hit ratio does not always mean a faster system."),
    PageText(2, "Example: hit time 2 ns, miss rate 0.1, miss penalty 100 ns, so AMAT = 12 ns.\nCPU -> Cache -> Main memory\n2.1 Mapping\nDirect mapping is called the simplest scheme because each block has exactly one line.")
)

class NotesPipelineTest {
    private val chunks = NotesPipeline.chunk(PAGES)

    @Test fun chunksByHeadingKeepingPages() {
        assertEquals(listOf("CACHE MEMORY", "2.1 Mapping"), chunks.map { it.heading })
        assertEquals(1, chunks[0].pageStart); assertEquals(2, chunks[0].pageEnd)
    }

    @Test fun ruleExtractionFindsStructure() {
        val n = NotesPipeline.extractRules(chunks[0])
        assertTrue(n.formulas.any { it.startsWith("AMAT") })
        assertTrue(n.definitions.any { "defined as" in it })
        assertTrue(n.traps.any { "Note that" in it })
        assertTrue(n.example.startsWith("Example"))
        assertTrue("->" in n.flow)
    }

    @Test fun verifyPassesGroundedNote() {
        val v = NotesPipeline.verify(NotesPipeline.extractRules(chunks[0]), chunks[0].text)
        assertFalse(v.lowConfidence, v.flags.toString())
    }

    @Test fun verifyFlagsInventedFormulaAndNumber() {
        val bad = StructuredNote("Cache", formulas = listOf("AMAT = hit time * miss rate"), definitions = listOf("Cache stores 64 gigabytes of quantum data"), pageStart = 1, pageEnd = 2)
        val v = NotesPipeline.verify(bad, chunks[0].text)
        assertTrue(v.lowConfidence)
        assertTrue(v.flags.size >= 2, v.flags.toString())
    }

    @Test fun mergeJoinsContinuedTopics() {
        val m = NotesPipeline.merge(listOf(StructuredNote("Cache", pageStart = 1, pageEnd = 1, formulas = listOf("a = b")), StructuredNote("Cache (cont.)", pageStart = 2, pageEnd = 3, formulas = listOf("a = b", "c = d"))))
        assertEquals(1, m.size); assertEquals(2, m[0].formulas.size); assertEquals(3, m[0].pageEnd)
    }

    @Test fun revisionSheetNeverExceedsCap() {
        val many = (1..60).map { StructuredNote("Topic $it", summary = "x".repeat(130), formulas = listOf("a = $it"), pageStart = it, pageEnd = it) }
        assertTrue(NotesPipeline.revisionSheet(many, 1000).length <= 1000)
    }
}
