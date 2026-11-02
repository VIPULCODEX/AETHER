package com.aether.core.model

/** [examNotes] is the user's own description of how this subject is examined (e.g. "GATE CS: mostly numericals on cache mapping"); it steers explanations and practice questions. */
data class Notebook(val id: String, val title: String, val createdAt: Long, val examNotes: String? = null)

data class StudySource(
    val id: String,
    val notebookId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    /** Set when the source was read from a web page. */
    val url: String? = null
)

/** SM-2 scheduling state lives on the card itself, so "what's due" is one indexed query. */
data class Flashcard(
    val id: String,
    val notebookId: String,
    val sourceId: String?,
    val front: String,
    val back: String,
    val ease: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueAt: Long,
    val createdAt: Long
)
