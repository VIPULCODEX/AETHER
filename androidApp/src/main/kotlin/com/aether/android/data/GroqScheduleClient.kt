package com.aether.android.data

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
 * The model name below is current as of this writing — Groq updates its
 * hosted model lineup periodically. If this starts failing with a
 * "model not found"-style error, check console.groq.com/docs/models and
 * swap the model string here.
 */
class GroqScheduleClient {

    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"
    private val model = "llama-3.3-70b-versatile"

    fun generateSchedule(apiKey: String, focusAreas: List<String>, description: String): List<ScheduleSlot> {
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
                throw GroqApiException("Groq API error ($responseCode): $responseText")
            }

            val content = JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val cleanedJson = content.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

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
