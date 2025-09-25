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
import com.hereliesaz.lexorcist.data.ScriptRepository
import com.hereliesaz.lexorcist.data.ScriptStateRepository
import com.hereliesaz.lexorcist.model.LogLevel // Assuming this is your custom LogLevel
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.model.Script
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
import kotlinx.coroutines.flow.first // Added import

@Singleton
class OcrProcessingService
@Inject
constructor(
    private val evidenceRepository: EvidenceRepository,
    private val scriptRepository: ScriptRepository,
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

    suspend fun processImage(
        uri: Uri,
        context: Context,
        caseId: Long,
        spreadsheetId: String,
        activeScriptIds: Set<String>,
        onProgress: (ProcessingState) -> Unit
    ): Pair<Evidence?, String?> {
        var statusMessage: String
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
                logService.addLog(statusMessage, LogLevel.INFO)
                Log.w("OcrProcessingService", "User recoverable error during image upload.", uploadResult.exception)
                onProgress(ProcessingState.Failure(statusMessage))
                return Pair(null, statusMessage)
            }
            is Result.Loading -> {
                logService.addLog("Image upload in progress...")
                onProgress(ProcessingState.InProgress(0.25f))
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
                val metadata = ExifUtils.getExifData(context, newUri)
                val fileSize = ExifUtils.getFileSize(context, newUri)
                logService.addLog("Determined document date: $documentDate")
                val fileHash = com.hereliesaz.lexorcist.utils.HashingUtils.getHash(context, newUri)

                var newEvidence =
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
                        category = "Image OCR",
                        tags = listOf("ocr", "image"),
                        commentary = null,
                        parentVideoId = null,
                        entities = entities,
                        fileHash = fileHash,
                    )

                val allScripts = scriptRepository.getScripts()
                val activeScripts = allScripts.filter { activeScriptIds.contains(it.id) }
                val scriptState = scriptStateRepository.scriptState.first()

                activeScripts.forEach { script ->
                    val scriptAlreadyRun = scriptState.contains("${newEvidence.id}:${script.hashCode()}")
                    if (!scriptAlreadyRun) {
                        logService.addLog("Running script: ${script.name}")
                        onProgress(ProcessingState.InProgress(0.75f))
                        val scriptResult = scriptRunner.runScript(script.content, newEvidence)
                        when (scriptResult) {
                            is Result.Success -> {
                                val currentTagsInImage: List<String> = newEvidence.tags
                                val newTagsFromScriptImage: List<String> = scriptResult.data.tags
                                val combinedTagsImage: List<String> = currentTagsInImage + newTagsFromScriptImage
                                newEvidence = newEvidence.copy(tags = combinedTagsImage)
                                scriptStateRepository.addScriptState(newEvidence.id, script.hashCode())
                                logService.addLog("Script finished. Added tags: ${newTagsFromScriptImage.joinToString(", ")}")
                            }
                            is Result.Error -> {
                                statusMessage = "Script error: ${scriptResult.exception.message}"
                                logService.addLog(statusMessage, LogLevel.ERROR)
                                Log.e("OcrProcessingService", "Script error for $uri: $statusMessage", scriptResult.exception)
                            }
                            else -> {}
                        }
                    }
                }

                logService.addLog("Saving evidence...")
                onProgress(ProcessingState.InProgress(0.90f))
                val savedEvidence = evidenceRepository.addEvidence(newEvidence)
                if (savedEvidence != null) {
                    val scriptState = scriptStateRepository.scriptState.first()
                    activeScripts.forEach { script ->
                        val scriptAlreadyRun = scriptState.contains("${savedEvidence.id}:${script.hashCode()}")
                        if (!scriptAlreadyRun) {
                            logService.addLog("Running script: ${script.name}")
                            onProgress(ProcessingState.InProgress(0.75f))
                            val scriptResult = scriptRunner.runScript(script.content, savedEvidence)
                            when (scriptResult) {
                                is Result.Success -> {
                                    val currentTagsInImage: List<String> = savedEvidence.tags
                                    val newTagsFromScriptImage: List<String> = scriptResult.data.tags
                                    val combinedTagsImage: List<String> = currentTagsInImage + newTagsFromScriptImage
                                    val updatedEvidence = savedEvidence.copy(tags = combinedTagsImage)
                                    evidenceRepository.updateEvidence(updatedEvidence)
                                    scriptStateRepository.addScriptState(savedEvidence.id, script.hashCode())
                                    logService.addLog("Script finished. Added tags: ${newTagsFromScriptImage.joinToString(", ")}")
                                }
                                is Result.Error -> {
                                    statusMessage = "Script error: ${scriptResult.exception.message}"
                                    logService.addLog(statusMessage, LogLevel.ERROR)
                                    Log.e("OcrProcessingService", "Script error for $uri: $statusMessage", scriptResult.exception)
                                }
                                else -> {}
                            }
                        }
                    }
                }

                logService.addLog("Evidence saved with ID: ${savedEvidence?.id}")
                onProgress(ProcessingState.InProgress(1.0f))
                statusMessage = if (savedEvidence != null) "Image processed and evidence saved successfully." else "Image processed but failed to save evidence."
                onProgress(ProcessingState.Completed(statusMessage))
                return Pair(savedEvidence, statusMessage)
            }
        }
    }
}
