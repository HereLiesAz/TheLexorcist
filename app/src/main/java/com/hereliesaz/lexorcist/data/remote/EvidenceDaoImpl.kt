package com.hereliesaz.lexorcist.data.remote

import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class EvidenceDaoImpl @Inject constructor(
    private val googleApiService: GoogleApiService
) : EvidenceDao {

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> = flow {
        // This is not possible with the current Google Sheets setup
        emit(emptyList())
    }

    override fun getEvidenceFlow(id: Int): Flow<Evidence> = flow {
        // This is not possible with the current Google Sheets setup
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        // This is not possible with the current Google Sheets setup
        return null
    }

    override suspend fun insert(evidence: Evidence): Long {
        googleApiService.addEvidenceToCase(evidence.spreadsheetId, evidence)
        return 0
    }

    override suspend fun update(evidence: Evidence) {
        googleApiService.updateEvidenceInCase(evidence.spreadsheetId, evidence)
    }

    override suspend fun delete(evidence: Evidence) {
        googleApiService.deleteEvidenceFromCase(evidence.spreadsheetId, evidence.id)
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        // This is not supported by the Google Sheets implementation.
    }
}
