package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
// import com.hereliesaz.lexorcist.di.qualifiers.ApplicationScope // Removed qualifier import
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob // Import SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService
    // Removed @ApplicationScope private val externalScope: CoroutineScope
) : TranscriptionService {

    // Internal scope for this service's operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var model: Model? = null
    private val _downloadProgress = MutableStateFlow<Float>(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    override suspend fun start(uri: Uri) {
        serviceScope.launch { // Use internal serviceScope
            _processingState.value = ProcessingState.InProgress(0f) 
            logService.addLog("VoskService: Starting transcription for $uri")
            try {
                if (model == null) {
                    val modelPath = initializeVoskModel() // This is a suspend function
                    model = Model(modelPath)
                }

                val recognizer = Recognizer(model, 16000f) 
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    val errorMsg = "Failed to open audio stream from URI: $uri"
                    logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                    _processingState.value = ProcessingState.Failure(errorMsg)
                    return@launch
                }

                // transcribeInputStream is a blocking call, ensure it's called within a coroutine context
                // that allows blocking IO, which serviceScope (Dispatchers.IO) does.
                val resultText = transcribeInputStream(recognizer, inputStream)
                logService.addLog("VoskService: Transcription completed for $uri. Result: $resultText")
                _processingState.value = ProcessingState.Completed(resultText)
            } catch (e: Exception) {
                val errorMsg = "Transcription failed for $uri: ${e.message}"
                logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure(errorMsg)
            }
        }
    }

    override fun stop() {
        logService.addLog("VoskService: Stop called, but Vosk processes file at once.")
    }

    // transcribeAudio already uses withContext(Dispatchers.IO) which is appropriate.
    override suspend fun transcribeAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        _processingState.value = ProcessingState.InProgress(0.0f)
        logService.addLog("VoskService: transcribeAudio called for $uri")
        try {
            if (model == null) {
                logService.addLog("VoskService: Model not initialized. Initializing...")
                val modelPath = initializeVoskModel()
                model = Model(modelPath)
                logService.addLog("VoskService: Model initialized.")
            }

            val recognizer = Recognizer(model, 16000f)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.Error(IOException("Failed to open audio stream from URI: $uri"))

            _processingState.value = ProcessingState.InProgress(0.5f)

            val resultText = transcribeInputStream(recognizer, inputStream)

            _processingState.value = ProcessingState.InProgress(1.0f)
            logService.addLog("VoskService: transcribeAudio completed for $uri. Result: $resultText")
            _processingState.value = ProcessingState.Completed(resultText)
            Result.Success(resultText)
        } catch (e: java.io.FileNotFoundException) {
            val errorMsg = "File not found for URI: $uri"
            logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            Result.Error(java.io.IOException(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = "transcribeAudio failed for $uri: ${e.message}"
            logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            Result.Error(e)
        }
    }

    // initializeVoskModel is a suspend function and uses withContext(Dispatchers.IO)
    private suspend fun initializeVoskModel(): String {
        return withContext(serviceScope.coroutineContext) { // Can use serviceScope's context or Dispatchers.IO directly
            val modelDir = File(context.filesDir, "vosk-model")
            if (!modelDir.exists() || modelDir.listFiles()?.isEmpty() == true) { 
                logService.addLog("Vosk model not found or empty at ${modelDir.absolutePath}, attempting to download and unzip...")
                _processingState.value = ProcessingState.InProgress(0.0f) 
                val downloadResult = downloadAndUnzipModel(modelDir)
                if (downloadResult is Result.Error) {
                    logService.addLog("Vosk model download failed: ${downloadResult.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                     _processingState.value = ProcessingState.Failure("Model download failed: ${downloadResult.exception.message}")
                    throw downloadResult.exception 
                }
                logService.addLog("Vosk model downloaded and unzipped successfully to ${modelDir.absolutePath}")
            }
            modelDir.absolutePath 
        }
    }

    // downloadAndUnzipModel is a suspend function and uses withContext(Dispatchers.IO)
    private suspend fun downloadAndUnzipModel(modelDir: File): Result<Unit> {
        return withContext(serviceScope.coroutineContext) { // Can use serviceScope's context or Dispatchers.IO directly
            try {
                val modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
                
                if (!modelDir.exists()) {
                    modelDir.mkdirs()
                }
                val zipFile = File(modelDir, "vosk-model.zip")

                logService.addLog("Downloading Vosk model from $modelUrl to ${zipFile.absolutePath}")
                _downloadProgress.value = 0f

                val url = java.net.URL(modelUrl)
                val connection = url.openConnection()
                connection.connect()
                val fileLength = connection.contentLength
                val inputStream = connection.getInputStream()
                val fileOutputStream = java.io.FileOutputStream(zipFile)
                val buffer = ByteArray(4096) 
                var len1: Int
                var total: Long = 0
                while (inputStream.read(buffer).also { len1 = it } > 0) {
                    total += len1
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toFloat()
                        _downloadProgress.value = progress / 100f 
                        _processingState.value = ProcessingState.InProgress(progress / 100f) 
                    }
                    fileOutputStream.write(buffer, 0, len1)
                }
                fileOutputStream.close()
                inputStream.close()
                logService.addLog("Vosk model ZIP downloaded. Size: $total bytes.")

                logService.addLog("Unzipping Vosk model...")
                _processingState.value = ProcessingState.InProgress(0.99f) 

                val zipInputStream = java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile))
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val newFile = File(modelDir, zipEntry.name) 
                    if (!newFile.canonicalPath.startsWith(modelDir.canonicalPath)) {
                        throw SecurityException("Zip entry tried to escape model directory: ${zipEntry.name}")
                    }
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        val parent = newFile.parentFile
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs()
                        }
                        val entryOutputStream = java.io.FileOutputStream(newFile)
                        var len2: Int
                        while (zipInputStream.read(buffer).also { len2 = it } > 0) {
                            entryOutputStream.write(buffer, 0, len2)
                        }
                        entryOutputStream.close()
                    }
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
                zipInputStream.close()
                zipFile.delete() 
                logService.addLog("Vosk model unzipped successfully.")
                _downloadProgress.value = 1.0f 
                Result.Success(Unit)
            } catch (e: Exception) {
                logService.addLog("Error during model download/unzip: ${e.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                Result.Error(e)
            }
        }
    }

    // This is a blocking function, should be called from a coroutine on an IO dispatcher.
    private fun transcribeInputStream(recognizer: Recognizer, inputStream: InputStream): String {
        try {
            val buffer = ByteArray(4096) 
            var nbytes: Int
            while (inputStream.read(buffer).also { nbytes = it } > 0) { 
                if (recognizer.acceptWaveForm(buffer, nbytes)) {
                    // val partialJson = recognizer.partialResult
                    // logService.addLog("Vosk partial: $partialJson")
                }
            }
            val finalResultJson = recognizer.finalResult
            logService.addLog("Vosk final result JSON: $finalResultJson")
            val finalResult = Gson().fromJson(finalResultJson, FinalResult::class.java)
            return finalResult.text
        } finally {
            try {
                inputStream.close()
            } catch (ioe: IOException) {
                logService.addLog("Error closing audio input stream: ${ioe.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            }
            recognizer.close() 
        }
    }
}

private data class FinalResult(val text: String)
