package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.DownloadState
import com.hereliesaz.lexorcist.model.LanguageModel
import com.hereliesaz.lexorcist.model.LogLevel
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.model.TranscriptionModels
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.lexorcist.utils.VideoUtils
import com.hereliesaz.whisper.asr.Whisper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WhisperTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService,
    private val settingsManager: SettingsManager
) : TranscriptionService {

    private var whisper: Whisper? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    private val downloadProgresses = Collections.synchronizedMap(mutableMapOf<String, MutableStateFlow<Float>>())

    init {
        serviceScope.launch {
            try {
                initialize()
            } catch (e: IOException) {
                logService.addLog("Whisper initialization failed on startup: ${e.message}", LogLevel.WARNING)
            }
        }
    }

    private suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val selectedLangCode = settingsManager.getTranscriptionLanguage()
            val model = TranscriptionModels.whisperModels.find { it.code == selectedLangCode }
                ?: throw IOException("Selected Whisper language '$selectedLangCode' not found or supported.")

            val modelFile = File(context.filesDir, model.modelName)
            val vocabFile = File(context.filesDir, model.modelName.replace(".tflite", ".bin"))

            if (!modelFile.exists() || !vocabFile.exists()) {
                throw IOException("Whisper model or vocab file not found. Please download it from settings.")
            }

            val isMultilingual = model.code == "multi"

            try {
                whisper = Whisper(context).apply {
                    loadModel(modelFile.absolutePath, vocabFile.absolutePath, isMultilingual)
                }
                logService.addLog("Whisper model '${model.modelName}' loaded successfully.")
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Failure("Failed to initialize Whisper: ${e.message}")
                logService.addLog("Failed to initialize Whisper: ${e.message}", LogLevel.ERROR)
                throw IOException("Failed to load Whisper model", e)
            }
        }
    }

    fun getDownloadProgress(modelName: String): StateFlow<Float> {
        return downloadProgresses.getOrPut(modelName) { MutableStateFlow(0f) }.asStateFlow()
    }

    fun downloadModel(model: LanguageModel): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading)
        val modelFile = File(context.filesDir, model.modelName)
        val vocabFile = File(context.filesDir, model.modelName.replace(".tflite", ".bin"))
        val progressFlow = downloadProgresses.getOrPut(model.modelName) { MutableStateFlow(0f) }

        try {
            withContext(Dispatchers.IO) {
                // Download model
                downloadFile(URL(model.modelUrl), modelFile, progressFlow, 0.5f)
                // Download vocab
                model.vocabUrl?.let {
                    downloadFile(URL(it), vocabFile, progressFlow, 1.0f)
                }
            }
            progressFlow.value = 1.0f
            logService.addLog("Whisper model '${model.modelName}' and vocab downloaded successfully.")
            emit(DownloadState.Downloaded)
        } catch (e: Exception) {
            logService.addLog("Error downloading Whisper model '${model.modelName}': ${e.message}", LogLevel.ERROR)
            e.printStackTrace()
            modelFile.delete()
            vocabFile.delete()
            emit(DownloadState.Error("Download failed: ${e.message}"))
        }
    }

    private suspend fun downloadFile(url: URL, file: File, progress: MutableStateFlow<Float>, progressWeight: Float) {
        url.openStream().use { input ->
            FileOutputStream(file).use { output ->
                val connection = url.openConnection()
                connection.connect()
                val fileLength = connection.contentLength
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead: Long = 0
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (fileLength > 0) {
                        progress.value = (totalBytesRead.toFloat() / fileLength.toFloat()) * progressWeight
                    }
                }
            }
        }
    }


    override suspend fun start(uri: Uri) {
        // Not needed for this implementation
    }

    override fun stop() {
        whisper?.stop()
    }

    override suspend fun transcribeAudio(uri: Uri): Result<String> {
        if (whisper == null) {
            try {
                initialize()
            } catch (e: IOException) {
                return Result.Error(e)
            }
        }
        val whisperInstance = whisper ?: return Result.Error(IllegalStateException("Whisper not initialized"))

        return suspendCancellableCoroutine { continuation ->
            _processingState.value = ProcessingState.InProgress(0f)

            val audioFilePath = try {
                getFilePathFromUri(uri)
            } catch (e: IOException) {
                _processingState.value = ProcessingState.Failure("Failed to get file path from Uri: ${e.message}")
                continuation.resumeWithException(e)
                return@suspendCancellableCoroutine
            }

            whisperInstance.setListener(object : Whisper.WhisperListener {
                override fun onUpdateReceived(message: String?) {
                    if (message == Whisper.MSG_PROCESSING) {
                        _processingState.value = ProcessingState.InProgress(0.5f)
                    }
                }

                override fun onResultReceived(result: String?) {
                    _processingState.value = ProcessingState.Completed(result ?: "")
                    if (result != null) {
                        continuation.resume(Result.Success(result))
                    } else {
                        continuation.resume(Result.Success(""))
                    }
                }
            })

            whisperInstance.setFilePath(audioFilePath)
            whisperInstance.setAction(Whisper.Action.TRANSCRIBE)
            whisperInstance.start()

            continuation.invokeOnCancellation {
                whisperInstance.stop()
            }
        }
    }

    override suspend fun transcribeVideo(uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val audioFilePath = VideoUtils.extractAudio(context, uri)
                val audioFile = File(audioFilePath)
                val audioUri = Uri.fromFile(audioFile)
                val result = transcribeAudio(audioUri)
                audioFile.delete() // Clean up the temporary audio file
                result
            } catch (e: IOException) {
                Result.Error(e)
            }
        }
    }

    private fun getFilePathFromUri(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream for URI: $uri")
        val tempFile = File.createTempFile("audio", ".wav", context.cacheDir)
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile.absolutePath
    }
}
