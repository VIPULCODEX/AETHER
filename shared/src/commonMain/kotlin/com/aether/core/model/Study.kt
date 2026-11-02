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
    val url: String? = null,
    /** The user's own video file, when this source is a video with subtitles. */
    val mediaUri: String? = null,
    /** Timed cues for that video: "startMs|text" per line. */
    val transcript: String? = null
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

/** A wrong answer kept for spaced revision. [dueAt] moves out each time it's answered right; after enough, it's [resolved]. */
data class StudyMistake(
    val id: String,
    val notebookId: String,
    val topic: String,
    val question: String,
    val correctAnswer: String,
    val yourAnswer: String,
    val createdAt: Long,
    val reviewCount: Int,
    val dueAt: Long,
    val resolved: Boolean
)
