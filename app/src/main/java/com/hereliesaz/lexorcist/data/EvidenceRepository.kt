package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface EvidenceRepository {
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>
    suspend fun refreshEvidenceForCase(spreadsheetId: String, caseId: Int)
    suspend fun addEvidence(spreadsheetId: String, evidence: Evidence)
    suspend fun updateEvidence(spreadsheetId: String, evidence: Evidence)
    suspend fun deleteEvidence(spreadsheetId: String, evidence: Evidence)
}
