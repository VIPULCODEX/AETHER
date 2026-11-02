package com.aether.core.engine

private val PREFERRED = listOf(
    "llama-3.3-70b-versatile",
    "openai/gpt-oss-120b",
    "openai/gpt-oss-20b",
    "meta-llama/llama-4-maverick-17b-128e-instruct",
    "meta-llama/llama-4-scout-17b-16e-instruct",
    "qwen/qwen3-32b",
    "moonshotai/kimi-k2-instruct",
    "llama-3.1-8b-instant"
)

// Models that exist on the account but can't write a schedule.
private val NOT_CHAT = listOf("whisper", "guard", "tts", "playai", "orpheus", "embed", "safeguard", "compound", "prompt-guard")

/**
 * Picks a usable chat model from whatever the provider says this key can access, instead of hard-coding a name that
 * the provider may retire (which is exactly what produced "model_not_found"). Preferred models win in order; if
 * none is available, any remaining chat-capable model is better than failing.
 */
fun pickChatModel(availableIds: List<String>): String? {
    val usable = availableIds.filter { id -> NOT_CHAT.none { id.lowercase().contains(it) } }
    return PREFERRED.firstOrNull { it in usable } ?: usable.firstOrNull()
}

/** First JSON array in a model reply, tolerating code fences and reasoning models' <think> preambles. */
fun extractJsonArray(reply: String): String? {
    val cleaned = reply.replace(Regex("(?s)<think>.*?</think>"), "")
    val start = cleaned.indexOf('[')
    val end = cleaned.lastIndexOf(']')
    return if (start in 0 until end) cleaned.substring(start, end + 1) else null
}

/**
 * Picks a Claude model from whatever /v1/models returns for the user's own key (the API lists newest first), so no
 * model id is ever written into the app. Family preference only: balanced "sonnet" first, then "haiku", then the newest.
 */
fun pickClaudeModel(availableIds: List<String>): String? {
    val usable = availableIds.filter { it.startsWith("claude", ignoreCase = true) }
    return listOf("sonnet", "haiku").firstNotNullOfOrNull { fam -> usable.firstOrNull { fam in it.lowercase() } } ?: usable.firstOrNull()
}

/** First JSON object in a model reply, tolerating code fences and preambles. */
fun extractJsonObject(reply: String): String? {
    val start = reply.indexOf('{')
    val end = reply.lastIndexOf('}')
    return if (start in 0 until end) reply.substring(start, end + 1) else null
}
