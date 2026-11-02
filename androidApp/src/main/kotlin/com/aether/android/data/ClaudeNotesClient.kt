package com.aether.android.data

import com.aether.core.engine.NoteChunk
import com.aether.core.engine.StructuredNote
import com.aether.core.engine.extractJsonObject
import com.aether.core.engine.pickClaudeModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ClaudeApiException(message: String) : Exception(message)

/**
 * Opt-in "Deep notes": sends ONE heading-sized chunk of the user's own material at a time to the Anthropic API using
 * the user's own key, and gets structured notes back. Nothing else leaves the device. The model is never hard-coded:
 * it's picked from the list the key can access, and re-picked once if the API reports it gone.
 */
class ClaudeNotesClient {

    private val base = "https://api.anthropic.com/v1"
    @Volatile private var cachedModel: String? = null

    fun extract(apiKey: String, chunk: NoteChunk): StructuredNote =
        try {
            request(apiKey, chunk)
        } catch (e: ClaudeApiException) {
            if (e.message?.contains("not_found") == true) { cachedModel = null; request(apiKey, chunk) } else throw e
        }

    private fun call(path: String, apiKey: String, body: JSONObject?): String {
        val conn = (URL("$base$path").openConnection() as HttpURLConnection).apply {
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            connectTimeout = 20_000
            readTimeout = 90_000
            if (body != null) {
                requestMethod = "POST"
                setRequestProperty("content-type", "application/json")
                doOutput = true
            }
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
            if (code !in 200..299) throw ClaudeApiException(
                when (code) {
                    401 -> "Anthropic rejected this API key. Check it in Settings."
                    429 -> "Rate limit hit. Wait a minute and try again."
                    529 -> "Anthropic is overloaded right now. Try again shortly."
                    else -> "Anthropic API error ($code): $text"
                }
            )
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveModel(apiKey: String): String {
        cachedModel?.let { return it }
        val data = JSONObject(call("/models?limit=100", apiKey, null)).getJSONArray("data")
        val ids = (0 until data.length()).map { data.getJSONObject(it).getString("id") }
        return (pickClaudeModel(ids) ?: throw ClaudeApiException("This key has no Claude model available.")).also { cachedModel = it }
    }

    private fun request(apiKey: String, chunk: NoteChunk): StructuredNote {
        val system = """
            You turn ONE section of a student's own study material into structured notes.
            Use ONLY the text provided. Never add facts, numbers or formulas that are not in it.
            If a field has nothing in the text, leave it empty. Copy formulas and numbers exactly as written.
            Reply with ONLY a JSON object, no prose, no code fences:
            {"topic": "", "summary": "one line", "definitions": [], "formulas": [], "traps": [], "example": "", "flow": "A -> B -> C"}
            "traps" are common mistakes or exceptions the text warns about.
        """.trimIndent()
        val body = JSONObject().apply {
            put("model", resolveModel(apiKey))
            put("max_tokens", 1500)
            put("system", system)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Section \"${chunk.heading}\" (pages ${chunk.pageStart}-${chunk.pageEnd}):\n\n${chunk.text}")))
        }
        val reply = JSONObject(call("/messages", apiKey, body)).getJSONArray("content").getJSONObject(0).getString("text")
        val json = JSONObject(extractJsonObject(reply) ?: throw ClaudeApiException("The model didn't return structured notes."))
        fun list(k: String) = json.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } } ?: emptyList()
        return StructuredNote(
            topic = json.optString("topic").ifBlank { chunk.heading },
            summary = json.optString("summary"), definitions = list("definitions"), formulas = list("formulas"), traps = list("traps"),
            example = json.optString("example"), flow = json.optString("flow"), pageStart = chunk.pageStart, pageEnd = chunk.pageEnd
        )
    }
}
