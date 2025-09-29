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
    private val storageService: StorageService,
    private val localFileStorageService: LocalFileStorageService,
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
        when (val result = localFileStorageService.getEvidenceForCase(spreadsheetId)) {
            is Result.Loading -> { /* Handle loading state, perhaps log or emit a specific UI state */ }
            is Result.Success<List<Evidence>> -> {
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
        val result = addEvidenceList(listOf(evidence))
        return when (result) {
            is Result.Success<List<Evidence>> -> result.data.firstOrNull()
            else -> null
        }
    }

    override suspend fun addEvidenceList(evidenceList: List<Evidence>): Result<List<Evidence>> {
        if (evidenceList.isEmpty()) {
            return Result.Success(emptyList())
        }
        val spreadsheetId = evidenceList.first().spreadsheetId
        val caseId = evidenceList.first().caseId

        // It's better to cast here to avoid changing the DI graph for a quick fix.
        val localService = storageService as? LocalFileStorageService
            ?: return Result.Error(Exception("StorageService is not a LocalFileStorageService, cannot perform batch add."))

        return when (val result = localService.addEvidenceList(spreadsheetId, evidenceList)) {
            is Result.Success<List<Evidence>> -> {
                refreshEvidence(spreadsheetId, caseId)
                Result.Success(result.data)
            }
            is Result.Error -> result
            is Result.UserRecoverableError -> result
            is Result.Loading -> result
        }
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        when (localFileStorageService.updateEvidence(evidence.spreadsheetId, evidence)) {
            is Result.Success<Unit> -> refreshEvidence(evidence.spreadsheetId, evidence.caseId)
            else -> { /* Handle error */ }
        }
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        when (localFileStorageService.deleteEvidence(evidence.spreadsheetId, evidence)) {
            is Result.Success<Unit> -> refreshEvidence(evidence.spreadsheetId, evidence.caseId)
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
        return localFileStorageService.uploadFile(caseSpreadsheetId, uri, mimeType)
    }

    override suspend fun updateTranscript(
        evidence: Evidence,
        newTranscript: String,
        reason: String,
    ): Result<Unit> {
        val result: Result<Unit> = localFileStorageService.updateTranscript(evidence, newTranscript, reason)
        if (result is Result.Success<Unit>) {
            refreshEvidence(evidence.spreadsheetId, evidence.caseId)
        }
        return result
    }

    override suspend fun getExhibitsForCase(caseSpreadsheetId: String): Flow<List<Exhibit>> {
        // This is a simple implementation that doesn't cache exhibits.
        // A more robust implementation would cache the exhibits similar to how evidence is cached.
        return when (val result = localFileStorageService.getExhibitsForCase(caseSpreadsheetId)) {
            is Result.Success<List<Exhibit>> -> kotlinx.coroutines.flow.flowOf(result.data)
            else -> emptyFlow()
        }
    }

    override suspend fun addExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Exhibit? {
        return when (val result = localFileStorageService.addExhibit(caseSpreadsheetId, exhibit)) {
            is Result.Success<Exhibit> -> result.data
            else -> null
        }
    }

    override suspend fun updateExhibit(caseSpreadsheetId: String, exhibit: Exhibit) {
        localFileStorageService.updateExhibit(caseSpreadsheetId, exhibit)
    }

    override suspend fun deleteExhibit(caseSpreadsheetId: String, exhibit: Exhibit) {
        localFileStorageService.deleteExhibit(caseSpreadsheetId, exhibit)
    }
}
