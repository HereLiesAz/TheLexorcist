package com.hereliesaz.lexorcist.data

import android.app.Application
import android.net.Uri
import com.hereliesaz.lexorcist.utils.EvidenceCacheManager
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl
@Inject
constructor(
    private val storageService: StorageService, // Injected StorageService
    private val evidenceCacheManager: EvidenceCacheManager,
    private val application: Application,
) : EvidenceRepository {
    private val evidenceByCaseMap =
        mutableMapOf<String, MutableStateFlow<List<Evidence>>>()

    override suspend fun getEvidenceForCase(
        spreadsheetId: String,
        caseId: Long, // caseId is not used in the new model, spreadsheetId is the key
    ): Flow<List<Evidence>> {
        if (!evidenceByCaseMap.containsKey(spreadsheetId)) {
            evidenceByCaseMap[spreadsheetId] = MutableStateFlow(emptyList())
            val cachedEvidence = evidenceCacheManager.loadEvidence(caseId) // a caseId long is still used for cache
            if (cachedEvidence != null) {
                evidenceByCaseMap[spreadsheetId]?.value = cachedEvidence
            } else {
                refreshEvidence(spreadsheetId, caseId)
            }
        }
        return evidenceByCaseMap[spreadsheetId]!!.asStateFlow()
    }

    private suspend fun refreshEvidence(
        spreadsheetId: String,
        caseId: Long,
    ) {
        when (val result = storageService.getEvidenceForCase(spreadsheetId)) {
            is Result.Loading -> { /* Handle loading state, perhaps log or emit a specific UI state */ }
            is Result.Success -> {
                evidenceCacheManager.saveEvidence(caseId, result.data)
                evidenceByCaseMap[spreadsheetId]?.value = result.data
            }
            is Result.Error -> { /* Handle error */ }
            is Result.UserRecoverableError -> { /* Handle error */ }
        }
    }

    override suspend fun getEvidenceById(id: Int): Evidence? =
        evidenceByCaseMap.values.flatMap { it.value }.find { it.id == id }

    override fun getEvidence(id: Int): Flow<Evidence> = emptyFlow()

    override suspend fun addEvidence(evidence: Evidence): Evidence? {
        val existingEvidenceResult = storageService.getEvidenceForCase(evidence.spreadsheetId)
        val existingEvidence = if (existingEvidenceResult is Result.Success) {
            existingEvidenceResult.data
        } else {
            emptyList()
        }
        val isDuplicate = evidence.fileHash != null && existingEvidence.any { it.fileHash == evidence.fileHash }

        val evidenceToAdd = if (isDuplicate) {
            evidence.copy(isDuplicate = true)
        } else {
            evidence
        }

        return when (val result = storageService.addEvidence(evidenceToAdd.spreadsheetId, evidenceToAdd)) {
            is Result.Success -> {
                refreshEvidence(evidenceToAdd.spreadsheetId, evidenceToAdd.caseId)
                result.data
            }
            else -> null
        }
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        when (storageService.updateEvidence(evidence.spreadsheetId, evidence)) {
            is Result.Success -> refreshEvidence(evidence.spreadsheetId, evidence.caseId)
            else -> { /* Handle error */ }
        }
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        when (storageService.deleteEvidence(evidence.spreadsheetId, evidence)) {
            is Result.Success -> refreshEvidence(evidence.spreadsheetId, evidence.caseId)
            else -> { /* Handle error */ }
        }
    }

    override suspend fun updateCommentary(
        id: Int,
        commentary: String,
    ) {
        val evidenceItem = getEvidenceById(id)
        if (evidenceItem != null) {
            val updatedEvidence = evidenceItem.copy(commentary = commentary)
            updateEvidence(updatedEvidence)
        }
    }

    override suspend fun uploadFile(
        uri: Uri,
        caseName: String, // Not used, but kept for interface compatibility
        caseSpreadsheetId: String
    ): Result<String> {
        val mimeType = application.contentResolver.getType(uri) ?: "application/octet-stream"
        return storageService.uploadFile(caseSpreadsheetId, uri, mimeType)
    }

    override suspend fun updateTranscript(
        evidence: Evidence,
        newTranscript: String,
        reason: String,
    ): Result<Unit> {
        val result: Result<Unit> = storageService.updateTranscript(evidence, newTranscript, reason)
        if (result is Result.Success) {
            refreshEvidence(evidence.spreadsheetId, evidence.caseId)
        }
        return result
    }

    override suspend fun getExhibitsForCase(caseSpreadsheetId: String): Flow<List<Exhibit>> {
        // This is a simple implementation that doesn't cache exhibits.
        // A more robust implementation would cache the exhibits similar to how evidence is cached.
        return when (val result = storageService.getExhibitsForCase(caseSpreadsheetId)) {
            is Result.Success -> kotlinx.coroutines.flow.flowOf(result.data)
            else -> emptyFlow()
        }
    }

    override suspend fun addExhibit(exhibit: Exhibit): Exhibit? {
        return when (val result = storageService.addExhibit(exhibit.caseId.toString(), exhibit)) {
            is Result.Success -> result.data
            else -> null
        }
    }

    override suspend fun updateExhibit(exhibit: Exhibit) {
        storageService.updateExhibit(exhibit.caseId.toString(), exhibit)
    }

    override suspend fun deleteExhibit(exhibit: Exhibit) {
        storageService.deleteExhibit(exhibit.caseId.toString(), exhibit)
    }
}
