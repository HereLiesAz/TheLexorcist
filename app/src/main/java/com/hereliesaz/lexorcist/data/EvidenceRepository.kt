package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface EvidenceRepository {
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>
    suspend fun addEvidence(evidence: Evidence)
    suspend fun updateEvidence(evidence: Evidence)
    suspend fun deleteEvidence(evidence: Evidence)
}
