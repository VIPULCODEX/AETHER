package com.aether.core.engine

import com.aether.core.model.Flashcard
import com.aether.core.model.StudyMistake
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun card(id: String, interval: Int, due: Long) = Flashcard(id, "n", null, "f", "b", 2.5, interval, 1, due, 0)
private fun miss(id: String, topic: String, due: Long, resolved: Boolean = false) = StudyMistake(id, "n", topic, "q", "a", "x", 0, 0, due, resolved)

class ProgressEngineTest {
    private val now = 100 * DAY

    @Test fun ranksWeakTopicsByOpenMistakes() {
        val p = ProgressEngine.snapshot(
            emptyList(),
            listOf(miss("1", "Cache", now - 1), miss("2", "Cache", now + DAY), miss("3", "Pipelining", now - 1), miss("4", "OS", 0, resolved = true)),
            now
        )
        assertEquals(listOf("Cache", "Pipelining"), p.weakTopics.map { it.topic })
        assertEquals(1, p.weakTopics[0].dueNow)
        assertEquals(3, p.mistakesOpen); assertEquals(1, p.mistakesResolved)
    }

    @Test fun countsDueAndMatureCards() {
        val p = ProgressEngine.snapshot(listOf(card("a", 30, now + DAY), card("b", 1, now - 1), card("c", 0, now)), emptyList(), now)
        assertEquals(2, p.cardsDue); assertEquals(1, p.cardsMature); assertEquals(33, p.maturePercent)
    }

    @Test fun mistakeGapsGrowThenCap() {
        assertEquals(now + DAY, ProgressEngine.nextMistakeDue(now, 0))
        assertEquals(now + 7 * DAY, ProgressEngine.nextMistakeDue(now, 2))
        assertTrue(ProgressEngine.nextMistakeDue(now, 99) == now + 21 * DAY)
    }
}
