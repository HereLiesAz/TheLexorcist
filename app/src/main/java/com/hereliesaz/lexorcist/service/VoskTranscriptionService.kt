package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.hereliesaz.lexorcist.model.LogLevel
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
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService
) : TranscriptionService {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var voskModel: Model? = null
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    // 1. Define FinalResult as a private data class member
    private data class FinalResult(val text: String)

    override suspend fun start(uri: Uri) {
        serviceScope.launch {
            _processingState.value = ProcessingState.InProgress(0f)
            logService.addLog("VoskService: Starting transcription for $uri")
            try {
                if (voskModel == null) {
                    val modelPath = initializeVoskModel()
                    voskModel = Model(modelPath)
                }
                // Make sure recognizer is created fresh or reset if model is reinitialized.
                val recognizer = Recognizer(voskModel, 16000f) 
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val resultText = transcribeAudioStream(recognizer, inputStream)
                    logService.addLog("VoskService: Transcription completed for $uri. Result: $resultText")
                    _processingState.value = ProcessingState.Completed(resultText)
                    recognizer.close() // Close recognizer after use
                } ?: run {
                    val errorMsg = "Failed to open audio stream from URI: $uri"
                    logService.addLog(errorMsg, LogLevel.ERROR)
                    _processingState.value = ProcessingState.Failure(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Transcription failed for $uri: ${e.message}"
                logService.addLog(errorMsg, LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure(errorMsg)
                e.printStackTrace()
            }
        }
    }

    override fun stop() {
        logService.addLog("VoskService: Stop called. Vosk processes file at once, but resources can be released if held.")
        // Model is an instance variable; consider if it needs closing on service stop/destroy.
        // voskModel?.close() // If model holds significant native resources and service is stopping permanently.
    }

    override suspend fun transcribeAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        _processingState.value = ProcessingState.InProgress(0.0f)
        logService.addLog("VoskService: transcribeAudio called for $uri")
        var recognizer: Recognizer? = null // Declare here to ensure it's closed in finally
        try {
            if (voskModel == null) {
                logService.addLog("VoskService: Model not initialized. Initializing...")
                val modelPath = initializeVoskModel()
                voskModel = Model(modelPath) // Model initialization
                logService.addLog("VoskService: Model initialized at path: $modelPath")
            }
            recognizer = Recognizer(voskModel, 16000f) // Create Recognizer after model is ensured
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                _processingState.value = ProcessingState.InProgress(0.5f) // Example progress
                val resultText = transcribeAudioStream(recognizer, inputStream)
                _processingState.value = ProcessingState.InProgress(1.0f) // Example progress
                logService.addLog("VoskService: transcribeAudio completed for $uri. Result: $resultText")
                _processingState.value = ProcessingState.Completed(resultText)
                Result.Success(resultText)
            } ?: Result.Error(IOException("Failed to open audio stream from URI: $uri"))
        } catch (e: java.io.FileNotFoundException) {
            val errorMsg = "File not found for URI: $uri"
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            Result.Error(IOException(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = "transcribeAudio failed for $uri: ${e.message}"
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            e.printStackTrace()
            Result.Error(e)
        } finally {
            recognizer?.close() // Ensure recognizer is closed
        }
    }

    private suspend fun initializeVoskModel(): String {
        // First, try to load from assets
        copyModelFromAssets()?.let {
            logService.addLog("Vosk model initialized from assets.")
            return it
        }

        // If not in assets, proceed with download logic
        logService.addLog("Vosk model not found in assets, attempting to download.", LogLevel.INFO)
        return withContext(serviceScope.coroutineContext) { // Use serviceScope's context for IO
            val baseModelDir = File(context.filesDir, "vosk-model")

            fun isValidModelDir(dir: File): Boolean {
                val amDir = File(dir, "am")
                val confDir = File(dir, "conf")
                val modelConfFile = File(confDir, "model.conf")
                // More robust check: also check for some expected files within am and conf if known
                return amDir.exists() && amDir.isDirectory &&
                        confDir.exists() && confDir.isDirectory &&
                        modelConfFile.exists() && modelConfFile.isFile
            }

            if (isValidModelDir(baseModelDir)) {
                logService.addLog("Vosk model found and valid at ${baseModelDir.absolutePath}")
                return@withContext baseModelDir.absolutePath
            }

            logService.addLog("Vosk model not found or invalid at ${baseModelDir.absolutePath}. Cleaning up and re-downloading...", LogLevel.INFO)
            if (baseModelDir.exists()) {
                if (!baseModelDir.deleteRecursively()) {
                    logService.addLog("Failed to delete existing model directory: ${baseModelDir.absolutePath}", LogLevel.WARNING)
                    // Continue, try to create anyway if deletion failed
                }
            }
            if (!baseModelDir.exists()) { // Check again in case deleteRecursively worked partially or it didn't exist
                 if (!baseModelDir.mkdirs()) {
                    val errorMsg = "Failed to create base model directory: ${baseModelDir.absolutePath}"
                    logService.addLog(errorMsg, LogLevel.ERROR)
                    throw IOException(errorMsg)
                }
            }


            _processingState.value = ProcessingState.InProgress(0.0f) // Start progress for download
            val downloadResult = downloadAndUnzipModel(baseModelDir)

            if (downloadResult is Result.Error) {
                logService.addLog("Vosk model download/unzip failed: ${downloadResult.exception.message}", LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure("Model download/unzip failed: ${downloadResult.exception.message}")
                // Clean up partially downloaded/extracted files
                if (baseModelDir.exists() && !baseModelDir.deleteRecursively()) {
                     logService.addLog("Failed to delete model directory after failed download: ${baseModelDir.absolutePath}", LogLevel.WARNING)
                }
                throw downloadResult.exception
            }
            logService.addLog("Vosk model downloaded and unzipped successfully to ${baseModelDir.absolutePath}")

            // After download, check again, including potential subdirectories from ZIP structure
            if (isValidModelDir(baseModelDir)) {
                logService.addLog("Vosk model successfully prepared at ${baseModelDir.absolutePath}")
                return@withContext baseModelDir.absolutePath
            }

            // Check common case: model files are inside a single subdirectory after unzipping
            logService.addLog("Vosk model not directly in ${baseModelDir.absolutePath}. Checking subdirectories...")
            val subFiles = baseModelDir.listFiles()
            if (subFiles != null) {
                val subDirs = subFiles.filter { it.isDirectory }
                if (subDirs.size == 1) { // Exactly one subdirectory
                    val potentialModelDir = subDirs[0]
                    if (isValidModelDir(potentialModelDir)) {
                        logService.addLog("Vosk model found in subdirectory: ${potentialModelDir.absolutePath}")
                        return@withContext potentialModelDir.absolutePath
                    } else {
                         logService.addLog("Subdirectory ${potentialModelDir.absolutePath} is not a valid Vosk model.", LogLevel.WARNING)
                    }
                } else if (subDirs.isEmpty()){
                    logService.addLog("No subdirectories found in ${baseModelDir.absolutePath} to check for model.", LogLevel.WARNING)
                } else {
                     logService.addLog("Multiple subdirectories found. Cannot determine model path automatically.", LogLevel.WARNING)
                }
            } else {
                 logService.addLog("Could not list files in ${baseModelDir.absolutePath}.", LogLevel.WARNING)
            }
            
            val errorMsg = "Failed to locate a valid Vosk model in ${baseModelDir.absolutePath} or its direct subdirectories after download."
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            if (baseModelDir.exists() && !baseModelDir.deleteRecursively()) { // Cleanup on final failure
                logService.addLog("Failed to delete model directory after final failure: ${baseModelDir.absolutePath}", LogLevel.WARNING)
            }
            throw IOException(errorMsg)
        }
    }

    private fun copyModelFromAssets(): String? {
        val assetModelPath = "vosk-model-en-us"
        val destDir = File(context.cacheDir, "vosk-model")
        try {
            val assetFiles = context.assets.list(assetModelPath)
            if (assetFiles == null || assetFiles.isEmpty()) {
                logService.addLog("Vosk model not found in assets.", LogLevel.INFO)
                return null
            }

            if (destDir.exists()) destDir.deleteRecursively()
            destDir.mkdirs()

            assetFiles.forEach { fileName ->
                val assetFile = "$assetModelPath/$fileName"
                val destFile = File(destDir, fileName)
                context.assets.open(assetFile).use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            logService.addLog("Successfully copied Vosk model from assets to ${destDir.absolutePath}")
            return destDir.absolutePath
        } catch (e: IOException) {
            logService.addLog("Failed to copy Vosk model from assets: ${e.message}", LogLevel.ERROR)
            // If copying fails, clean up the destination directory
            if (destDir.exists()) {
                destDir.deleteRecursively()
            }
            return null
        }
    }

    // 3. Implement a robust ZIP extraction logic
    private suspend fun downloadAndUnzipModel(modelDir: File): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
                // Ensure modelDir exists, create if not. (already done in initializeVoskModel, but good for safety)
                if (!modelDir.exists() && !modelDir.mkdirs()) {
                     throw IOException("Failed to create model directory: ${modelDir.absolutePath}")
                }
                val zipFile = File(modelDir, "vosk-model.zip")

                logService.addLog("Downloading Vosk model from $modelUrl to ${zipFile.absolutePath}")
                _downloadProgress.value = 0f

                // Download
                URL(modelUrl).openStream().use { inputStreamFromUrl ->
                    FileOutputStream(zipFile).use { fileOutputStream ->
                        val connection = URL(modelUrl).openConnection()
                        connection.connect() // Ensure connection to get content length
                        val fileLength = connection.contentLength
                        val buffer = ByteArray(8192) // Adjusted buffer size
                        var len1: Int
                        var total: Long = 0
                        while (inputStreamFromUrl.read(buffer).also { len1 = it } != -1) { // Check for -1
                            total += len1
                            if (fileLength > 0) { // Check fileLength > 0 to avoid division by zero
                                val progress = (total * 100 / fileLength).toFloat()
                                _downloadProgress.value = progress / 100f // Normalize to 0.0-1.0
                                _processingState.value = ProcessingState.InProgress(progress / 100f)
                            }
                            fileOutputStream.write(buffer, 0, len1)
                        }
                    }
                }
                logService.addLog("Vosk model ZIP downloaded: ${zipFile.absolutePath}")
                _processingState.value = ProcessingState.InProgress(0.99f) // Indicate download near complete

                // Unzip
                logService.addLog("Unzipping Vosk model from ${zipFile.absolutePath} to ${modelDir.absolutePath}")
                ZipInputStream(zipFile.inputStream()).use { zis ->
                    val buffer = ByteArray(8192)
                    var zipEntry = zis.nextEntry
                    while (zipEntry != null) {
                        // Normalize entry name and create the file path relative to modelDir
                        val entryName = zipEntry.name.replace('\\', '/');
                        val newFile = File(modelDir, entryName);

                        // Security check: Prevent Zip Slip
                        if (!newFile.canonicalPath.startsWith(modelDir.canonicalPath + File.separator)) {
                            zis.closeEntry() // Close current entry before throwing
                            throw SecurityException("Zip entry is outside of the target dir: ${zipEntry.name}")
                        }

                        if (zipEntry.isDirectory) {
                            if (!newFile.exists()) {
                                newFile.mkdirs()
                            }
                        } else {
                            // Create parent directory if it doesn't exist
                            val parentDir = newFile.parentFile
                            if (parentDir != null && !parentDir.exists()) {
                                parentDir.mkdirs()
                            }
                            // Write file
                            FileOutputStream(newFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                        zipEntry = zis.nextEntry
                    }
                }
                // Delete the zip file after successful extraction
                if (!zipFile.delete()) {
                    logService.addLog("Failed to delete zip file: ${zipFile.absolutePath}", LogLevel.WARNING)
                }
                logService.addLog("Vosk model unzipped successfully into ${modelDir.absolutePath}")
                _downloadProgress.value = 1.0f // Download and unzip complete
                Result.Success(Unit)
            } catch (e: Exception) {
                logService.addLog("Error during Vosk model download/unzip: ${e.message}", LogLevel.ERROR)
                e.printStackTrace()
                // Return a specific error type if possible, or wrap the exception
                Result.Error(IOException("Failed to download or unzip Vosk model", e))
            }
        }
    }

    // 2. Define transcribeAudioStream as a private member function
    private fun transcribeAudioStream(recognizer: Recognizer, inputStream: InputStream): String {
        // This function is blocking and should be called from a Dispatchers.IO context.
        // The inputStream is managed by the caller's `use` block.
        try {
            val buffer = ByteArray(4096) // Standard buffer size for audio
            var nbytes: Int
            while (inputStream.read(buffer).also { nbytes = it } > 0) {
                // Pass data to the recognizer
                if (recognizer.acceptWaveForm(buffer, nbytes)) {
                    // Optional: process partial results if needed
                    // val partialJson = recognizer.partialResult
                    // logService.addLog("Vosk partial: $partialJson", LogLevel.DEBUG)
                } else {
                    // Optional: process intermediate results
                    // val partialJson = recognizer.partialResult
                    // logService.addLog("Vosk intermediate: $partialJson", LogLevel.DEBUG)
                }
            }
            // Get the final result after all data has been processed
            val finalResultJson = recognizer.finalResult
            logService.addLog("Vosk final result JSON: $finalResultJson", LogLevel.DEBUG) // Log the raw JSON

            // Parse the JSON using Gson
            val finalResult = Gson().fromJson(finalResultJson, FinalResult::class.java)
            return finalResult?.text ?: "" // Return the transcribed text or empty string if null/parsing fails
        } finally {
            // The Recognizer itself should be closed by the calling method (start/transcribeAudio)
            // if it's single-use for that transcription.
            // If the Recognizer is instance-level and reused, its lifecycle is managed elsewhere.
            // For Vosk, a Recognizer is typically created per audio stream and should be closed.
            // Caller (start and transcribeAudio) will now handle closing the recognizer.
        }
    }
}
