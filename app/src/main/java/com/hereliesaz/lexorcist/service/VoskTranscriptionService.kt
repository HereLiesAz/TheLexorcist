package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.DownloadState
import com.hereliesaz.lexorcist.model.LanguageModel
import com.hereliesaz.lexorcist.model.LogLevel
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.model.TranscriptionModels
import com.hereliesaz.lexorcist.utils.Result
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
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Collections
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService,
    private val settingsManager: SettingsManager
) : TranscriptionService {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var voskModel: Model? = null
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState

    private val downloadProgresses = Collections.synchronizedMap(mutableMapOf<String, MutableStateFlow<Float>>())

    fun getDownloadProgress(modelName: String): StateFlow<Float> {
        return downloadProgresses.getOrPut(modelName) { MutableStateFlow(0f) }.asStateFlow()
    }

    fun downloadModel(model: LanguageModel): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading)
        val result = downloadAndUnzipModel(model)
        if (result is Result.Success) {
            emit(DownloadState.Downloaded)
        } else if (result is Result.Error) {
            emit(DownloadState.Error(result.exception.message ?: "Unknown error"))
        }
    }

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
        val selectedLangCode = settingsManager.getTranscriptionLanguage()
        val model = TranscriptionModels.voskModels.find { it.code == selectedLangCode }
            ?: throw IOException("Selected language model '$selectedLangCode' not found.")

        val modelDir = File(context.filesDir, model.modelName)
        logService.addLog("Attempting to initialize model from: ${modelDir.absolutePath}")

        fun isValidModelDir(dir: File): Boolean {
            val amDir = File(dir, "am")
            val confDir = File(dir, "conf")
            val modelConfFile = File(confDir, "model.conf")
            val isValid = amDir.exists() && amDir.isDirectory &&
                    confDir.exists() && confDir.isDirectory &&
                    modelConfFile.exists() && modelConfFile.isFile
            if (!isValid) logService.addLog("Model directory is not valid: ${dir.absolutePath}", LogLevel.WARNING)
            return isValid
        }

        // The model might be in a subdirectory inside the model directory.
        // e.g. vosk-model-small-en-us-0.15/vosk-model-small-en-us-0.15
        fun findValidModelPath(baseDir: File): String? {
            if (isValidModelDir(baseDir)) {
                return baseDir.absolutePath
            }
            val subDirs = baseDir.listFiles { file -> file.isDirectory }
            return subDirs?.firstOrNull { isValidModelDir(it) }?.absolutePath
        }

        var validModelPath = findValidModelPath(modelDir)
        if (validModelPath != null) {
            logService.addLog("Vosk model found and valid at $validModelPath")
            return validModelPath
        }

        // If model not found, try to download it automatically
        logService.addLog("Vosk model for language '$selectedLangCode' not found. Attempting to download...", LogLevel.INFO)
        val downloadResult = downloadAndUnzipModel(model)

        if (downloadResult is Result.Success) {
            logService.addLog("Model for '$selectedLangCode' downloaded successfully. Re-validating path.")
            // After download, re-check for the valid model path
            validModelPath = findValidModelPath(modelDir)
            if (validModelPath != null) {
                return validModelPath
            }
        }

        val errorMsg = "Vosk model for language '$selectedLangCode' could not be found or downloaded."
        logService.addLog(errorMsg, LogLevel.ERROR)
        throw IOException(errorMsg)
    }

    private suspend fun downloadAndUnzipModel(model: LanguageModel): Result<Unit> {
        val modelDir = File(context.filesDir, model.modelName)
        val progressFlow = downloadProgresses.getOrPut(model.modelName) { MutableStateFlow(0f) }

        return withContext(Dispatchers.IO) {
            try {
                if (modelDir.exists() && !modelDir.deleteRecursively()) {
                    logService.addLog("Failed to delete existing model directory: ${modelDir.absolutePath}", LogLevel.WARNING)
                }
                if (!modelDir.mkdirs()) {
                    throw IOException("Failed to create model directory: ${modelDir.absolutePath}")
                }

                val zipFile = File(modelDir, "${model.modelName}.zip")
                logService.addLog("Downloading Vosk model from ${model.modelUrl} to ${zipFile.absolutePath}")
                progressFlow.value = 0f

                URL(model.modelUrl).openStream().use { inputStreamFromUrl ->
                    FileOutputStream(zipFile).use { fileOutputStream ->
                        val connection = URL(model.modelUrl).openConnection()
                        connection.connect()
                        val fileLength = connection.contentLength
                        val buffer = ByteArray(8192)
                        var len1: Int
                        var total: Long = 0
                        while (inputStreamFromUrl.read(buffer).also { len1 = it } > 0) {
                            total += len1
                            if (fileLength > 0) {
                                progressFlow.value = (total.toFloat() / fileLength.toFloat())
                            }
                            fileOutputStream.write(buffer, 0, len1)
                        }
                    }
                }
                logService.addLog("Vosk model ZIP downloaded.")

                logService.addLog("Unzipping Vosk model...")
                ZipInputStream(zipFile.inputStream()).use { zis ->
                    var zipEntry = zis.nextEntry
                    while (zipEntry != null) {
                        val newFile = File(modelDir, zipEntry.name)
                        if (!newFile.canonicalPath.startsWith(modelDir.canonicalPath + File.separator)) {
                            throw SecurityException("Zip entry is outside of the target dir: ${zipEntry.name}")
                        }
                        if (zipEntry.isDirectory) {
                            if (!newFile.isDirectory && !newFile.mkdirs()) {
                                throw IOException("Failed to create directory $newFile")
                            }
                        } else {
                            val parent = newFile.parentFile
                            if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
                                throw IOException("Failed to create directory $parent")
                            }
                            FileOutputStream(newFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        zipEntry = zis.nextEntry
                    }
                }

                zipFile.delete()
                logService.addLog("Vosk model unzipped successfully.")
                progressFlow.value = 1.0f
                Result.Success(Unit)
            } catch (e: Exception) {
                logService.addLog("Error during model download/unzip: ${e.message}", LogLevel.ERROR)
                e.printStackTrace()
                // Clean up failed download
                if (modelDir.exists()) {
                    modelDir.deleteRecursively()
                }
                Result.Error(e)
            }
        }
    }

    private fun transcribeAudioStream(recognizer: Recognizer, inputStream: InputStream): String {
        try {
            val buffer = ByteArray(4096)
            var nbytes: Int
            while (inputStream.read(buffer).also { nbytes = it } > 0) {
                if (recognizer.acceptWaveForm(buffer, nbytes)) {
                    // Partial result logic can be added here if needed
                }
            }
            val finalResultJson = recognizer.finalResult
            val finalResult = Gson().fromJson(finalResultJson, FinalResult::class.java)
            return finalResult?.text ?: ""
        } finally {
            recognizer.close()
        }
    }

    private data class FinalResult(val text: String)
}
