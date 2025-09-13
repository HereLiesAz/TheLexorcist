package com.hereliesaz.lexorcist.data

import android.app.Application
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl
    @Inject
    constructor(
        private val credentialHolder: CredentialHolder,
        private val evidenceCacheManager: com.hereliesaz.lexorcist.utils.EvidenceCacheManager,
        private val application: Application,
    ) : EvidenceRepository {
        private val evidenceByCaseMap =
            mutableMapOf<Long, kotlinx.coroutines.flow.MutableStateFlow<List<Evidence>>>()

        override suspend fun getEvidenceForCase(
            spreadsheetId: String,
            caseId: Long,
        ): Flow<List<Evidence>> {
            if (!evidenceByCaseMap.containsKey(caseId)) {
                evidenceByCaseMap[caseId] = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
                val cachedEvidence = evidenceCacheManager.loadEvidence(caseId)
                if (cachedEvidence != null) {
                    evidenceByCaseMap[caseId]?.value = cachedEvidence
                } else {
                    refreshEvidence(spreadsheetId, caseId)
                }
            }
            return evidenceByCaseMap[caseId]!!.asStateFlow()
        }

        private suspend fun refreshEvidence(
            spreadsheetId: String,
            caseId: Long,
        ) {
            val googleApiService = credentialHolder.googleApiService // Get from holder
            googleApiService?.getEvidenceForCase(spreadsheetId, caseId)?.let { remoteEvidence ->
                evidenceCacheManager.saveEvidence(caseId, remoteEvidence)
                evidenceByCaseMap[caseId]?.value = remoteEvidence
            }
        }

        override suspend fun getEvidenceById(id: Int): Evidence? = evidenceByCaseMap.values.flatMap { it.value }.find { it.id == id }

        override fun getEvidence(id: Int): Flow<Evidence> = emptyFlow()

        override suspend fun addEvidence(evidence: Evidence): Evidence? {
            val googleApiService = credentialHolder.googleApiService
            val response = googleApiService?.addEvidenceToCase(evidence)
            if (googleApiService != null) {
                refreshEvidence(evidence.spreadsheetId, evidence.caseId)
            }
            return response?.updates?.updatedRange?.let {
                val row = it.substringAfter("A").substringBefore(":").toIntOrNull()
                if (row != null) {
                    evidence.copy(id = row)
                } else {
                    null
                }
            }
        }

        override suspend fun updateEvidence(evidence: Evidence) {
            val googleApiService = credentialHolder.googleApiService // Get from holder
            googleApiService?.let {
                if (it.updateEvidenceInSheet(evidence)) {
                    refreshEvidence(evidence.spreadsheetId, evidence.caseId)
                }
            }
        }

        override suspend fun deleteEvidence(evidence: Evidence) {
            val googleApiService = credentialHolder.googleApiService // Get from holder
            googleApiService?.let {
                if (it.deleteEvidenceFromSheet(evidence)) {
                    refreshEvidence(evidence.spreadsheetId, evidence.caseId)
                }
            }
        }

        override suspend fun updateCommentary(
            id: Int,
            commentary: String,
        ) {
            val evidenceItem = getEvidenceById(id) // Renamed for clarity
            if (evidenceItem != null) {
                val updatedEvidence = evidenceItem.copy(commentary = commentary)
                updateEvidence(updatedEvidence)
            }
        }

        override suspend fun uploadFile(
            uri: android.net.Uri,
            caseName: String,
        ): Result<com.google.api.services.drive.model.File?> {
            val googleApiService = credentialHolder.googleApiService ?: return Result.Error(Exception("GoogleApiService not available"))
            val evidenceFolderId =
                googleApiService.getOrCreateEvidenceFolder(caseName)
                    ?: return Result.Error(Exception("Could not create evidence folder"))

            val tempFile =
                File(application.cacheDir, "upload_${System.currentTimeMillis()}")
            try {
                application.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val mimeType = application.contentResolver.getType(uri) ?: "application/octet-stream"
                return googleApiService.uploadFile(tempFile, evidenceFolderId, mimeType)
            } catch (e: Exception) {
                return Result.Error(e)
            } finally {
                tempFile.delete()
            }
        }

        override suspend fun updateTranscript(
            evidence: Evidence,
            newTranscript: String,
            reason: String,
        ) {
            val googleApiService = credentialHolder.googleApiService
            googleApiService?.let {
                val updatedEvidence = evidence.copy(content = newTranscript)
                if (it.updateEvidenceInSheet(updatedEvidence)) {
                    val sheetData = it.readSpreadsheet(evidence.spreadsheetId)
                    if (sheetData?.get("Edit History") == null) {
                        it.addSheet(evidence.spreadsheetId, "Edit History")
                        val header =
                            listOf(
                                listOf(
                                    "Evidence ID",
                                    "Timestamp",
                                    "Original Transcript",
                                    "New Transcript",
                                    "Reason for Edit",
                                ),
                            )
                        it.appendData(evidence.spreadsheetId, "Edit History!A1", header)
                    }

                    val editHistoryRow =
                        listOf(
                            listOf(
                                evidence.id.toString(),
                                System.currentTimeMillis().toString(),
                                evidence.content,
                                newTranscript,
                                reason,
                            ),
                        )
                    it.appendData(evidence.spreadsheetId, "Edit History!A:E", editHistoryRow)
                    refreshEvidence(evidence.spreadsheetId, evidence.caseId)
                }
            }
        }
    }
