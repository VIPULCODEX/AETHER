package com.aether.android.data

import com.aether.core.engine.extractJsonArray
import com.aether.core.engine.pickChatModel
import com.aether.core.model.ScheduleSlot
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GroqApiException(message: String) : Exception(message)

/**
 * Calls Groq's OpenAI-compatible chat completions endpoint using the
 * user's own API key (bring-your-own-key — never a key I hold). Only the
 * focus areas and the free-text description typed for scheduling purposes
 * are sent; nothing else from the Life Data Store.
 *
 * The model is not hard-coded: it's chosen from the list Groq returns for
 * the user's own key, because Groq retires models periodically.
 */
class GroqScheduleClient {

    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"
    private val modelsEndpoint = "https://api.groq.com/openai/v1/models"

    /** Chosen from the models this key can actually use; cleared if Groq later says it's gone. */
    @Volatile private var cachedModel: String? = null

    fun generateSchedule(apiKey: String, focusAreas: List<String>, description: String): List<ScheduleSlot> {
        return try {
            request(apiKey, focusAreas, description)
        } catch (e: GroqApiException) {
            // The provider retires models from time to time: re-pick from the live list and try once more.
            if (e.message?.contains("model_not_found") == true) {
                cachedModel = null
                request(apiKey, focusAreas, description)
            } else {
                throw e
            }
        }
    }

    private fun resolveModel(apiKey: String): String {
        cachedModel?.let { return it }
        val conn = (URL(modelsEndpoint).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
            if (code == 401) throw GroqApiException("Groq rejected this API key. Check it in Settings.")
            if (code !in 200..299) throw GroqApiException("Couldn't list Groq models ($code): $body")
            val data = JSONObject(body).getJSONArray("data")
            val ids = (0 until data.length()).map { data.getJSONObject(it).getString("id") }
            val picked = pickChatModel(ids) ?: throw GroqApiException("This Groq key has no chat model available.")
            cachedModel = picked
            return picked
        } finally {
            conn.disconnect()
        }
    }

    private fun request(apiKey: String, focusAreas: List<String>, description: String): List<ScheduleSlot> {
        val model = resolveModel(apiKey)
        val systemPrompt = """
            You are a scheduling assistant. Given the user's focus areas and their
            description of their routine and constraints, produce a realistic weekly
            schedule. Respond with ONLY a JSON array, no prose, no markdown fences.
            Each element must look exactly like:
            {"day": 0, "time": "7:00 AM - 8:30 AM", "activity": "short label", "domain": "one of the focus areas"}
            "day" is 0-6 where 0 = Monday and 6 = Sunday. Cover the full week.
        """.trimIndent()

        val userPrompt = "Focus areas: ${focusAreas.joinToString(", ")}\n\nMy routine and constraints: $description"

        val requestBody = JSONObject().apply {
            put("model", model)
            put("temperature", 0.3)
            put("max_completion_tokens", 4000)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                    put(JSONObject().apply { put("role", "user"); put("content", userPrompt) })
                }
            )
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
        }

        try {
            connection.outputStream.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(requestBody.toString()) }
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = responseStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            if (responseCode !in 200..299) {
                throw GroqApiException(
                    when (responseCode) {
                        401 -> "Groq rejected this API key. Check it in Settings."
                        429 -> "Groq's free rate limit was hit. Wait a minute and try again."
                        else -> "Groq API error ($responseCode): $responseText"
                    }
                )
            }

            val content = JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val cleanedJson = extractJsonArray(content)
                ?: throw GroqApiException("The AI reply didn't contain a schedule. Try again, or describe your routine more simply.")

            val jsonArray = JSONArray(cleanedJson)
            val slots = mutableListOf<ScheduleSlot>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                slots.add(
                    ScheduleSlot(
                        id = "ai_slot_$i",
                        dayOfWeek = obj.getInt("day"),
                        timeLabel = obj.getString("time"),
                        activityLabel = obj.getString("activity"),
                        domain = obj.optString("domain", "")
                    )
                )
            }
            return slots
        } finally {
            connection.disconnect()
        }
    }
}
