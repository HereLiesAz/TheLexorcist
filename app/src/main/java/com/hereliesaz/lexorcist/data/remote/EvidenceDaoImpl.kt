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
        // This implementation is problematic as it requires a spreadsheetId, which is not available here.
        // This indicates a design issue where the DAO interface doesn't match the service capabilities.
        // For now, we'll emit an empty list to satisfy the interface.
        emit(emptyList())
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        // Not supported by GoogleApiService yet.
        return null
    }

    override fun getEvidenceFlow(id: Int): Flow<Evidence> {
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
