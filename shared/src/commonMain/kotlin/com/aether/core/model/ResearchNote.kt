package com.aether.core.model

data class ResearchNote(
    val id: String,
    val title: String,
    val note: String,
    val status: String,
    val createdAt: Long
)
