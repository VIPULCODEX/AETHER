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

/** Instruction-tuned chat models expect their own prompt wrapper; sending bare text degrades them badly. */
enum class ModelFamily {
    GEMMA, QWEN;

    fun wrap(prompt: String): String = when (this) {
        GEMMA -> "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"
        QWEN -> "<|im_start|>system\nYou are a precise study assistant. Follow the instructions exactly.<|im_end|>\n<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
    }

    companion object {
        /** Best guess from a file name; user-imported files are Gemma unless they say otherwise. */
        fun fromFileName(name: String): ModelFamily = if (name.lowercase().contains("qwen")) QWEN else GEMMA
    }
}
