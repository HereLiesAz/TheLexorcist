package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.DataParser
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.utils.ExifUtils
import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

@Singleton
class OcrProcessingService
    @Inject
    constructor(
        private val evidenceRepository: EvidenceRepository,
        private val caseRepository: CaseRepository,
        private val settingsManager: SettingsManager,
        private val scriptRunner: ScriptRunner,
    ) {
        suspend fun processImageFrame(
            uri: Uri,
            context: Context, // Pass context for ExifUtils and other Android-specific operations
        ): String {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromFilePath(context, uri)
            return suspendCoroutine { continuation ->
                recognizer
                    .process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resumeWith(kotlin.Result.success(visionText.text))
                    }.addOnFailureListener { e ->
                        continuation.resumeWith(kotlin.Result.success("Failed to recognize text: ${e.message}"))
                    }
            }
        }

        suspend fun processImage(
            uri: Uri,
            context: Context,
            caseId: Long,
            spreadsheetId: String,
        ) {
            val case = caseRepository.getCaseBySpreadsheetId(spreadsheetId) ?: return
            val uploadResult = evidenceRepository.uploadFile(uri, case.name)

            if (uploadResult is Result.Success) {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromFilePath(context, uri)
                val ocrText =
                    suspendCoroutine { continuation ->
                        recognizer
                            .process(image)
                            .addOnSuccessListener { visionText ->
                                continuation.resumeWith(kotlin.Result.success(visionText.text))
                            }.addOnFailureListener { e ->
                                continuation.resumeWith(kotlin.Result.success("Failed to recognize text: ${e.message}"))
                            }
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
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = uploadResult.data?.webViewLink ?: uri.toString(),
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
                            newEvidence = newEvidence.copy(tags = newEvidence.tags + scriptResult.data.tags)
                        }
                        is Result.Error -> {
                            Log.e(
                                "OcrProcessingService",
                                "Script error for $uri: ${scriptResult.exception.message}",
                                scriptResult.exception,
                            )
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

                evidenceRepository.addEvidence(newEvidence)
            }
        }
    }
