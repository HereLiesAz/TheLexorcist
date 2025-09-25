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
        allegationElementName: String?,
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
                allegationElementName = allegationElementName,
                category = "Video Frame OCR",
                tags = listOf("ocr", "video_frame"),
                commentary = null,
                parentVideoId = parentVideoId,
                entities = entities,
                fileHash = fileHash
            )

        logService.addLog("Saving frame evidence...")
        val savedEvidence = evidenceRepository.addEvidence(initialEvidence)

        return savedEvidence?.let { evidence ->
            logService.addLog("Frame evidence saved with ID: ${evidence.id}. Applying scripts...")
            val allScripts = scriptRepository.getScripts()
            val activeScriptIds = activeScriptRepository.activeScriptIds.value
            val activeScripts = allScripts.filter { activeScriptIds.contains(it.id.toString()) }
            val sortedActiveScripts = activeScripts.sortedBy { script -> activeScriptIds.indexOf(script.id.toString()) }

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

    suspend fun processImage(
        uri: Uri,
        context: Context,
        caseId: Long,
        spreadsheetId: String,
        activeScriptIds: List<String>,
        onProgress: (ProcessingState) -> Unit
    ): Pair<Evidence?, String?> {
        var statusMessage: String
        logService.addLog("Starting image processing for URI: $uri")
        onProgress(ProcessingState.InProgress(0.0f))

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
                // This should ideally not happen if the suspending function is designed correctly
                statusMessage = "Image upload is still in progress."
                logService.addLog(statusMessage, LogLevel.INFO)
                onProgress(ProcessingState.Failure(statusMessage))
                Pair(null, statusMessage)
            }
            is Result.Success -> {
                val newUri = uploadResult.data.toUri()
                logService.addLog("Image uploaded to: $newUri")
                onProgress(ProcessingState.InProgress(0.25f))

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
                        allegationElementName = null,
                        category = "Image OCR",
                        tags = listOf("ocr", "image"),
                        commentary = null,
                        parentVideoId = null,
                        entities = entities,
                        fileHash = fileHash,
                    )

                logService.addLog("Saving initial evidence...")
                onProgress(ProcessingState.InProgress(0.7f))
                val savedEvidence = evidenceRepository.addEvidence(initialEvidence)

                savedEvidence?.let { evidence ->
                    logService.addLog("Evidence saved with ID: ${evidence.id}. Applying scripts...")
                    onProgress(ProcessingState.InProgress(0.8f))

                    val allScripts = scriptRepository.getScripts()
                    val activeScripts = allScripts.filter { activeScriptIds.contains(it.id.toString()) }
                    val sortedActiveScripts = activeScripts.sortedBy { script -> activeScriptIds.indexOf(script.id.toString()) }

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
                                    // Potentially update other fields here if the script supports it
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
                                // This case might not be relevant for synchronous script execution
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

                // This block is reached if savedEvidence is null
                statusMessage = "Image processed but failed to save evidence."
                logService.addLog(statusMessage, LogLevel.ERROR)
                onProgress(ProcessingState.Failure(statusMessage))
                return Pair(null, statusMessage)
            }
        }
    }
}