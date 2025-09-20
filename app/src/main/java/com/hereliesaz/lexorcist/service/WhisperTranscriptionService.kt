package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.whisper.asr.Whisper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WhisperTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService
) : TranscriptionService {

    private var whisper: Whisper? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    private val modelPath = "whisper-tiny.en.tflite"
    private val vocabPath = "filters_vocab_en.bin"

    init {
        serviceScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                whisper = Whisper(context).apply {
                    loadModelFromAssets(modelPath, vocabPath, false)
                }
                logService.addLog("Whisper model loaded successfully.")
            } catch (e: IOException) {
                _processingState.value = ProcessingState.Failure("Failed to initialize Whisper: ${e.message}")
                logService.addLog("Failed to initialize Whisper: ${e.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
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
            initialize()
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
