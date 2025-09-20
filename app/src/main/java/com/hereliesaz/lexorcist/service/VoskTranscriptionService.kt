package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log.e
import com.google.gson.Gson
import com.hereliesaz.lexorcist.model.LogLevel // Import LogLevel
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
import org.vosk.Model // Vosk Model
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
    private var voskModel: Model? = null // Explicitly Vosk Model
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
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val resultText = transcribeAudioStream(recognizer, inputStream)
                    logService.addLog("VoskService: Transcription completed for $uri. Result: $resultText")
                    _processingState.value = ProcessingState.Completed(resultText)
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
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                _processingState.value = ProcessingState.InProgress(0.5f)
                val resultText = transcribeAudioStream(recognizer, inputStream)
                _processingState.value = ProcessingState.InProgress(1.0f)
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

            logService.addLog("Vosk model not found or invalid at ${baseModelDir.absolutePath}. Cleaning up...", LogLevel.INFO)
            if (baseModelDir.exists() && !baseModelDir.deleteRecursively()) {
                logService.addLog("Failed to delete existing model directory: ${baseModelDir.absolutePath}", LogLevel.WARNING)
            }
            if (!baseModelDir.exists() && !baseModelDir.mkdirs()) {
                val errorMsg = "Failed to create base model directory: ${baseModelDir.absolutePath}"
                logService.addLog(errorMsg, LogLevel.ERROR)
                throw IOException(errorMsg)
            }

            _processingState.value = ProcessingState.InProgress(0.0f)
            val downloadResult = downloadAndUnzipModel(baseModelDir)

            if (downloadResult is Result.Error) {
                logService.addLog("Vosk model download/unzip failed: ${downloadResult.exception.message}", LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure("Model download/unzip failed: ${downloadResult.exception.message}")
                if (baseModelDir.exists() && !baseModelDir.deleteRecursively()) {
                    logService.addLog("Failed to delete model directory after failed download: ${baseModelDir.absolutePath}", LogLevel.WARNING)
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
                val subDirs = subFiles.filter { it.isDirectory }
                if (subDirs.size == 1) {
                    val potentialModelDir = subDirs[0]
                    if (isValidModelDir(potentialModelDir)) {
                        logService.addLog("Vosk model found in subdirectory: ${potentialModelDir.absolutePath}")
                        return@withContext potentialModelDir.absolutePath
                    } else {
                        logService.addLog("Subdirectory ${potentialModelDir.absolutePath} is not a valid Vosk model.", LogLevel.WARNING)
                    }
                } else if (subDirs.isEmpty()) {
                    logService.addLog("No subdirectories found in ${baseModelDir.absolutePath} to check for model.", LogLevel.WARNING)
                } else {
                    logService.addLog("Multiple subdirectories found. Cannot determine model path.", LogLevel.WARNING)
                }
            } else {
                logService.addLog("Could not list files in ${baseModelDir.absolutePath}.", LogLevel.WARNING)
            }

            val errorMsg = "Failed to locate a valid Vosk model in ${baseModelDir.absolutePath} after download."
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            if (baseModelDir.exists() && !baseModelDir.deleteRecursively()) {
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

    private suspend fun downloadAndUnzipModel(modelDir: File): Result<Unit> {
        return withContext(Dispatchers.IO) { // Explicitly Dispatchers.IO
            try {
                val modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
                if (!modelDir.exists()) modelDir.mkdirs()
                val zipFile = File(modelDir, "vosk-model.zip")

                logService.addLog("Downloading Vosk model from $modelUrl to ${zipFile.absolutePath}")
                _downloadProgress.value = 0f

                URL(modelUrl).openStream().use { inputStreamFromUrl ->
                    FileOutputStream(zipFile).use { fileOutputStream ->
                        val connection = URL(modelUrl).openConnection()
                        connection.connect()
                        val fileLength = connection.contentLength
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
                    }
                }
                logService.addLog("Vosk model ZIP downloaded.")

                logService.addLog("Unzipping Vosk model...")
                _processingState.value = ProcessingState.InProgress(0.99f) // Near end of download phase

                val entryNames = mutableListOf<String>()
                ZipInputStream(zipFile.inputStream()).use { zis ->
                    generateSequence { zis.nextEntry }.forEach { entry ->
                        entryNames.add(entry.name)
                        zis.closeEntry()
                    }
                }

                var commonPrefix = ""
                if (entryNames.isNotEmpty()) {
                    val normalizedEntryNames = entryNames.map { it.replace('\\', '/') }
                        val firstEntryParts = normalizedEntryNames.first().split('/')
                        if (firstEntryParts.size > 1 && firstEntryParts.first().isNotEmpty()) {
                            val potentialPrefix = firstEntryParts.first() + "/"
                            if (normalizedEntryNames.all { it.startsWith(potentialPrefix) }) {
                                commonPrefix = potentialPrefix
                            }
                        }
                    }

                    ZipInputStream(zipFile.inputStream()).use { zis ->
                        var zipEntry = zis.nextEntry
                        val buffer = ByteArray(4096)
                        while (zipEntry != null) {
                            var entryNameString = zipEntry.name.replace('\\', '/')
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
                                if (!newFile.exists()) newFile.mkdirs()
                            } else {
                                val parent = newFile.parentFile
                                if (parent != null && !parent.exists()) parent.mkdirs()
                                FileOutputStream(newFile).use { fos ->
                                    var len2: Int
                                    while (zis.read(buffer).also { len2 = it } > 0) {
                                        fos.write(buffer, 0, len2)
                                    }
                                }
                            }}
                            zis.closeEntry()
                            zipEntry = zis.nextEntry
                        }

                    zipFile.delete()
                    logService.addLog("Vosk model unzipped successfully.")
                    _downloadProgress.value = 1.0f
                    Result.Success(Unit)
                } catch (e: Exception) {
                    logService.addLog("Error during model download/unzip: ${e.message}", LogLevel.ERROR)
                    e.printStackTrace()
                    Result.Error(e)
                }
            }
        }

        // This is a blocking function, should be called from a coroutine on an IO dispatcher.
        private fun transcribeAudioStream(recognizer: Recognizer, inputStream: InputStream): String {
            // Removed `inputStream.use` from here as it's handled by the caller or this function's finally block.
            // The caller (transcribeAudio, start) now uses `use` which will close the stream.
            try {
                val buffer = ByteArray(4096)
                var nbytes = 0 // Initialized
                while (inputStream.read(buffer).also { nbytes = it } > 0) {
                    if (recognizer.acceptWaveForm(buffer, nbytes)) {
                        // val partialJson = recognizer.partialResult
                        // logService.addLog("Vosk partial: $partialJson")
                    }
                }
                val finalResultJson = recognizer.finalResult
                logService.addLog("Vosk final result JSON: $finalResultJson")
                val finalResult = Gson().fromJson(finalResultJson, FinalResult::class.java)
                return finalResult?.text ?: ""
            } finally {
                // inputStream is managed by the caller's `use` block.
                // Recognizer should be closed if it's single-use, or managed by the class lifecycle.
                // For now, closing it here as per original logic.
                recognizer.close()
            }
        }

        private data class FinalResult(val text: String)
    }
