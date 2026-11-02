package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aether.core.db.AetherDatabase
import com.aether.core.engine.CardDraft
import com.aether.core.engine.Schedule
import com.aether.core.model.Flashcard
import com.aether.core.engine.ProgressEngine
import com.aether.core.model.Notebook
import com.aether.core.model.StudyMistake
import com.aether.core.model.StudySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class NotebookStorage(val notebookId: String, val title: String, val sourceCount: Int, val bytes: Long, val cardCount: Int)

/** Notebooks, their sources and spaced-repetition cards. Source text never leaves the device. */
class StudyRepository(private val database: AetherDatabase) {

    private val q get() = database.aetherQueries

    fun observeNotebooks(): Flow<List<Notebook>> =
        q.selectAllNotebooks().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    suspend fun createNotebook(title: String): String {
        val id = generateId()
        q.insertNotebook(id, title.trim(), currentTimeMillis())
        return id
    }

    suspend fun deleteNotebook(id: String) {
        q.transaction {
            q.deleteFlashcardsForNotebook(id)
            q.deleteMistakesForNotebook(id)
            q.deleteSourcesForNotebook(id)
            q.deleteNotebook(id)
        }
    }

    fun observeSources(notebookId: String): Flow<List<StudySource>> =
        q.selectSourcesForNotebook(notebookId).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    suspend fun addSource(
        notebookId: String, title: String, content: String, url: String? = null,
        mediaUri: String? = null, transcript: String? = null
    ) {
        q.insertStudySource(generateId(), notebookId, title.trim(), content.trim(), currentTimeMillis(), url, mediaUri, transcript)
    }

    suspend fun setExamNotes(notebookId: String, notes: String) {
        q.updateNotebookExamNotes(notes.trim().ifBlank { null }, notebookId)
    }

    /** Approximate on-device footprint of each notebook's text and cards, so the user can see and reclaim space. */
    fun observeStorage(): Flow<List<NotebookStorage>> = combine(
        observeNotebooks(),
        q.selectSourceStorage().asFlow().mapToList(Dispatchers.Default),
        q.selectCardCounts().asFlow().mapToList(Dispatchers.Default)
    ) { notebooks, sources, cards ->
        notebooks.map { nb ->
            val src = sources.find { it.notebookId == nb.id }
            NotebookStorage(
                notebookId = nb.id,
                title = nb.title,
                sourceCount = src?.sourceCount?.toInt() ?: 0,
                bytes = src?.chars ?: 0L,
                cardCount = cards.find { it.notebookId == nb.id }?.cardCount?.toInt() ?: 0
            )
        }
    }

    suspend fun deleteSource(id: String) {
        q.transaction {
            q.deleteFlashcardsForSource(id)
            q.deleteStudySource(id)
        }
    }

    fun observeCards(notebookId: String): Flow<List<Flashcard>> =
        q.selectFlashcardsForNotebook(notebookId).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    /** New cards are due immediately. Callers dedupe against existing fronts. */
    suspend fun addCards(notebookId: String, drafts: List<CardDraft>) {
        val now = currentTimeMillis()
        q.transaction {
            drafts.forEach { d ->
                q.insertFlashcard(generateId(), notebookId, d.sourceId, d.front, d.back, 2.5, 0L, 0L, now, now)
            }
        }
    }

    suspend fun applySchedule(cardId: String, schedule: Schedule) {
        q.updateFlashcardSchedule(schedule.ease, schedule.intervalDays.toLong(), schedule.repetitions.toLong(), schedule.dueAt, cardId)
    }

    // ---- error notebook ------------------------------------------------------
    fun observeMistakes(): Flow<List<StudyMistake>> =
        q.selectAllMistakes().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    fun observeAllCards(): Flow<List<Flashcard>> =
        q.selectAllFlashcards().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    suspend fun recordMistake(notebookId: String, topic: String, question: String, correct: String, yours: String) {
        val now = currentTimeMillis()
        q.insertMistake(generateId(), notebookId, topic.trim().ifBlank { "General" }, question.trim(), correct.trim(), yours.trim(), now, ProgressEngine.nextMistakeDue(now, 0))
    }

    /** Answering a revisited mistake right pushes it out; wrong resets it to tomorrow. Enough right answers resolves it. */
    suspend fun reviewMistake(m: StudyMistake, gotItRight: Boolean) {
        val now = currentTimeMillis()
        val count = if (gotItRight) m.reviewCount + 1 else 0
        q.updateMistakeReview(count.toLong(), ProgressEngine.nextMistakeDue(now, count), if (count >= ProgressEngine.RESOLVE_AFTER) 1L else 0L, m.id)
    }

    suspend fun deleteMistake(id: String) {
        q.deleteMistake(id)
    }
}
