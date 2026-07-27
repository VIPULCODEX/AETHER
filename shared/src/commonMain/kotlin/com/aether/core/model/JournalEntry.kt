package com.aether.core.model

data class JournalEntry(
    val id: String,
    val createdAt: Long,
    val content: String,
    val mood: Int?,
    val attachmentUri: String?
)
