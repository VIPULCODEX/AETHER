package com.aether.android.data

import android.content.Context
import android.net.Uri
import com.aether.core.engine.LocalLlm
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed interface LlmStatus {
    data object NotLoaded : LlmStatus
    data object Loading : LlmStatus
    data class Ready(val modelName: String) : LlmStatus
    data class Failed(val message: String) : LlmStatus
}

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
    private val mutex = Mutex()
    private var engine: LlmInference? = null

    private val _status = MutableStateFlow<LlmStatus>(LlmStatus.NotLoaded)
    val status: StateFlow<LlmStatus> = _status.asStateFlow()

    /** True when a model is loaded *or* can be loaded on demand (it is unloaded in the background to free RAM). */
    override val isReady: Boolean get() = engine != null || (modelFile.exists() && _status.value !is LlmStatus.Failed && _status.value !is LlmStatus.Loading)

    val hasModelFile: Boolean get() = modelFile.exists()

    /** Disk used by the imported model, for the storage overview. */
    val modelBytes: Long get() = if (modelFile.exists()) modelFile.length() else 0L

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
        _status.value = LlmStatus.NotLoaded
    }

    private suspend fun load(name: String = "On-device model") {
        _status.value = LlmStatus.Loading
        runCatching {
            withContext(Dispatchers.Default) {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(2048)
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
            llm.generateResponse("<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n")
        }
    }
}
