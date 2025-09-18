package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
) : TranscriptionService {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var model: Model? = null
    private val _downloadProgress = MutableStateFlow<Float>(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    override suspend fun start(uri: Uri) {
        serviceScope.launch {
            _processingState.value = ProcessingState.InProgress(0f)
            logService.addLog("VoskService: Starting transcription for $uri")
            try {
                if (model == null) {
                    val modelPath = initializeVoskModel()
                    model = Model(modelPath) // This should now get the correct subdirectory path
                }

                val recognizer = Recognizer(model, 16000f)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    val errorMsg = "Failed to open audio stream from URI: $uri"
                    logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                    _processingState.value = ProcessingState.Failure(errorMsg)
                    return@launch
                }

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

    override suspend fun transcribeAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        _processingState.value = ProcessingState.InProgress(0.0f)
        logService.addLog("VoskService: transcribeAudio called for $uri")
        try {
            if (model == null) {
                logService.addLog("VoskService: Model not initialized. Initializing...")
                val modelPath = initializeVoskModel()
                model = Model(modelPath) // This should now get the correct subdirectory path
                logService.addLog("VoskService: Model initialized from path: $modelPath")
            }

            val recognizer = Recognizer(model, 16000f)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                val errorMsg = "Failed to open audio stream from URI: $uri for transcribeAudio"
                logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure(errorMsg)
                return@withContext Result.Error(IOException(errorMsg))
            }
            
            _processingState.value = ProcessingState.InProgress(0.5f)
            val resultText = transcribeInputStream(recognizer, inputStream)
            _processingState.value = ProcessingState.InProgress(1.0f)
            logService.addLog("VoskService: transcribeAudio completed for $uri. Result: $resultText")
            _processingState.value = ProcessingState.Completed(resultText)
            Result.Success(resultText)
        } catch (e: Exception) {
            val errorMsg = "transcribeAudio failed for $uri: ${e.message}"
            logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            Result.Error(e) // Propagate the original exception for the test to assert specific exceptions if needed.
        }
    }

    private suspend fun initializeVoskModel(): String {
        return withContext(serviceScope.coroutineContext) {
            val baseModelDir = File(context.filesDir, "vosk-model-assets") // Base directory for extraction
            if (!baseModelDir.exists()) {
                baseModelDir.mkdirs()
            }

            // Attempt to find an already extracted model directory
            val actualModelDir = findActualModelDirectory(baseModelDir)
            if (actualModelDir != null && actualModelDir.exists()) {
                logService.addLog("Vosk model found at ${actualModelDir.absolutePath}")
                return@withContext actualModelDir.absolutePath
            }
            
            // If not found or if the found directory is empty (e.g. partial extraction), download and unzip
            logService.addLog("Vosk model not found or invalid at ${baseModelDir.absolutePath}, attempting to download and unzip...")
            _processingState.value = ProcessingState.InProgress(0.0f)
            val downloadResult = downloadAndUnzipModel(baseModelDir) // Download to the base directory
            if (downloadResult is Result.Error) {
                val errorMsg = "Vosk model download failed: ${downloadResult.exception.message}"
                logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure(errorMsg)
                throw downloadResult.exception
            }
            logService.addLog("Vosk model downloaded and unzipped successfully to ${baseModelDir.absolutePath}")

            // After download, find the actual model directory again
            val newlyExtractedModelDir = findActualModelDirectory(baseModelDir)
            if (newlyExtractedModelDir != null && newlyExtractedModelDir.exists()) {
                logService.addLog("Using newly extracted Vosk model at ${newlyExtractedModelDir.absolutePath}")
                return@withContext newlyExtractedModelDir.absolutePath
            }

            val finalErrorMsg = "Vosk model directory not found after extraction in ${baseModelDir.absolutePath}"
            logService.addLog(finalErrorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            throw IOException(finalErrorMsg)
        }
    }

    // Helper to find the actual model directory (e.g., "vosk-model-small-en-us-0.15")
    private fun findActualModelDirectory(baseDir: File): File? {
        if (!baseDir.exists() || !baseDir.isDirectory) return null

        // Strategy 1: Look for a directory that seems like a Vosk model (e.g., contains an "am" subdirectory)
        baseDir.listFiles()?.forEach { file ->
            if (file.isDirectory && File(file, "am").exists() && File(file, "conf").exists()) {
                return file // Found a likely model directory
            }
        }
        // Strategy 2: If the model is extracted directly into baseDir (less common for Vosk archives but a fallback)
        if (File(baseDir, "am").exists() && File(baseDir, "conf").exists()){
             return baseDir
        }
        return null // Model directory not found with these strategies
    }

    private suspend fun downloadAndUnzipModel(destinationBaseDir: File): Result<Unit> {
        return withContext(serviceScope.coroutineContext) {
            try {
                val modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
                // Ensure the base directory exists
                if (!destinationBaseDir.exists()) {
                    destinationBaseDir.mkdirs()
                }
                val zipFile = File(destinationBaseDir, "vosk-model.zip")

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

                logService.addLog("Unzipping Vosk model into ${destinationBaseDir.absolutePath}...")
                _processingState.value = ProcessingState.InProgress(0.99f)

                val zipInputStream = java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile))
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val newFile = File(destinationBaseDir, zipEntry.name) // Extract relative to destinationBaseDir
                     // Security check for Zip Slip vulnerability
                    if (!newFile.canonicalPath.startsWith(destinationBaseDir.canonicalPath + File.separator)) {
                        zipInputStream.close()
                        zipFile.delete() // Clean up the downloaded zip
                        throw SecurityException("Zip entry tried to escape model directory: ${zipEntry.name}")
                    }
                    if (zipEntry.isDirectory) {
                        if (!newFile.exists()) newFile.mkdirs()
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
                logService.addLog("Vosk model unzipped successfully into ${destinationBaseDir.absolutePath}.")
                _downloadProgress.value = 1.0f
                Result.Success(Unit)
            } catch (e: Exception) {
                logService.addLog("Error during model download/unzip: ${e.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                Result.Error(e)
            }
        }
    }

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
            // Handle potentially null or empty finalResultJson
            if (finalResultJson.isNullOrEmpty()) {
                logService.addLog("Vosk final result JSON is null or empty.", com.hereliesaz.lexorcist.model.LogLevel.INFO)
                return "" // Or throw an exception, depending on desired behavior
            }
            val finalResult = Gson().fromJson(finalResultJson, FinalResult::class.java)
            return finalResult?.text ?: "" // Handle case where parsing might somehow yield null FinalResult or null text
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
