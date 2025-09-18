package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService,
    private val externalScope: CoroutineScope
) : TranscriptionService {

    private var model: Model? = null
    private val _downloadProgress = MutableStateFlow<Float>(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    override suspend fun start(uri: Uri) {
        externalScope.launch(Dispatchers.IO) {
            _processingState.value = ProcessingState.InProgress(0f)
            try {
                if (model == null) {
                    val modelPath = initializeVoskModel()
                    model = Model(modelPath)
                }

                val recognizer = Recognizer(model, 16000f)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    logService.addLog("Failed to open audio stream", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                    _processingState.value = ProcessingState.Failure("Failed to open audio stream")
                    return@launch
                }

                val result = transcribeInputStream(recognizer, inputStream)
                _processingState.value = ProcessingState.Completed(result)
            } catch (e: Exception) {
                logService.addLog("Transcription failed: ${e.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure("Transcription failed: ${e.message}")
            }
        }
    }

    override fun stop() {
        // Not implemented for Vosk, as it processes the whole file at once.
    }

    private suspend fun initializeVoskModel(): String {
        return withContext(Dispatchers.IO) {
            val modelDir = File(context.filesDir, "vosk-model")
            if (!modelDir.exists()) {
                logService.addLog("Vosk model not found, downloading...")
                val downloadResult = downloadAndUnzipModel()
                if (downloadResult is Result.Error) {
                    throw downloadResult.exception
                }
            }
            modelDir.absolutePath
        }
    }

    private suspend fun downloadAndUnzipModel(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
                val modelDir = File(context.filesDir, "vosk-model")
                if (!modelDir.exists()) {
                    modelDir.mkdirs()
                }
                val zipFile = File(modelDir, "vosk-model.zip")
                val url = java.net.URL(modelUrl)
                val connection = url.openConnection()
                connection.connect()
                val fileLength = connection.contentLength
                val inputStream = connection.getInputStream()
                val fileOutputStream = java.io.FileOutputStream(zipFile)
                val buffer = ByteArray(1024)
                var len1: Int
                var total: Long = 0
                while (inputStream.read(buffer).also { len1 = it } > 0) {
                    total += len1
                    if (fileLength > 0) {
                        _downloadProgress.value = (total * 100 / fileLength).toFloat()
                    }
                    fileOutputStream.write(buffer, 0, len1)
                }
                fileOutputStream.close()
                inputStream.close()

                _downloadProgress.value = 100f // Unzipping started
                val zipInputStream = java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile))
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val newFile = File(modelDir, zipEntry.name)
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        val parent = newFile.parentFile
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs()
                        }
                        val fileOutputStream2 = java.io.FileOutputStream(newFile)
                        var len2: Int
                        while (zipInputStream.read(buffer).also { len2 = it } > 0) {
                            fileOutputStream2.write(buffer, 0, len2)
                        }
                        fileOutputStream2.close()
                    }
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
                zipInputStream.close()
                zipFile.delete()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    private fun transcribeInputStream(recognizer: Recognizer, inputStream: InputStream): String {
        val buffer = ByteArray(4096)
        var nbytes: Int
        while (inputStream.read(buffer).also { nbytes = it } >= 0) {
            recognizer.acceptWaveForm(buffer, nbytes)
        }
        val finalResult = Gson().fromJson(recognizer.finalResult, FinalResult::class.java)
        return finalResult.text
    }
}

private data class PartialResult(val partial: String)
private data class FinalResult(val text: String)
