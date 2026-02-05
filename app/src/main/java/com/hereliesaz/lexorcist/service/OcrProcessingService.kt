package com.hereliesaz.lexorcist.service

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.data.ActiveScriptRepository
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.ScriptRepository
import com.hereliesaz.lexorcist.data.ScriptStateRepository
import com.hereliesaz.lexorcist.model.LogLevel
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.DataParser
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

/**
 * Service responsible for Optical Character Recognition (OCR) on images.
 *
 * This service orchestrates the pipeline of:
 * 1. Extracting text from an image URI using Google MLKit.
 * 2. Creating an [Evidence] object with the extracted text and metadata.
 * 3. Saving the evidence to the local database/spreadsheet.
 * 4. Automatically running any "active" user scripts against the new evidence.
 */
@Singleton
class OcrProcessingService
@Inject
constructor(
    private val evidenceRepository: EvidenceRepository,
    private val scriptRepository: ScriptRepository,
    private val activeScriptRepository: ActiveScriptRepository,
    private val scriptStateRepository: ScriptStateRepository,
    private val scriptRunner: ScriptRunner,
    private val logService: LogService,
    private val storageService: com.hereliesaz.lexorcist.data.StorageService,
) {
    /**
     * Performs OCR on an image located at the given URI.
     *
     * @param context The application context.
     * @param uri The URI of the image to process (content:// or file://).
     * @return The extracted text as a String.
     */
    private suspend fun recognizeTextFromUri(
        context: Context,
        uri: Uri,
    ): String {
        // Initialize MLKit Text Recognizer with default options (Latin script).
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            // suspendCancellableCoroutine converts the callback-based MLKit API into a coroutine-suspending function.
            return suspendCancellableCoroutine { continuation ->
                // Ensure the recognizer is closed if the coroutine is cancelled.
                continuation.invokeOnCancellation { recognizer.close() }
                val image: InputImage
                var inputStream: InputStream? = null
                try {
                    // Handle different URI schemes to get an InputStream.
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
                    // Create MLKit InputImage. Rotation is set to 0 as we assume correct orientation or don't handle it yet.
                    image = InputImage.fromBitmap(bitmap, 0)
                } catch (e: Exception) {
                    Log.e("OcrProcessingService", "Error creating InputImage from URI: $uri", e)
                    continuation.resumeWithException(e)
                    return@suspendCancellableCoroutine
                } finally {
                    inputStream?.close()
                }

                // Process the image asynchronously.
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

    /**
     * Processes a single video frame as an image.
     * Called by [VideoProcessingWorker] or similar services.
     */
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

        // Heuristic extraction of entities and dates.
        val entities = DataParser.tagData(ocrText)
        val documentDate =
            ExifUtils.getExifDate(context, uri)
                ?: DataParser.parseDates(ocrText).firstOrNull()
                ?: System.currentTimeMillis()

        val fileHash = com.hereliesaz.lexorcist.utils.HashingUtils.getHash(context, uri)
        val initialEvidence =
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
                allegationElementName = "",
                category = "Video Frame OCR",
                tags = listOf("ocr", "video_frame") + if (ocrText.isBlank()) listOf("non-textual") else emptyList(),
                commentary = null,
                parentVideoId = parentVideoId,
                entities = entities,
                fileHash = fileHash
            )

        logService.addLog("Saving frame evidence...")
        val savedEvidence = evidenceRepository.addEvidence(initialEvidence)

        // If saved successfully, run active scripts on this new evidence.
        return savedEvidence?.let { evidence ->
            logService.addLog("Frame evidence saved with ID: ${evidence.id}. Applying scripts...")
            val allScripts = scriptRepository.getScripts()
            val activeScriptIds = activeScriptRepository.activeScriptIds.value
            val activeScripts = allScripts.filter { activeScriptIds.contains(it.id) }
            val sortedActiveScripts = activeScripts.sortedBy { script -> activeScriptIds.indexOf(script.id) }

            var evidenceToUpdate = evidence
            sortedActiveScripts.forEach { script ->
                logService.addLog("Running script '${script.name}' on frame evidence ${evidenceToUpdate.id}")
                val scriptResult = scriptRunner.runScript(script.content, evidenceToUpdate)
                when (scriptResult) {
                    is Result.Success -> {
                        val currentTags: List<String> = evidenceToUpdate.tags
                        val newTags: List<String> = scriptResult.data.tags
                        val combinedTags: List<String> = (currentTags + newTags).distinct()
                        evidenceToUpdate = evidenceToUpdate.copy(tags = combinedTags)
                        scriptStateRepository.addScriptState(evidenceToUpdate.id, script.id)
                        logService.addLog("Script '${script.name}' for frame finished. Added tags: ${newTags.joinToString(", ")}")
                    }
                    is Result.Error -> {
                        val statusMessage = "Script error for frame '${script.name}': ${scriptResult.exception.message}"
                        logService.addLog(statusMessage, LogLevel.ERROR)
                        Log.e("OcrProcessingService", statusMessage, scriptResult.exception)
                    }
                    is Result.UserRecoverableError -> {
                        val statusMessage = "User recoverable script error for frame '${script.name}': ${scriptResult.exception.message}"
                        logService.addLog(statusMessage, LogLevel.INFO)
                        Log.w("OcrProcessingService", statusMessage, scriptResult.exception)
                    }
                    is Result.Loading -> {
                        logService.addLog("Script '${script.name}' for frame is loading...", LogLevel.INFO)
                    }
                }
            }

            if (evidenceToUpdate != evidence) {
                logService.addLog("Updating frame evidence ${evidenceToUpdate.id} with script changes.")
                evidenceRepository.updateEvidence(evidenceToUpdate)
            }
            evidenceToUpdate
        } ?: run {
            logService.addLog("Failed to save frame evidence.", LogLevel.ERROR)
            null
        }
    }

    /**
     * Main entry point for processing an image.
     * Uploads the file, runs OCR, extracts metadata, saves evidence, and runs scripts.
     *
     * @param onProgress Callback to report processing status and percentage.
     */
    suspend fun processImage(
        uri: Uri,
        context: Context,
        caseId: Long,
        spreadsheetId: String,
        onProgress: (ProcessingState) -> Unit
    ): Pair<Evidence?, String?> {
        var statusMessage: String
        logService.addLog("Starting image processing for URI: $uri")
        onProgress(ProcessingState.InProgress(0.0f))

        // Step 1: Upload file to local storage (sanitized).
        logService.addLog("Uploading image to raw evidence folder...")
        onProgress(ProcessingState.InProgress(0.1f))
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val uploadResult = storageService.uploadFile(spreadsheetId, uri, mimeType)

        return when (uploadResult) {
            is Result.Error -> {
                statusMessage = "Error uploading image: ${uploadResult.exception.message}"
                logService.addLog(statusMessage, LogLevel.ERROR)
                Log.e("OcrProcessingService", "Failed to upload image.", uploadResult.exception)
                onProgress(ProcessingState.Failure(statusMessage))
                Pair(null, statusMessage)
            }
            is Result.UserRecoverableError -> {
                statusMessage = "User recoverable error during image upload: ${uploadResult.exception.message}"
                logService.addLog(statusMessage, LogLevel.INFO)
                Log.w("OcrProcessingService", "User recoverable error during image upload.", uploadResult.exception)
                onProgress(ProcessingState.Failure(statusMessage))
                Pair(null, statusMessage)
            }
            is Result.Loading -> {
                statusMessage = "Image upload is still in progress."
                logService.addLog(statusMessage, LogLevel.INFO)
                onProgress(ProcessingState.Failure(statusMessage))
                Pair(null, statusMessage)
            }
            is Result.Success -> {
                val newUri = uploadResult.data.toUri()
                logService.addLog("Image uploaded to: $newUri")
                onProgress(ProcessingState.InProgress(0.25f))

                // Step 2: Run OCR.
                val ocrText =
                    try {
                        recognizeTextFromUri(context, newUri)
                    } catch (e: Exception) {
                        statusMessage = "Error recognizing text from image: ${e.message}"
                        logService.addLog(statusMessage, LogLevel.ERROR)
                        Log.e("OcrProcessingService", "Failed to recognize text from image.", e)
                        onProgress(ProcessingState.Failure(statusMessage))
                        return Pair(null, statusMessage)
                    }

                logService.addLog("Text recognition complete. Found ${ocrText.length} characters.")
                onProgress(ProcessingState.InProgress(0.5f))

                // Step 3: Extract Metadata & create Evidence object.
                val entities = DataParser.tagData(ocrText)
                val documentDate = ExifUtils.getExifDate(context, newUri)
                    ?: DataParser.parseDates(ocrText).firstOrNull()
                    ?: System.currentTimeMillis()
                val metadata = ExifUtils.getExifData(context, newUri)
                val fileSize = ExifUtils.getFileSize(context, newUri)
                val fileHash = com.hereliesaz.lexorcist.utils.HashingUtils.getHash(context, newUri)

                val initialEvidence =
                    Evidence(
                        id = 0,
                        caseId = caseId,
                        metadata = metadata,
                        fileSize = fileSize,
                        spreadsheetId = spreadsheetId,
                        type = "image",
                        content = ocrText,
                        formattedContent = "```\n$ocrText\n```",
                        mediaUri = newUri.toString(),
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = newUri.toString(),
                        documentDate = documentDate,
                        allegationId = null,
                        allegationElementName = "",
                        category = "Image OCR",
                        tags = listOf("ocr", "image") + if (ocrText.isBlank()) listOf("non-textual") else emptyList(),
                        commentary = null,
                        parentVideoId = null,
                        entities = entities,
                        fileHash = fileHash,
                    )

                logService.addLog("Saving initial evidence...")
                onProgress(ProcessingState.InProgress(0.7f))
                val savedEvidence = evidenceRepository.addEvidence(initialEvidence)

                // Step 4: Run Active Scripts.
                savedEvidence?.let { evidence ->
                    logService.addLog("Evidence saved with ID: ${evidence.id}. Applying scripts...")
                    onProgress(ProcessingState.InProgress(0.8f))

                    val allScripts = scriptRepository.getScripts()
                    val activeScriptIds = activeScriptRepository.activeScriptIds.value
                    val activeScripts = allScripts.filter { activeScriptIds.contains(it.id) }
                    // Preserve order of script execution.
                    val sortedActiveScripts = activeScripts.sortedBy { script -> activeScriptIds.indexOf(script.id) }

                    var evidenceToUpdate = evidence
                    sortedActiveScripts.forEach { script ->
                        logService.addLog("Running script '${script.name}' on evidence ${evidenceToUpdate.id}")
                        val scriptResult = scriptRunner.runScript(script.content, evidenceToUpdate)
                        when (scriptResult) {
                            is Result.Success -> {
                                val currentTags: List<String> = evidenceToUpdate.tags
                                val newTags: List<String> = scriptResult.data.tags
                                val combinedTags: List<String> = (currentTags + newTags).distinct()
                                evidenceToUpdate = evidenceToUpdate.copy(
                                    tags = combinedTags,
                                )
                                scriptStateRepository.addScriptState(evidenceToUpdate.id, script.id)
                                logService.addLog("Script '${script.name}' finished. Added tags: ${newTags.joinToString(", ")}")
                            }
                            is Result.Error -> {
                                statusMessage = "Script error for '${script.name}': ${scriptResult.exception.message}"
                                logService.addLog(statusMessage, LogLevel.ERROR)
                                Log.e("OcrProcessingService", statusMessage, scriptResult.exception)
                            }
                            is Result.UserRecoverableError -> {
                                statusMessage = "User recoverable script error for '${script.name}': ${scriptResult.exception.message}"
                                logService.addLog(statusMessage, LogLevel.INFO)
                                Log.w("OcrProcessingService", statusMessage, scriptResult.exception)
                            }
                            is Result.Loading -> {
                                logService.addLog("Script '${script.name}' is loading...", LogLevel.INFO)
                            }
                        }
                    }

                    if (evidenceToUpdate != evidence) {
                        logService.addLog("Updating evidence ${evidenceToUpdate.id} with script changes.")
                        evidenceRepository.updateEvidence(evidenceToUpdate)
                    }

                    statusMessage = "Image processed and evidence saved successfully."
                    onProgress(ProcessingState.Completed(statusMessage))
                    return Pair(evidenceToUpdate, statusMessage)
                }

                statusMessage = "Image processed but failed to save evidence."
                logService.addLog(statusMessage, LogLevel.ERROR)
                onProgress(ProcessingState.Failure(statusMessage))
                return Pair(null, statusMessage)
            }
        }
    }
}
