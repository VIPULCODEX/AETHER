package com.aether.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelFamilyTest {
    @Test fun wrapsPromptsInEachFamiliesChatTemplate() {
        assertTrue(ModelFamily.GEMMA.wrap("hi").startsWith("<start_of_turn>user\nhi<end_of_turn>"))
        val qwen = ModelFamily.QWEN.wrap("hi")
        assertTrue("<|im_start|>user\nhi<|im_end|>" in qwen && qwen.endsWith("<|im_start|>assistant\n"))
    }

    @Test fun detectsFamilyFromFileName() {
        assertEquals(ModelFamily.QWEN, ModelFamily.fromFileName("Qwen2.5-0.5B-Instruct_q8.task"))
        assertEquals(ModelFamily.GEMMA, ModelFamily.fromFileName("gemma3-1b-it-int4.task"))
        assertEquals(ModelFamily.GEMMA, ModelFamily.fromFileName("model.task"))
    }

    @Test fun promptBudgetsFitTheDefaultModelsWindow() {
        // ~4 chars/token: context (2.2k chars) + instructions + a 400-token answer must fit 1280 tokens.
        val longNotes = "x".repeat(10_000)
        val llm = FakeLlm("- one point here\n- two points here")
        runSuspend { StudyEngine().summarizeGrounded(longNotes, llm) }
        assertTrue(llm.lastPrompt.length < 2_600, "prompt chars=${llm.lastPrompt.length}")
    }
}
