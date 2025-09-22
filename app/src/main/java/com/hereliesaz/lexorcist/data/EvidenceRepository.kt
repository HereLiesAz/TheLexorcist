package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow
import com.hereliesaz.lexorcist.utils.Result

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
    ): Result<Unit>

    suspend fun getExhibitsForCase(caseSpreadsheetId: String): Flow<List<Exhibit>>
    suspend fun addExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Exhibit?
    suspend fun updateExhibit(caseSpreadsheetId: String, exhibit: Exhibit)
    suspend fun deleteExhibit(caseSpreadsheetId: String, exhibit: Exhibit)
}
