package com.aether.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelPickerTest {
    @Test fun prefersKnownGoodModelsInOrder() {
        assertEquals("openai/gpt-oss-120b", pickChatModel(listOf("whisper-large-v3", "openai/gpt-oss-20b", "openai/gpt-oss-120b")))
        assertEquals("llama-3.3-70b-versatile", pickChatModel(listOf("openai/gpt-oss-120b", "llama-3.3-70b-versatile")))
    }

    @Test fun neverPicksAudioOrGuardModels() {
        assertEquals("some-new-chat-model", pickChatModel(listOf("whisper-large-v3-turbo", "meta-llama/llama-guard-4-12b", "playai-tts", "some-new-chat-model")))
        assertNull(pickChatModel(listOf("whisper-large-v3", "playai-tts")))
        assertNull(pickChatModel(emptyList()))
    }

    @Test fun extractsArrayFromFencesAndReasoning() {
        assertEquals("[{\"a\":1}]", extractJsonArray("```json\n[{\"a\":1}]\n```"))
        assertEquals("[1,2]", extractJsonArray("<think>I should [maybe] reason</think>\nHere: [1,2] done"))
        assertNull(extractJsonArray("no array here"))
    }
}
