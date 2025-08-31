package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.flow.Flow
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl @Inject constructor(
    private val evidenceDao: EvidenceDao,
    private val googleApiService: GoogleApiService?
) : EvidenceRepository {

    override fun getEvidenceForCase(spreadsheetId: String, caseId: Long): Flow<List<Evidence>> {
        return evidenceDao.getEvidenceForCase(caseId)
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        return evidenceDao.getEvidenceById(id)
    }

    override fun getEvidence(id: Int): Flow<Evidence> {
        return evidenceDao.getEvidence(id)
    }

    override suspend fun addEvidence(evidence: Evidence) {
        googleApiService?.let { api ->
            val newId = api.addEvidenceToCase(evidence.spreadsheetId, evidence)
            if (newId != null) {
                val finalEvidence = evidence.copy(id = newId)
                evidenceDao.insert(finalEvidence)
            }
        }
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        googleApiService?.updateEvidenceInCase(evidence.spreadsheetId, evidence)
        evidenceDao.update(evidence)
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        googleApiService?.deleteEvidenceFromCase(evidence.spreadsheetId, evidence.id)
        evidenceDao.delete(evidence)
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        // This method is not supported by the Google Sheets implementation.
    }
}
