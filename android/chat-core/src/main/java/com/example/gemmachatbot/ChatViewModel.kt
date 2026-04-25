package com.example.gemmachatbot

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class LoadStatus { Idle, Loading, Ready, Error }

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val imagePaths: List<String> = emptyList(),
) {
    enum class Role { User, Assistant }
}

data class ChatUiState(
    val status: LoadStatus = LoadStatus.Idle,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val pendingImages: List<String> = emptyList(),
    val generating: Boolean = false,
    val modelPath: String = "",
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val gemma = GemmaLlm(app)

    private val _state = MutableStateFlow(
        ChatUiState(modelPath = defaultModelPath(app))
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null

    fun setInput(value: String) = _state.update { it.copy(input = value) }
    fun setModelPath(value: String) = _state.update { it.copy(modelPath = value) }

    /**
     * Copies the picked image into the app's cache dir and stores the absolute
     * path in pendingImages. LiteRT-LM needs a real filesystem path, not a
     * content:// Uri.
     */
    fun attachImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val ext = app.contentResolver.getType(uri)
                ?.substringAfterLast('/')
                ?.lowercase()
                ?.let { if (it in setOf("jpeg", "jpg", "png", "webp")) it else "jpg" }
                ?: "jpg"
            val dest = File(app.cacheDir, "img_${System.currentTimeMillis()}.$ext")
            app.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: return@launch
            _state.update { it.copy(pendingImages = it.pendingImages + dest.absolutePath) }
        }
    }

    fun removePendingImage(path: String) {
        _state.update { it.copy(pendingImages = it.pendingImages - path) }
        runCatching { File(path).delete() }
    }

    /**
     * Allocates an empty cache file for the camera to write into and returns
     * a content:// Uri pointing at it (via our FileProvider). Call before
     * launching the TakePicture contract; on success, call [confirmCameraImage]
     * with the same path.
     */
    fun createCameraImageFile(): Pair<String, Uri> {
        val app = getApplication<Application>()
        val file = File(app.cacheDir, "img_${System.currentTimeMillis()}.jpg")
        file.createNewFile()
        val uri = androidx.core.content.FileProvider.getUriForFile(
            app, "${app.packageName}.fileprovider", file
        )
        return file.absolutePath to uri
    }

    fun confirmCameraImage(path: String, success: Boolean) {
        if (success) {
            _state.update { it.copy(pendingImages = it.pendingImages + path) }
        } else {
            runCatching { File(path).delete() }
        }
    }

    fun loadModel() {
        if (_state.value.status == LoadStatus.Loading) return
        viewModelScope.launch {
            _state.update { it.copy(status = LoadStatus.Loading, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    gemma.load(
                        GemmaLoadOptions(
                            modelPath = _state.value.modelPath,
                            backend = GemmaBackend.GPU,
                        )
                    )
                }
                _state.update { it.copy(status = LoadStatus.Ready) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(status = LoadStatus.Error, error = t.message ?: t.toString())
                }
            }
        }
    }

    fun send() {
        val s = _state.value
        val prompt = s.input.trim()
        if ((prompt.isEmpty() && s.pendingImages.isEmpty()) || s.generating || s.status != LoadStatus.Ready) return

        val images = s.pendingImages
        val userMsg = ChatMessage(
            id = System.nanoTime(),
            role = ChatMessage.Role.User,
            text = prompt,
            imagePaths = images,
        )
        val asstMsg = ChatMessage(System.nanoTime() + 1, ChatMessage.Role.Assistant, "")
        _state.update {
            it.copy(
                input = "",
                pendingImages = emptyList(),
                generating = true,
                messages = it.messages + userMsg + asstMsg,
            )
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val flow = if (images.isEmpty()) {
                    gemma.generateStreaming(prompt)
                } else {
                    // litertlm requires a non-empty prompt alongside images.
                    val effectivePrompt = prompt.ifEmpty { "Describe this image." }
                    gemma.generateStreamingMultimodal(effectivePrompt, images)
                }
                flow.collect { token ->
                    _state.update { st ->
                        st.copy(messages = st.messages.map { m ->
                            if (m.id == asstMsg.id) m.copy(text = m.text + token) else m
                        })
                    }
                }
            } catch (t: Throwable) {
                _state.update { st ->
                    st.copy(messages = st.messages.map { m ->
                        if (m.id == asstMsg.id) m.copy(text = "Error: ${t.message ?: t}") else m
                    })
                }
            } finally {
                _state.update { it.copy(generating = false) }
            }
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        gemma.unload()
        super.onCleared()
    }

    companion object {
        /** Default model location: app's external files dir. Push with adb. */
        fun defaultModelPath(app: Application): String =
            File(
                app.getExternalFilesDir(null) ?: app.filesDir,
                "gemma-3n-E2B-it-int4.litertlm",
            ).absolutePath
    }
}
