package com.aether.android.data

import android.content.Context
import android.net.Uri
import com.aether.core.engine.LocalLlm
import com.aether.core.engine.ModelFamily
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface LlmStatus {
    data object NotLoaded : LlmStatus
    data object Loading : LlmStatus
    data class Downloading(val percent: Int) : LlmStatus
    data class Ready(val modelName: String) : LlmStatus
    data class Failed(val message: String) : LlmStatus
}

private const val DEFAULT_MODEL_URL =
    "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"
private const val DEFAULT_MODEL_BYTES = 547_000_000L

/**
 * Runs a Gemma-family `.task` model (e.g. Gemma 3 1B int4, ~550 MB) through
 * MediaPipe's LLM Inference API, fully offline. The model is a one-time file
 * the user picks (Hugging Face needs a login for Gemma, so there is no
 * silent download); it's copied into app-private storage and loaded from
 * there. The chat template below is Gemma's; a different model family needs
 * its own template.
 */
class OnDeviceLlm(context: Context) : LocalLlm {

    private val appContext = context.applicationContext
    private val modelFile = File(appContext.filesDir, "models/model.task")
    private val partFile = File(appContext.filesDir, "models/model.task.part")
    private val familyFile = File(appContext.filesDir, "models/family.txt")
    private val mutex = Mutex()
    private var engine: LlmInference? = null

    private val _status = MutableStateFlow<LlmStatus>(LlmStatus.NotLoaded)
    val status: StateFlow<LlmStatus> = _status.asStateFlow()

    /** True when a model is loaded *or* can be loaded on demand (it is unloaded in the background to free RAM). */
    override val isReady: Boolean get() = engine != null || (modelFile.exists() && _status.value !is LlmStatus.Failed && _status.value !is LlmStatus.Loading && _status.value !is LlmStatus.Downloading)

    val hasPartialDownload: Boolean get() = partFile.exists() && partFile.length() > 0

    val hasModelFile: Boolean get() = modelFile.exists()

    /** Disk used by the imported model, for the storage overview. */
    val modelBytes: Long get() = if (modelFile.exists()) modelFile.length() else 0L

    private var family: ModelFamily
        get() = runCatching { ModelFamily.valueOf(familyFile.readText().trim()) }.getOrDefault(ModelFamily.GEMMA)
        set(value) { familyFile.parentFile?.mkdirs(); familyFile.writeText(value.name) }

    /**
     * One-tap default model: Qwen2.5-0.5B-Instruct (Apache-2.0, ungated, ~547 MB). Resumable, so a dropped
     * connection continues instead of restarting. After this, everything is offline.
     */
    suspend fun downloadDefaultModel() {
        if (_status.value is LlmStatus.Downloading) return
        _status.value = LlmStatus.Downloading(0)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                modelFile.parentFile?.mkdirs()
                val free = modelFile.parentFile!!.usableSpace
                check(free > DEFAULT_MODEL_BYTES * 2) { "Not enough free space (need about 1.2 GB)." }
                var done = if (partFile.exists()) partFile.length() else 0L
                var redirects = 0
                var url = DEFAULT_MODEL_URL
                while (true) {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 20_000
                        readTimeout = 30_000
                        if (done > 0) setRequestProperty("Range", "bytes=$done-")
                    }
                    try {
                        val code = conn.responseCode
                        if (code in 300..399 && redirects++ < 5) { url = conn.getHeaderField("Location") ?: error("Bad redirect"); continue }
                        check(code == 200 || code == 206) { "Download failed (HTTP $code)." }
                        if (code == 200 && done > 0) { partFile.delete(); done = 0 } // server ignored Range: start over
                        val total = if (conn.contentLengthLong > 0) (if (code == 206) done else 0L) + conn.contentLengthLong else 0L
                        java.io.FileOutputStream(partFile, code == 206).use { out ->
                            conn.inputStream.use { input ->
                                val buf = ByteArray(64 * 1024)
                                var lastPercent = -1
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                    done += n
                                    val percent = if (total > 0) (done * 100 / total).toInt() else 0
                                    if (percent != lastPercent) { lastPercent = percent; _status.value = LlmStatus.Downloading(percent) }
                                }
                            }
                        }
                        check(partFile.length() >= DEFAULT_MODEL_BYTES * 9 / 10) { "Download was cut short. Tap Download again to resume." }
                        modelFile.delete()
                        check(partFile.renameTo(modelFile)) { "Couldn't save the model." }
                        break
                    } finally {
                        conn.disconnect()
                    }
                }
            }
        }
        result.onFailure {
            _status.value = LlmStatus.Failed(it.message ?: "Download failed. Check your connection and try again.")
            return
        }
        family = ModelFamily.QWEN
        engine = null
        load("Qwen2.5-0.5B (built-in default)")
    }

    /** Loads a previously imported model, if any. Cheap to call repeatedly. */
    suspend fun loadIfPresent() {
        if (engine != null || !modelFile.exists()) return
        load()
    }

    /** Copies a user-picked model file into private storage, then loads it. */
    suspend fun importModel(uri: Uri, displayName: String) {
        _status.value = LlmStatus.Loading
        runCatching {
            withContext(Dispatchers.IO) {
                modelFile.parentFile?.mkdirs()
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    modelFile.outputStream().use { input.copyTo(it) }
                } ?: error("Couldn't open the selected file")
            }
        }.onFailure {
            _status.value = LlmStatus.Failed(it.message ?: "Import failed")
            return
        }
        family = ModelFamily.fromFileName(displayName)
        engine = null
        load(displayName)
    }

    /** Frees the model's RAM (hundreds of MB) without deleting the file; it reloads on next use. */
    fun unload() {
        if (engine == null) return
        engine?.close()
        engine = null
        _status.value = LlmStatus.NotLoaded
    }

    fun removeModel() {
        engine?.close()
        engine = null
        modelFile.delete()
        partFile.delete()
        _status.value = LlmStatus.NotLoaded
    }

    private suspend fun load(name: String = "On-device model") {
        _status.value = LlmStatus.Loading
        runCatching {
            withContext(Dispatchers.Default) {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1280)
                    .build()
                engine = LlmInference.createFromOptions(appContext, options)
            }
        }.onSuccess {
            _status.value = LlmStatus.Ready(name)
        }.onFailure {
            engine = null
            _status.value = LlmStatus.Failed(it.message ?: "Couldn't load the model (is it a MediaPipe .task file?)")
        }
    }

    override suspend fun generate(prompt: String): String = mutex.withLock {
        if (engine == null && modelFile.exists()) load()
        val llm = engine ?: error("Model not loaded")
        withContext(Dispatchers.Default) {
            llm.generateResponse(family.wrap(prompt))
        }
    }
}
