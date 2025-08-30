package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface EvidenceRepository {
    fun setGoogleApiService(googleApiService: com.hereliesaz.lexorcist.GoogleApiService?)
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>
    fun getEvidence(id: Int): Flow<Evidence>
    suspend fun addEvidence(evidence: Evidence)
    suspend fun updateEvidence(evidence: Evidence)
    suspend fun deleteEvidence(evidence: Evidence)
    suspend fun updateCommentary(id: Int, commentary: String)
}
