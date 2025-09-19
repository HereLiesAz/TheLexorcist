package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
// import android.util.Log.e // Not used directly, logService is used
import com.google.gson.Gson
import com.hereliesaz.lexorcist.model.LogLevel // Added import
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
import org.vosk.Model // Keep Vosk model
import org.vosk.Recognizer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream // Added for clarity
import java.io.FileOutputStream // Added for clarity
import java.net.URL // Added for clarity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService
) : TranscriptionService {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var voskModel: Model? = null // Renamed to avoid any potential future conflicts, and explicitly Vosk
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    override suspend fun start(uri: Uri) {
        serviceScope.launch {
            _processingState.value = ProcessingState.InProgress(0f)
            logService.addLog("VoskService: Starting transcription for $uri")
            try {
                if (voskModel == null) {
                    val modelPath = initializeVoskModel()
                    voskModel = Model(modelPath)
                }

                val recognizer = Recognizer(voskModel, 16000f)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    val errorMsg = "Failed to open audio stream from URI: $uri"
                    logService.addLog(errorMsg, LogLevel.ERROR)
                    _processingState.value = ProcessingState.Failure(errorMsg)
                    return@launch
                }
                val resultText = transcribeInputStreamInternal(recognizer, inputStream) // Renamed for clarity
                logService.addLog("VoskService: Transcription completed for $uri. Result: $resultText")
                _processingState.value = ProcessingState.Completed(resultText)
            } catch (e: Exception) {
                val errorMsg = "Transcription failed for $uri: ${e.message}"
                logService.addLog(errorMsg, LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure(errorMsg)
                e.printStackTrace() // For more detailed logs during debugging
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
            if (voskModel == null) {
                logService.addLog("VoskService: Model not initialized. Initializing...")
                val modelPath = initializeVoskModel()
                voskModel = Model(modelPath)
                logService.addLog("VoskService: Model initialized at path: $modelPath")
            }

            val recognizer = Recognizer(voskModel, 16000f)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.Error(IOException("Failed to open audio stream from URI: $uri"))

            _processingState.value = ProcessingState.InProgress(0.5f)
            val resultText = transcribeInputStreamInternal(recognizer, inputStream) // Renamed for clarity
            _processingState.value = ProcessingState.InProgress(1.0f)
            logService.addLog("VoskService: transcribeAudio completed for $uri. Result: $resultText")
            _processingState.value = ProcessingState.Completed(resultText)
            Result.Success(resultText)
        } catch (e: java.io.FileNotFoundException) {
            val errorMsg = "File not found for URI: $uri"
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            Result.Error(IOException(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = "transcribeAudio failed for $uri: ${e.message}"
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            e.printStackTrace() // For more detailed logs
            Result.Error(e)
        }
    }

    private suspend fun initializeVoskModel(): String {
        return withContext(serviceScope.coroutineContext) {
            val baseModelDir = File(context.filesDir, "vosk-model")

            fun isValidModelDir(dir: File): Boolean {
                val amDir = File(dir, "am")
                val confDir = File(dir, "conf")
                val modelConfFile = File(confDir, "model.conf")
                return amDir.exists() && amDir.isDirectory &&
                       confDir.exists() && confDir.isDirectory &&
                       modelConfFile.exists() && modelConfFile.isFile
            }

            if (isValidModelDir(baseModelDir)) {
                logService.addLog("Vosk model found and valid at ${baseModelDir.absolutePath}")
                return@withContext baseModelDir.absolutePath
            }

            logService.addLog("Vosk model not found or invalid at ${baseModelDir.absolutePath}. Cleaning up and attempting to download and unzip...")
            if (baseModelDir.exists()) {
                logService.addLog("Deleting existing model directory: ${baseModelDir.absolutePath}")
                if (!baseModelDir.deleteRecursively()) {
                    logService.addLog("Failed to delete existing model directory: ${baseModelDir.absolutePath}", LogLevel.WARNING)
                }
            }
            if (!baseModelDir.exists()) {
                if (!baseModelDir.mkdirs()) {
                    val errorMsg = "Failed to create base model directory: ${baseModelDir.absolutePath}"
                    logService.addLog(errorMsg, LogLevel.ERROR)
                    throw IOException(errorMsg)
                }
            }

            _processingState.value = ProcessingState.InProgress(0.0f)
            val downloadResult = downloadAndUnzipModel(baseModelDir)

            if (downloadResult is Result.Error) {
                logService.addLog("Vosk model download/unzip failed: ${downloadResult.exception.message}", LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure("Model download/unzip failed: ${downloadResult.exception.message}")
                if (baseModelDir.exists()) {
                    if (!baseModelDir.deleteRecursively()) {
                         logService.addLog("Failed to delete model directory after failed download: ${baseModelDir.absolutePath}", LogLevel.WARNING)
                    }
                }
                throw downloadResult.exception
            }
            logService.addLog("Vosk model downloaded and unzipped successfully to ${baseModelDir.absolutePath}")

            if (isValidModelDir(baseModelDir)) {
                logService.addLog("Vosk model successfully prepared at ${baseModelDir.absolutePath}")
                return@withContext baseModelDir.absolutePath
            }

            logService.addLog("Vosk model not directly in ${baseModelDir.absolutePath}. Checking subdirectories...")
            val subFiles = baseModelDir.listFiles()
            if (subFiles != null) {
                val subDirs = subFiles.filter { it.isDirectory }.toList()
                if (subDirs.size == 1) {
                    val potentialModelDir = subDirs[0]
                    if (isValidModelDir(potentialModelDir)) {
                        logService.addLog("Vosk model found in subdirectory: ${potentialModelDir.absolutePath}")
                        return@withContext potentialModelDir.absolutePath
                    } else {
                        logService.addLog("Subdirectory ${potentialModelDir.absolutePath} is not a valid Vosk model. Contents: ${potentialModelDir.listFiles()?.joinToString { it.name } ?: "N/A"}", LogLevel.WARNING)
                    }
                } else if (subDirs.isEmpty()) {
                    logService.addLog("No subdirectories found in ${baseModelDir.absolutePath} to check for model. Contents: ${baseModelDir.listFiles()?.joinToString { it.name } ?: "N/A"}", LogLevel.WARNING)
                } else {
                    logService.addLog("Multiple subdirectories found in ${baseModelDir.absolutePath}: ${subDirs.joinToString { it.name }}. Cannot determine model path automatically.", LogLevel.WARNING)
                }
            } else {
                 logService.addLog("Could not list files in ${baseModelDir.absolutePath}.", LogLevel.WARNING)
            }

            val errorMsg = "Failed to locate a valid Vosk model structure in ${baseModelDir.absolutePath} (or its direct subdirectory) after download and unzip."
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            if (baseModelDir.exists()) {
                 if(!baseModelDir.deleteRecursively()) {
                    logService.addLog("Failed to delete model directory after final failure: ${baseModelDir.absolutePath}", LogLevel.WARNING)
                 }
            }
            throw IOException(errorMsg)
        }
    }

    private suspend fun downloadAndUnzipModel(modelDir: File): Result<Unit> {
        return withContext(serviceScope.coroutineContext) {
            try {
                val modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
                
                if (!modelDir.exists()) {
                    modelDir.mkdirs()
                }
                val zipFile = File(modelDir, "vosk-model.zip")

                logService.addLog("Downloading Vosk model from $modelUrl to ${zipFile.absolutePath}")
                _downloadProgress.value = 0f

                val url = URL(modelUrl)
                val connection = url.openConnection()
                connection.connect()
                val fileLength = connection.contentLength
                val inputStreamFromUrl = connection.getInputStream() // Renamed to avoid confusion
                val fileOutputStream = FileOutputStream(zipFile)
                val buffer = ByteArray(4096)
                var len1: Int
                var total: Long = 0
                while (inputStreamFromUrl.read(buffer).also { len1 = it } > 0) {
                    total += len1
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toFloat()
                        _downloadProgress.value = progress / 100f
                        _processingState.value = ProcessingState.InProgress(progress / 100f)
                    }
                    fileOutputStream.write(buffer, 0, len1)
                }
                fileOutputStream.close()
                inputStreamFromUrl.close()
                logService.addLog("Vosk model ZIP downloaded. Size: $total bytes.")

                logService.addLog("Unzipping Vosk model...")
                _processingState.value = ProcessingState.InProgress(0.99f)

                // First pass: get all entry names to find common prefix
                val entryNames = mutableListOf<String>()
                ZipInputStream(zipFile.inputStream()).use { zis -> // Use .inputStream() for safety
                    var zipEntry = zis.nextEntry
                    while (zipEntry != null) {
                        entryNames.add(zipEntry.name)
                        zis.closeEntry()
                        zipEntry = zis.nextEntry
                    }
                }
                
                var commonPrefix = ""
                if (entryNames.isNotEmpty()) {
                    val normalizedEntryNames = entryNames.map { it.replace('\', '/') } // Corrected replace
                    val firstEntryParts = normalizedEntryNames.first().split('/')
                    if (firstEntryParts.size > 1 && firstEntryParts.first().isNotEmpty()) {
                        commonPrefix = firstEntryParts.first() + "/"
                        if (!normalizedEntryNames.all { it.startsWith(commonPrefix) }) {
                            commonPrefix = ""
                        }
                    }
                }
                
                // Second pass: actual extraction
                ZipInputStream(zipFile.inputStream()).use { zis -> // Use .inputStream() for safety
                    var zipEntry = zis.nextEntry
                    while (zipEntry != null) {
                        var entryNameString = zipEntry.name.replace('\', '/') // Corrected replace
                        if (commonPrefix.isNotEmpty() && entryNameString.startsWith(commonPrefix)) {
                            entryNameString = entryNameString.substring(commonPrefix.length)
                        }

                        if (entryNameString.isEmpty()) {
                            zis.closeEntry()
                            zipEntry = zis.nextEntry
                            continue
                        }

                        val newFile = File(modelDir, entryNameString)
                        if (!newFile.canonicalPath.startsWith(modelDir.canonicalPath)) {
                            zis.closeEntry()
                            throw SecurityException("Zip entry tried to escape model directory: ${zipEntry.name}")
                        }
                        if (zipEntry.isDirectory) {
                            if (!newFile.exists()) {
                               newFile.mkdirs()
                            }
                        } else {
                            val parent = newFile.parentFile
                            if (parent != null && !parent.exists()) {
                                parent.mkdirs()
                            }
                            FileOutputStream(newFile).use { fos ->
                                var len2: Int
                                while (zis.read(buffer).also { len2 = it } > 0) {
                                    fos.write(buffer, 0, len2)
                                }
                            }
                        }
                        zis.closeEntry()
                        zipEntry = zis.nextEntry
                    }
                }
                zipFile.delete()
                logService.addLog("Vosk model unzipped successfully.")
                _downloadProgress.value = 1.0f
                Result.Success(Unit)
            } catch (e: Exception) {
                logService.addLog("Error during model download/unzip: ${e.message}", LogLevel.ERROR)
                e.printStackTrace() // For more detailed logs
                Result.Error(e)
            }
        }
    }

    // Renamed to transcribeInputStreamInternal to avoid potential naming conflicts if ever refactoring
    private fun transcribeInputStreamInternal(recognizer: Recognizer, inputStream: InputStream): String {
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
            return finalResult?.text ?: "" // Handle null finalResult or text
        } finally {
            try {
                inputStream.close()
            } catch (ioe: IOException) {
                logService.addLog("Error closing audio input stream: ${ioe.message}", LogLevel.ERROR)
            }
            recognizer.close()
        }
    }

    private data class FinalResult(val text: String)
}
