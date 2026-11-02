package com.aether.core.engine

/**
 * A language model running entirely on this device. Platform code supplies
 * the implementation; the shared engines only depend on this, so retrieval
 * and grounding rules are identical (and testable) with or without a model.
 */
interface LocalLlm {
    val isReady: Boolean

    /** Returns the model's completion for a plain-text prompt. May throw if inference fails. */
    suspend fun generate(prompt: String): String
}
