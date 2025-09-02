package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hereliesaz.lexorcist.DataParser
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.utils.ExifUtils
import com.hereliesaz.lexorcist.utils.Result // Your Result class
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrProcessingService
    @Inject
    constructor(
        private val evidenceRepository: EvidenceRepository,
        private val settingsManager: SettingsManager,
        private val scriptRunner: ScriptRunner,
    ) {
        suspend fun processImageFrame(
            uri: Uri,
            context: Context, // Pass context for ExifUtils and other Android-specific operations
            caseId: Int,
            spreadsheetId: String, // Added: spreadsheetId is needed for Evidence
            parentVideoId: String?,
        ) {
            Log.d("OcrProcessingService", "processImageFrame called for URI: $uri, caseId: $caseId, parentVideoId: $parentVideoId")

            // Actual OCR processing logic would go here. For now, using placeholder.
            val ocrText = "Placeholder OCR text from image $uri"
            val entities = DataParser.tagData(ocrText)
            val documentDate =
                ExifUtils.getExifDate(context, uri)
                    ?: DataParser.parseDates(ocrText).firstOrNull()
                    ?: System.currentTimeMillis()

            var newEvidence =
                Evidence(
                    id = 0, // Repository might assign this
                    caseId = caseId.toLong(),
                    spreadsheetId = spreadsheetId, // Use passed spreadsheetId
                    type = "ocr_image_from_video",
                    content = ocrText,
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
            if (script.isNotBlank()) { // Check if script is not blank instead of not null
                val scriptResult = scriptRunner.runScript(script, newEvidence)
                when (scriptResult) {
                    is Result.Success -> {
                        newEvidence = newEvidence.copy(tags = newEvidence.tags + scriptResult.data.tags)
                    }
                    is Result.Error -> {
                        Log.e("OcrProcessingService", "Script error for $uri: ${scriptResult.exception.message}", scriptResult.exception)
                    }
                }
            }

            Log.d("OcrProcessingService", "Adding evidence for frame: $uri")
            evidenceRepository.addEvidence(newEvidence)
        }

        suspend fun processImage(
            uri: Uri,
            context: Context,
            caseId: Long,
            spreadsheetId: String,
        ) {
            val ocrText = "Placeholder OCR text from image $uri" // Replace with actual OCR logic
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
                        newEvidence = newEvidence.copy(tags = newEvidence.tags + scriptResult.data.tags)
                    }
                    is Result.Error -> {
                        Log.e("OcrProcessingService", "Script error for $uri: ${scriptResult.exception.message}", scriptResult.exception)
                    }
                }
            }

            evidenceRepository.addEvidence(newEvidence)
        }
    }
