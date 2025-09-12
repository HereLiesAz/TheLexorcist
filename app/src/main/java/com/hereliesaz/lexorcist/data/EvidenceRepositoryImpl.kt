package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.auth.CredentialHolder // Added import
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl
    @Inject
    constructor(
        private val credentialHolder: CredentialHolder, // Changed from GoogleApiService?
        private val evidenceCacheManager: com.hereliesaz.lexorcist.utils.EvidenceCacheManager,
    ) : EvidenceRepository {
        private val evidenceByCaseMap = mutableMapOf<Long, kotlinx.coroutines.flow.MutableStateFlow<List<Evidence>>>()

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

        override suspend fun getEvidenceById(id: Int): Evidence? {
            return evidenceByCaseMap.values.flatMap { it.value }.find { it.id == id }
        }

        override fun getEvidence(id: Int): Flow<Evidence> {
            return emptyFlow()
        }

        override suspend fun addEvidence(evidence: Evidence) {
            val googleApiService = credentialHolder.googleApiService // Get from holder
            googleApiService?.addEvidenceToCase(evidence)
            if (googleApiService != null) {
                refreshEvidence(evidence.spreadsheetId, evidence.caseId)
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
    }
