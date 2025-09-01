package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl @Inject constructor(
    // Changed to nullable
    private val googleApiService: GoogleApiService?,
    private val evidenceCacheManager: com.hereliesaz.lexorcist.utils.EvidenceCacheManager
) : EvidenceRepository {

    private val evidenceByCaseMap = mutableMapOf<Long, kotlinx.coroutines.flow.MutableStateFlow<List<Evidence>>>()

    override suspend fun getEvidenceForCase(spreadsheetId: String, caseId: Long): Flow<List<Evidence>> {
        if (!evidenceByCaseMap.containsKey(caseId)) {
            evidenceByCaseMap[caseId] = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
            // Load from cache or remote
            val cachedEvidence = evidenceCacheManager.loadEvidence(caseId)
            if (cachedEvidence != null) {
                evidenceByCaseMap[caseId]?.value = cachedEvidence
            } else {
                refreshEvidence(spreadsheetId, caseId)
            }
        }
        return evidenceByCaseMap[caseId]!!.asStateFlow()
    }

    private suspend fun refreshEvidence(spreadsheetId: String, caseId: Long) {
        // Null-safe call needed here after the change
        googleApiService?.getEvidenceForCase(spreadsheetId, caseId)?.let { remoteEvidence ->
            evidenceCacheManager.saveEvidence(caseId, remoteEvidence)
            evidenceByCaseMap[caseId]?.value = remoteEvidence
        }
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        // This is inefficient, should be improved if needed
        return evidenceByCaseMap.values.flatMap { it.value }.find { it.id == id }
    }

    override fun getEvidence(id: Int): Flow<Evidence> {
        // This is inefficient and doesn't update. Not implemented.
        return emptyFlow()
    }

    override suspend fun addEvidence(evidence: Evidence) {
        // Null-safe call needed here after the change
        googleApiService?.addEvidenceToCase(evidence)
        // refreshEvidence might also need to be conditional on googleApiService not being null
        // if it strictly relies on a successful remote operation.
        // For now, keeping existing logic, but this is a point of attention.
        if (googleApiService != null) { // Only refresh if service was available to add
            refreshEvidence(evidence.spreadsheetId, evidence.caseId)
        }
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        // TODO: Implement actual logic to update evidence in Google Sheets (with null safety for googleApiService)
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        // TODO: Implement actual logic to delete evidence from Google Sheets (with null safety for googleApiService)
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        // TODO: Implement actual logic to update commentary for an evidence item (with null safety for googleApiService)
    }
}
