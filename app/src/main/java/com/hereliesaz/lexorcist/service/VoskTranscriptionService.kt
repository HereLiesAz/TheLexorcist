\
package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log.e
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
                logService.addLog("VoskService: Model initialized at path: $modelPath")
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
        return withContext(serviceScope.coroutineContext) {
            val baseModelDir = File(context.filesDir, "vosk-model")

            // Helper function to check if a directory looks like a valid Vosk model directory
            fun isValidModelDir(dir: File): Boolean {
                val amDir = File(dir, "am")
                // Standard Vosk models have specific subdirectories like 'am', 'conf', 'graph'.
                // 'conf/model.conf' or 'ivector/final.ie' are good indicators.
                // For small models, 'am' and 'conf' are typical.
                val confDir = File(dir, "conf")
                val modelConfFile = File(confDir, "model.conf")
                // More robust check: presence of key directories/files
                // val rescoreDir = File(dir, "rescore") // Optional, but if present, indicates structure
                // val rnnDir = File(dir, "rnn") // Optional

                // Check for core components. Modify this if your model structure differs.
                val hasCoreComponents = amDir.exists() && amDir.isDirectory &&
                                        confDir.exists() && confDir.isDirectory &&
                                        modelConfFile.exists() && modelConfFile.isFile

                // You might also check for a graph directory or other specific files based on your model type
                // val graphDir = File(dir, "graph")
                // val hclgFile = File(graphDir, "HCLG.fst")
                // return hasCoreComponents && graphDir.exists() && graphDir.isDirectory && hclgFile.exists()
                                        
                return hasCoreComponents
            }

            // 1. Check if the baseModelDir itself is a valid model
            if (isValidModelDir(baseModelDir)) {
                logService.addLog("Vosk model found and valid at ${baseModelDir.absolutePath}")
                return@withContext baseModelDir.absolutePath
            }

            // 2. If not valid or doesn't exist, try to clean up and download/unzip
            logService.addLog("Vosk model not found or invalid at ${baseModelDir.absolutePath}. Cleaning up and attempting to download and unzip...")
            if (baseModelDir.exists()) {
                logService.addLog("Deleting existing model directory: ${baseModelDir.absolutePath}")
                if (!baseModelDir.deleteRecursively()) {
                    logService.addLog("Failed to delete existing model directory: ${baseModelDir.absolutePath}", com.hereliesaz.lexorcist.model.LogLevel.WARNING)
                    // If deletion fails, it might cause issues, but we proceed with caution
                }
            }
            // Ensure base directory exists or can be created
            if (!baseModelDir.exists()) {
                if (!baseModelDir.mkdirs()) {
                    val errorMsg = "Failed to create base model directory: ${baseModelDir.absolutePath}"
                    logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                    throw IOException(errorMsg)
                }
            }


            _processingState.value = ProcessingState.InProgress(0.0f)
            val downloadResult = downloadAndUnzipModel(baseModelDir) // downloadAndUnzipModel unzips into baseModelDir

            if (downloadResult is Result.Error) {
                logService.addLog("Vosk model download/unzip failed: ${downloadResult.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure("Model download/unzip failed: ${downloadResult.exception.message}")
                // Clean up if download failed to avoid issues on next attempt
                if (baseModelDir.exists()) {
                    if (!baseModelDir.deleteRecursively()) {
                         logService.addLog("Failed to delete model directory after failed download: ${baseModelDir.absolutePath}", com.hereliesaz.lexorcist.model.LogLevel.WARNING)
                    }
                }
                throw downloadResult.exception
            }
            logService.addLog("Vosk model downloaded and unzipped successfully to ${baseModelDir.absolutePath}")

            // 3. After unzipping, re-check baseModelDir
            if (isValidModelDir(baseModelDir)) {
                logService.addLog("Vosk model successfully prepared at ${baseModelDir.absolutePath}")
                return@withContext baseModelDir.absolutePath
            }

            // 4. If baseModelDir is still not a valid model, check for a single subdirectory
            //    that might contain the actual model files (common with zips).
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
                        logService.addLog("Subdirectory ${potentialModelDir.absolutePath} is not a valid Vosk model. Contents: ${potentialModelDir.listFiles()?.joinToString { it.name } ?: "N/A"}", com.hereliesaz.lexorcist.model.LogLevel.WARNING)
                    }
                } else if (subDirs.isEmpty()) {
                    logService.addLog("No subdirectories found in ${baseModelDir.absolutePath} to check for model. Contents: ${baseModelDir.listFiles()?.joinToString { it.name } ?: "N/A"}", com.hereliesaz.lexorcist.model.LogLevel.WARNING)
                } else {
                    logService.addLog("Multiple subdirectories found in ${baseModelDir.absolutePath}: ${subDirs.joinToString { it.name }}. Cannot determine model path automatically.", com.hereliesaz.lexorcist.model.LogLevel.WARNING)
                }
            } else {
                 logService.addLog("Could not list files in ${baseModelDir.absolutePath}.", com.hereliesaz.lexorcist.model.LogLevel.WARNING)
            }


            // 5. If model still not found, throw an error.
            val errorMsg = "Failed to locate a valid Vosk model structure in ${baseModelDir.absolutePath} (or its direct subdirectory) after download and unzip. Please ensure the zip file directly contains model files or has them in one subdirectory."
            logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            // Clean up to prevent issues on next launch by removing the potentially corrupt/incomplete model directory
            if (baseModelDir.exists()) {
                 if(!baseModelDir.deleteRecursively()) {
                    logService.addLog("Failed to delete model directory after final failure: ${baseModelDir.absolutePath}", com.hereliesaz.lexorcist.model.LogLevel.WARNING)
                 }
            }
            throw IOException(errorMsg)
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
                // Find the common root directory in the zip file
                val entryNames = mutableListOf<String>()
                while(zipEntry != null) {
                    entryNames.add(zipEntry.name)
                    zipInputStream.closeEntry() // Important: close entry before reading next
                    zipEntry = zipInputStream.nextEntry
                }
                zipInputStream.close() // Close and reopen to reset
                
                var commonPrefix = ""
                if (entryNames.isNotEmpty()) {
                    // Normalize entry names to use '/'
                    val normalizedEntryNames = entryNames.map { it.replace("\\\\", "/") }
                    val firstEntryParts = normalizedEntryNames.first().split('/')
                    if (firstEntryParts.size > 1 && firstEntryParts.first().isNotEmpty()) { // if the first entry is in a directory
                        commonPrefix = firstEntryParts.first() + "/"
                        var allHavePrefix = true
                        for (name in normalizedEntryNames) {
                            if (!name.startsWith(commonPrefix)) {
                                allHavePrefix = false
                                break
                            }
                        }
                        if (!allHavePrefix) {
                            commonPrefix = "" // Not all entries share this prefix
                        }
                    }
                }
                
                // Re-open the stream for actual extraction
                val freshZipInputStream = java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile))
                zipEntry = freshZipInputStream.nextEntry

                while (zipEntry != null) {
                    var entryName = zipEntry.name.replace('\\\\', '/') // Corrected escape. Normalize entry name
                    // Strip the common prefix
                    if (commonPrefix.isNotEmpty() && entryName.startsWith(commonPrefix)) {
                        entryName = entryName.substring(commonPrefix.length)
                    }

                    if (entryName.isEmpty()) { // Skip the root folder itself if it's an entry or an empty string after stripping
                        freshZipInputStream.closeEntry()
                        zipEntry = freshZipInputStream.nextEntry
                        continue
                    }

                    val newFile = File(modelDir, entryName) 
                    if (!newFile.canonicalPath.startsWith(modelDir.canonicalPath)) {
                         freshZipInputStream.closeEntry() // Close entry before throwing
                        throw SecurityException("Zip entry tried to escape model directory: ${zipEntry.name}")
                    }
                    if (zipEntry.isDirectory) {
                        if (!newFile.exists()) { // Only create if it doesn't exist to avoid issues with existing dirs
                           newFile.mkdirs()
                        }
                    } else {
                        val parent = newFile.parentFile
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs()
                        }
                        val entryOutputStream = java.io.FileOutputStream(newFile)
                        var len2: Int = 0 // Initialized len2
                        while (freshZipInputStream.read(buffer).also { len2 = it } > 0) {
                            entryOutputStream.write(buffer, 0, len2)
                        }
                        entryOutputStream.close()
                    }
                    freshZipInputStream.closeEntry()
                    zipEntry = freshZipInputStream.nextEntry
                }
                freshZipInputStream.close()
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
    fun transcribeInputStream(recognizer: Recognizer, inputStream: InputStream): String {
        try {
            val buffer = ByteArray(4096) 
            var nbytes: Int = 0 // Initialized nbytes
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
    private data class FinalResult(val text: String)
}
