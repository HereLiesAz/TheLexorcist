package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface EvidenceRepository {
    fun setGoogleApiService(googleApiService: com.hereliesaz.lexorcist.GoogleApiService?)
    fun setCaseSpreadsheetId(id: String)
    fun setCaseScriptId(id: String)
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>
    suspend fun addEvidence(caseId: Int, content: String, sourceDocument: String, category: String, allegationId: Int?)
    suspend fun getEvidenceById(id: Int): Evidence?
    fun getEvidence(id: Int): Flow<Evidence>
    suspend fun addEvidence(evidence: Evidence)
    suspend fun updateEvidence(evidence: Evidence)
    suspend fun deleteEvidence(evidence: Evidence)
    suspend fun updateCommentary(id: Int, commentary: String)
}
