package com.aether.core.engine

import com.aether.core.model.Goal
import com.aether.core.model.GoalType
import com.aether.core.model.Task
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/** Runs a suspend block to completion without a coroutines dependency (the fakes below never actually suspend). */
fun <T> runSuspend(block: suspend () -> T): T {
    var out: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { out = it })
    return out!!.getOrThrow()
}

class FakeLlm(var reply: String? = "", override var isReady: Boolean = true) : LocalLlm {
    var calls = 0
    var lastPrompt = ""
    override suspend fun generate(prompt: String): String {
        calls++
        lastPrompt = prompt
        return reply ?: error("model failure")
    }
}

fun goal(id: String, domain: String, due: Long? = null, effort: Int = 3, impact: Int = 3) =
    Goal(id, "G-$id", domain, due, 0, 0.0, false, GoalType.WEEKLY, null, null, effort, impact, null)

fun task(id: String, goalId: String, done: Boolean = false, at: Long? = null, due: Long? = null, effort: Int? = null) =
    Task(id, goalId, "T-$id", due, null, effort, null, null, done, at, 0)

const val DAY = 86_400_000L
