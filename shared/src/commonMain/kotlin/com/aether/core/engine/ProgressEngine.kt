package com.aether.core.engine

import com.aether.core.model.Flashcard
import com.aether.core.model.StudyMistake

data class TopicWeakness(val topic: String, val open: Int, val dueNow: Int)

/** One glance at how studying is going, from data the app already keeps (no tracking). */
data class StudyProgress(
    val cardsTotal: Int,
    val cardsDue: Int,
    /** Cards whose review interval has reached 3 weeks: the ones that have genuinely stuck. */
    val cardsMature: Int,
    val mistakesOpen: Int,
    val mistakesDue: Int,
    val mistakesResolved: Int,
    val weakTopics: List<TopicWeakness>
) {
    val maturePercent: Int get() = if (cardsTotal == 0) 0 else cardsMature * 100 / cardsTotal
    val hasAnything: Boolean get() = cardsTotal > 0 || mistakesOpen + mistakesResolved > 0
}

object ProgressEngine {
    const val RESOLVE_AFTER = 4
    private const val DAY = 24L * 60 * 60 * 1000
    private val MISTAKE_GAPS_DAYS = longArrayOf(1, 3, 7, 21)
    private const val MATURE_DAYS = 21

    /** Next time a mistake should resurface after [reviewCount] correct reviews in a row: 1d, 3d, 7d, 21d. */
    fun nextMistakeDue(now: Long, reviewCount: Int): Long = now + MISTAKE_GAPS_DAYS[reviewCount.coerceIn(0, MISTAKE_GAPS_DAYS.lastIndex)] * DAY

    fun snapshot(cards: List<Flashcard>, mistakes: List<StudyMistake>, now: Long, topN: Int = 3): StudyProgress {
        val open = mistakes.filter { !it.resolved }
        val weak = open.groupBy { it.topic }
            .map { (topic, ms) -> TopicWeakness(topic, ms.size, ms.count { it.dueAt <= now }) }
            .sortedWith(compareByDescending<TopicWeakness> { it.open }.thenByDescending { it.dueNow }.thenBy { it.topic })
            .take(topN)
        return StudyProgress(
            cardsTotal = cards.size,
            cardsDue = cards.count { it.dueAt <= now },
            cardsMature = cards.count { it.intervalDays >= MATURE_DAYS },
            mistakesOpen = open.size,
            mistakesDue = open.count { it.dueAt <= now },
            mistakesResolved = mistakes.count { it.resolved },
            weakTopics = weak
        )
    }
}
