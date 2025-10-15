package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.ScriptRepository
import com.hereliesaz.lexorcist.data.ActiveScriptRepository
import com.hereliesaz.lexorcist.utils.ExifUtils
import com.hereliesaz.lexorcist.utils.HashingUtils
import com.hereliesaz.lexorcist.utils.VideoUtils
import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProcessingService @Inject constructor(
    private val ocrProcessingService: OcrProcessingService,
    private val transcriptionService: TranscriptionService,
    private val evidenceRepository: EvidenceRepository,
    private val scriptRepository: ScriptRepository,
    private val activeScriptRepository: ActiveScriptRepository,
    private val scriptRunner: ScriptRunner,
    private val logService: LogService
) {

    suspend fun processVideo(
        context: Context,
        videoUri: Uri,
        caseId: Int,
        spreadsheetId: String,
        onProgress: (Float, String) -> Unit
    ): Evidence? {
        onProgress(0.1f, "Extracting frames...")
        val frameFiles = VideoUtils.extractFrames(context, videoUri, 5000)

        onProgress(0.3f, "Transcribing audio...")
        val transcriptionResult = transcriptionService.transcribeVideo(videoUri)
        val audioTranscript = if (transcriptionResult is Result.Success) {
            transcriptionResult.data
        } else {
            "Audio transcription failed."
        }

        onProgress(0.5f, "Processing frames for OCR...")
        val ocrTexts = mutableListOf<String>()
        frameFiles.forEach { frameFile ->
            val frameUri = Uri.fromFile(frameFile)
            val evidence = ocrProcessingService.processImageFrame(frameUri, context, caseId, spreadsheetId, null)
            evidence?.content?.let { ocrTexts.add(it) }
        }

        onProgress(0.7f, "Combining results...")
        val combinedText = buildString {
            append("Audio Transcript:\n")
            append(audioTranscript)
            append("\n\n--- OCR Text from Frames ---\n")
            ocrTexts.forEachIndexed { index, text ->
                append("\n[Frame ${index + 1}]\n")
                append(text)
            }
        }

        val documentDate = ExifUtils.getExifDate(context, videoUri) ?: System.currentTimeMillis()
        val fileHash = HashingUtils.getHash(context, videoUri)

        val initialEvidence = Evidence(
            id = 0,
            caseId = caseId.toLong(),
            spreadsheetId = spreadsheetId,
            type = "video",
            content = combinedText,
            formattedContent = "```\n$combinedText\n```",
            mediaUri = videoUri.toString(),
            timestamp = System.currentTimeMillis(),
            sourceDocument = videoUri.toString(),
            documentDate = documentDate,
            allegationId = null,
            allegationElementName = "",
            category = "Video Evidence",
            tags = listOf("video") + if (combinedText.isBlank()) listOf("non-textual") else emptyList(),
            commentary = null,
            parentVideoId = null,
            entities = emptyMap(),
            fileHash = fileHash
        )

        onProgress(0.8f, "Saving evidence...")
        val savedEvidence = evidenceRepository.addEvidence(initialEvidence)

        val evidenceToUpdate = savedEvidence?.let { evidence ->
            logService.addLog("Evidence saved with ID: ${evidence.id}. Applying scripts...")
            val allScripts = scriptRepository.getScripts()
            val activeScriptIds = activeScriptRepository.activeScriptIds.value
            val activeScripts = allScripts.filter { activeScriptIds.contains(it.id) }
            val sortedActiveScripts = activeScripts.sortedBy { script -> activeScriptIds.indexOf(script.id) }

            var tempEvidence = evidence
            sortedActiveScripts.forEach { script ->
                logService.addLog("Running script '${script.name}' on evidence ${tempEvidence.id}")
                val scriptResult = scriptRunner.runScript(script.content, tempEvidence)
                if (scriptResult is Result.Success) {
                    val currentTags: List<String> = tempEvidence.tags
                    val newTags: List<String> = scriptResult.data.tags
                    val combinedTags: List<String> = (currentTags + newTags).distinct()
                    tempEvidence = tempEvidence.copy(tags = combinedTags)
                }
            }
            if (tempEvidence != evidence) {
                evidenceRepository.updateEvidence(tempEvidence)
            }
            tempEvidence
        }

        onProgress(0.9f, "Cleaning up...")
        frameFiles.forEach { it.delete() }

        onProgress(1.0f, "Done.")
        return evidenceToUpdate
    }
}
