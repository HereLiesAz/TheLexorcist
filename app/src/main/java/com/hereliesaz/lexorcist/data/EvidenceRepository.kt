package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface EvidenceRepository {
    suspend fun getEvidenceForCase(
        spreadsheetId: String,
        caseId: Long,
    ): Flow<List<Evidence>>

    suspend fun getEvidenceById(id: Int): Evidence?

    fun getEvidence(id: Int): Flow<Evidence>

    suspend fun addEvidence(evidence: Evidence): Evidence?

    suspend fun updateEvidence(evidence: Evidence)

    suspend fun deleteEvidence(evidence: Evidence)

    suspend fun updateCommentary(
        id: Int,
        commentary: String,
    )

    suspend fun uploadFile(
        uri: android.net.Uri,
        caseName: String,
        caseSpreadsheetId: String
    ): com.hereliesaz.lexorcist.utils.Result<String>

    suspend fun updateTranscript(
        evidence: Evidence,
        newTranscript: String,
        reason: String,
    )
}
