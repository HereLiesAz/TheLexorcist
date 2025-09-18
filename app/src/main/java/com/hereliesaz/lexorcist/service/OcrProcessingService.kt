package com.hereliesaz.lexorcist.service

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.DataParser
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.LogLevel // Assuming this is your custom LogLevel
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.ExifUtils
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.net.toUri
// Removed: import java.util.logging.Level.WARNING

@Singleton
class OcrProcessingService
    @Inject
    constructor(
        private val evidenceRepository: EvidenceRepository,
        private val settingsManager: SettingsManager,
        private val scriptRunner: ScriptRunner,
        private val logService: LogService,
        private val storageService: com.hereliesaz.lexorcist.data.StorageService,
    ) {
        private suspend fun recognizeTextFromUri(
            context: Context,
            uri: Uri,
        ): String {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                return suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { recognizer.close() }
                    val image: InputImage
                    var inputStream: InputStream? = null
                    try {
                        when (uri.scheme) {
                            "content" -> {
                                inputStream = context.contentResolver.openInputStream(uri)
                            }
                            "file", null -> {
                                uri.path?.let {
                                    val file = File(it)
                                    if (file.exists()) {
                                        inputStream = FileInputStream(file)
                                    } else {
                                        throw java.io.FileNotFoundException("File not found at path: $it")
                                    }
                                } ?: throw java.io.FileNotFoundException("URI path is null")
                            }
                            else -> {
                                throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
                            }
                        }

                        if (inputStream == null) {
                            throw java.io.FileNotFoundException("Failed to open InputStream for URI: $uri")
                        }
                        
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        
                        if (bitmap == null) {
                            continuation.resumeWithException(RuntimeException("Failed to decode bitmap from URI: $uri"))
                            return@suspendCancellableCoroutine
                        }
                        image = InputImage.fromBitmap(bitmap, 0) // Using 0 rotation for now
                    } catch (e: Exception) {
                        Log.e("OcrProcessingService", "Error creating InputImage from URI: $uri", e)
                        continuation.resumeWithException(e)
                        return@suspendCancellableCoroutine
                    } finally {
                        inputStream?.close()
                    }

                    recognizer
                        .process(image)
                        .addOnSuccessListener { visionText ->
                            continuation.resume(visionText.text)
                        }.addOnFailureListener { e ->
                            Log.e("OcrProcessingService", "Text recognition failed for URI: $uri", e)
                            continuation.resumeWithException(e)
                        }
                }
            } finally {
                recognizer.close()
            }
        }

        suspend fun processImageFrame(
            uri: Uri,
            context: Context,
            caseId: Int,
            spreadsheetId: String,
            parentVideoId: String?,
        ): Evidence? {
            logService.addLog("Processing video frame: $uri")
            val ocrText =
                try {
                    recognizeTextFromUri(context, uri)
                } catch (e: Exception) {
                    logService.addLog("Error recognizing text from frame: ${e.message}", LogLevel.ERROR)
                    Log.e("OcrProcessingService", "Failed to recognize text from image frame.", e)
                    "Error recognizing text from image frame: ${e.message}"
                }
            logService.addLog("Frame recognition complete. Found ${ocrText.length} characters.")

            val entities = DataParser.tagData(ocrText)
            val documentDate =
                ExifUtils.getExifDate(context, uri)
                    ?: DataParser.parseDates(ocrText).firstOrNull()
                    ?: System.currentTimeMillis()

            var newEvidence =
                Evidence(
                    id = 0,
                    caseId = caseId.toLong(),
                    spreadsheetId = spreadsheetId,
                    type = "ocr_image_from_video",
                    content = ocrText,
                    formattedContent = "```\n$ocrText\n```",
                    mediaUri = uri.toString(),
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = documentDate,
                    allegationId = null,
                    category = "Video Frame OCR",
                    tags = listOf("ocr", "video_frame"),
                    commentary = null,
                    parentVideoId = parentVideoId,
                    entities = entities,
                )

            val script = settingsManager.getScript()
            if (script.isNotBlank()) {
                val scriptResult = scriptRunner.runScript(script, newEvidence)
                when (scriptResult) {
                    is Result.Loading -> {
                        logService.addLog("Script for frame is loading...")
                    }
                    is Result.Success -> {
                        newEvidence = newEvidence.copy(tags = newEvidence.tags + scriptResult.data)
                    }
                    is Result.Error -> {
                        logService.addLog("Script error for frame: ${scriptResult.exception.message}", LogLevel.ERROR)
                        Log.e("OcrProcessingService", "Script error for $uri: ${scriptResult.exception.message}", scriptResult.exception)
                    }
                    is Result.UserRecoverableError -> {
                        logService.addLog("Script error for frame: ${scriptResult.exception.message}", LogLevel.INFO) // Changed from WARNING to INFO
                        Log.e(
                            "OcrProcessingService",
                            "User recoverable script error for $uri: ${scriptResult.exception.message}",
                            scriptResult.exception,
                        )
                    }
                }
            }

            logService.addLog("Saving frame evidence...")
            val savedEvidence = evidenceRepository.addEvidence(newEvidence)
            logService.addLog("Frame evidence saved with ID: ${savedEvidence?.id}")
            return savedEvidence
        }

        suspend fun processImage(
            uri: Uri,
            context: Context,
            caseId: Long,
            spreadsheetId: String,
            onProgress: (ProcessingState) -> Unit
        ): Pair<Evidence?, String?> {
            var statusMessage: String // Initializer removed
            logService.addLog("Starting image processing...")
            onProgress(ProcessingState.InProgress(0.0f))

            logService.addLog("Uploading image to raw evidence folder...")
            onProgress(ProcessingState.InProgress(0.25f))
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val uploadResult = storageService.uploadFile(spreadsheetId, uri, mimeType)

            when (uploadResult) {
                is Result.Error -> {
                    statusMessage = "Error uploading image: ${uploadResult.exception.message}"
                    logService.addLog(statusMessage, LogLevel.ERROR)
                    Log.e("OcrProcessingService", "Failed to upload image.", uploadResult.exception)
                    onProgress(ProcessingState.Failure(statusMessage))
                    return Pair(null, statusMessage)
                }
                is Result.UserRecoverableError -> { 
                    statusMessage = "User recoverable error during image upload: ${uploadResult.exception.message}"
                    logService.addLog(statusMessage, LogLevel.INFO) // Changed from WARNING to INFO
                    Log.w("OcrProcessingService", "User recoverable error during image upload.", uploadResult.exception)
                    onProgress(ProcessingState.Failure(statusMessage))
                    return Pair(null, statusMessage)
                }
                is Result.Loading -> {
                    logService.addLog("Image upload in progress...")
                    onProgress(ProcessingState.InProgress(0.25f)) 
                    // statusMessage assignment removed as it was not used
                    onProgress(ProcessingState.Failure("Image upload did not complete in time.")) 
                    return Pair(null, "Image upload did not complete in time.")
                }
                is Result.Success -> {
                    val newUri = uploadResult.data.toUri()
                    logService.addLog("Image uploaded to: $newUri")

                    val ocrText =
                        try {
                            logService.addLog("Recognizing text from image...")
                            onProgress(ProcessingState.InProgress(0.50f))
                            recognizeTextFromUri(context, newUri)
                        } catch (e: Exception) {
                            statusMessage = "Error recognizing text from image: ${e.message}"
                            logService.addLog(statusMessage, LogLevel.ERROR)
                            Log.e("OcrProcessingService", "Failed to recognize text from image.", e)
                            onProgress(ProcessingState.Failure(statusMessage))
                            return Pair(null, statusMessage) 
                        }

                    logService.addLog("Text recognition complete. Found ${ocrText.length} characters.")
                    val entities = DataParser.tagData(ocrText)
                    logService.addLog("Parsed ${entities.size} entities.")
                    val documentDate =
                        ExifUtils.getExifDate(context, newUri)
                            ?: DataParser.parseDates(ocrText).firstOrNull()
                            ?: System.currentTimeMillis()
                    logService.addLog("Determined document date: $documentDate")

                    var newEvidence =
                        Evidence(
                            id = 0,
                            caseId = caseId,
                            spreadsheetId = spreadsheetId,
                            type = "image",
                            content = ocrText,
                            formattedContent = "```\n$ocrText\n```",
                            mediaUri = newUri.toString(),
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = newUri.toString(),
                            documentDate = documentDate,
                            allegationId = null,
                            category = "Image OCR",
                            tags = listOf("ocr", "image"),
                            commentary = null,
                            parentVideoId = null,
                            entities = entities,
                        )

                    val script = settingsManager.getScript()
                    if (script.isNotBlank()) {
                        logService.addLog("Running script...")
                        onProgress(ProcessingState.InProgress(0.75f))
                        val scriptResult = scriptRunner.runScript(script, newEvidence)
                        when (scriptResult) {
                            is Result.Loading -> {
                                logService.addLog("Script for image is loading...")
                            }
                            is Result.Success -> {
                                newEvidence = newEvidence.copy(tags = newEvidence.tags + scriptResult.data)
                                logService.addLog("Script finished. Added tags: ${scriptResult.data.joinToString(", ")}")
                            }
                            is Result.Error -> {
                                statusMessage = "Script error: ${scriptResult.exception.message}"
                                logService.addLog(statusMessage, LogLevel.ERROR)
                                Log.e("OcrProcessingService", "Script error for $uri: $statusMessage", scriptResult.exception)
                                // Note: statusMessage is set but not immediately returned. It will be used if no further error occurs before final return.
                            }
                            is Result.UserRecoverableError -> {
                                statusMessage = "User recoverable script error: ${scriptResult.exception.message}"
                                logService.addLog(statusMessage, LogLevel.INFO) // Changed from WARNING to INFO
                                Log.w("OcrProcessingService", "User recoverable script error for $uri: $statusMessage", scriptResult.exception)
                                // Note: statusMessage is set but not immediately returned. 
                            }
                        }
                    }

                    logService.addLog("Saving evidence...")
                    onProgress(ProcessingState.InProgress(0.90f))
                    val savedEvidence = evidenceRepository.addEvidence(newEvidence)
                    logService.addLog("Evidence saved with ID: ${savedEvidence?.id}")
                    onProgress(ProcessingState.InProgress(1.0f))
                    statusMessage = if (savedEvidence != null) "Image processed and evidence saved successfully." else "Image processed but failed to save evidence."
                    onProgress(ProcessingState.Completed(statusMessage))
                    return Pair(savedEvidence, statusMessage)
                } 
            } 
        }
    }
