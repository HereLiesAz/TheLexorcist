package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.DataParser
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.utils.ExifUtils
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrProcessingService
    @Inject
    constructor(
        private val evidenceRepository: EvidenceRepository,
        private val settingsManager: SettingsManager,
        private val scriptRunner: ScriptRunner,
    ) {
        private suspend fun recognizeTextFromUri(
            context: Context,
            uri: Uri,
        ): String {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                return suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { recognizer.close() }
                    val image =
                        try {
                            InputImage.fromFilePath(context, uri)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                            return@suspendCancellableCoroutine
                        }

                    recognizer
                        .process(image)
                        .addOnSuccessListener { visionText ->
                            continuation.resume(visionText.text)
                        }.addOnFailureListener { e ->
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
            Log.d("OcrProcessingService", "processImageFrame called for URI: $uri, caseId: $caseId, parentVideoId: $parentVideoId")

            val ocrText =
                try {
                    recognizeTextFromUri(context, uri)
                } catch (e: Exception) {
                    Log.e("OcrProcessingService", "Failed to recognize text from image frame.", e)
                    "Error recognizing text from image frame: ${e.message}"
                }

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
                    is Result.Success -> {
                        newEvidence = newEvidence.copy(tags = newEvidence.tags + scriptResult.data)
                    }
                    is Result.Error -> {
                        Log.e("OcrProcessingService", "Script error for $uri: ${scriptResult.exception.message}", scriptResult.exception)
                    }
                    is Result.UserRecoverableError -> {
                        Log.e(
                            "OcrProcessingService",
                            "User recoverable script error for $uri: ${scriptResult.exception.message}",
                            scriptResult.exception,
                        )
                    }
                }
            }

            Log.d("OcrProcessingService", "Adding evidence for frame: $uri")
            return evidenceRepository.addEvidence(newEvidence)
        }

        suspend fun processImage(
            uri: Uri,
            context: Context,
            caseId: Long,
            spreadsheetId: String,
        ): Evidence? {
            val ocrText =
                try {
                    recognizeTextFromUri(context, uri)
                } catch (e: Exception) {
                    Log.e("OcrProcessingService", "Failed to recognize text from image.", e)
                    "Error recognizing text from image: ${e.message}"
                }

            val entities = DataParser.tagData(ocrText)
            val documentDate =
                ExifUtils.getExifDate(context, uri)
                    ?: DataParser.parseDates(ocrText).firstOrNull()
                    ?: System.currentTimeMillis()

            var newEvidence =
                Evidence(
                    id = 0,
                    caseId = caseId,
                    spreadsheetId = spreadsheetId,
                    type = "image",
                    content = ocrText,
                    formattedContent = "```\n$ocrText\n```",
                    mediaUri = uri.toString(),
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
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
                val scriptResult = scriptRunner.runScript(script, newEvidence)
                when (scriptResult) {
                    is Result.Success -> {
                        newEvidence = newEvidence.copy(tags = newEvidence.tags + scriptResult.data)
                    }
                    is Result.Error -> {
                        Log.e("OcrProcessingService", "Script error for $uri: ${scriptResult.exception.message}", scriptResult.exception)
                    }
                    is Result.UserRecoverableError -> {
                        Log.e(
                            "OcrProcessingService",
                            "User recoverable script error for $uri: ${scriptResult.exception.message}",
                            scriptResult.exception,
                        )
                    }
                }
            }

            return evidenceRepository.addEvidence(newEvidence)
        }
    }
