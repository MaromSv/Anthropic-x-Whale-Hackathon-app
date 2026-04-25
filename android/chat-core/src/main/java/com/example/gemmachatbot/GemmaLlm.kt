package com.example.gemmachatbot

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

enum class GemmaBackend(val id: Int) { CPU(0), GPU(1), NPU(2) }

data class GemmaLoadOptions(
    val modelPath: String,
    val backend: GemmaBackend = GemmaBackend.GPU,
    val systemInstruction: String? = "You are a helpful, concise assistant.",
    val topK: Int = 64,
    val topP: Double = 0.95,
    val temperature: Double = 1.0,
)

class ModelNotLoadedException : IllegalStateException("Model not loaded. Call load() first.")
class ModelFileMissingException(path: String) : IllegalStateException("Model file not found at: $path")

/**
 * Thin wrapper around the LiteRT-LM Engine/Conversation lifecycle.
 * All public functions are safe to call from any thread; the engine
 * itself is thread-confined per LiteRT-LM contract, so callers should
 * funnel calls through a single dispatcher (the ViewModel uses Dispatchers.IO).
 */
class GemmaLlm(private val appContext: Context) {

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    val isLoaded: Boolean get() = engine != null

    fun load(opts: GemmaLoadOptions) {
        if (!File(opts.modelPath).exists()) throw ModelFileMissingException(opts.modelPath)

        val backend = when (opts.backend) {
            GemmaBackend.CPU -> Backend.CPU()
            GemmaBackend.GPU -> Backend.GPU()
            GemmaBackend.NPU -> Backend.NPU(
                nativeLibraryDir = appContext.applicationInfo.nativeLibraryDir,
            )
        }

        unload()

        val newEngine = Engine(
            EngineConfig(
                modelPath = opts.modelPath,
                backend = backend,
                // Gemma 3n is multi-modal; route vision through the GPU when
                // we have OpenCL, otherwise fall back to CPU.
                visionBackend = if (opts.backend == GemmaBackend.GPU) Backend.GPU() else Backend.CPU(),
                cacheDir = appContext.cacheDir.path,
            )
        )
        newEngine.initialize()

        val convConfig = ConversationConfig(
            systemInstruction = opts.systemInstruction?.let { Contents.of(it) },
            samplerConfig = SamplerConfig(
                topK = opts.topK,
                topP = opts.topP,
                temperature = opts.temperature,
            ),
        )

        conversation = newEngine.createConversation(convConfig)
        engine = newEngine
    }

    fun unload() {
        conversation?.close(); conversation = null
        engine?.close(); engine = null
    }

    fun generate(prompt: String): String {
        val c = conversation ?: throw ModelNotLoadedException()
        return c.sendMessage(prompt).toString()
    }

    /** Emits each streamed token as it arrives. Completes when generation finishes. */
    fun generateStreaming(prompt: String): Flow<String> = flow {
        val c = conversation ?: throw ModelNotLoadedException()
        c.sendMessageAsync(prompt).collect { msg -> emit(msg.toString()) }
    }

    /**
     * Multimodal streaming: sends a message containing zero or more image
     * file paths plus a text prompt. Image paths must be readable by the app
     * (e.g. files copied into [Context.cacheDir] or [Context.filesDir]).
     */
    fun generateStreamingMultimodal(
        prompt: String,
        imagePaths: List<String>,
    ): Flow<String> = flow {
        val c = conversation ?: throw ModelNotLoadedException()
        val parts = buildList<Content> {
            imagePaths.forEach { add(Content.ImageFile(it)) }
            add(Content.Text(prompt))
        }
        c.sendMessageAsync(Contents.of(parts)).collect { msg -> emit(msg.toString()) }
    }
}
