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

    override fun getEvidenceForCase(spreadsheetId: String, caseId: Long): Flow<List<Evidence>> = flow {
        emit(googleApiService.getEvidenceForCase(spreadsheetId, caseId.toInt()))
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        // Not supported by GoogleApiService yet.
        return null
    }

    override fun getEvidence(id: Int): Flow<Evidence> {
        // Not supported by GoogleApiService yet.
        return flow { }
    }

    override suspend fun insert(evidence: Evidence) {
        googleApiService.addEvidenceToCase(evidence.spreadsheetId, evidence)
    }

    override suspend fun update(evidence: Evidence) {
        googleApiService.updateEvidenceInCase(evidence.spreadsheetId, evidence)
    }

    override suspend fun delete(evidence: Evidence) {
        googleApiService.deleteEvidenceFromCase(evidence.spreadsheetId, evidence.id)
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        // Not supported by GoogleApiService yet.
    }
}
