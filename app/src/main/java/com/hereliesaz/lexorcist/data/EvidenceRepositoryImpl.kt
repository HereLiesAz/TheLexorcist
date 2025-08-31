package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class EvidenceRepositoryImpl @Inject constructor(
    private val evidenceDao: EvidenceDao,
    private val googleApiService: GoogleApiService?
) : EvidenceRepository {

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> {
        return evidenceDao.getEvidenceForCase(caseId)
    }

    override fun getEvidenceFlow(id: Int): Flow<Evidence> {
        return evidenceDao.getEvidenceFlow(id)
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        return evidenceDao.getEvidenceById(id)
    }

    override suspend fun addEvidence(evidence: Evidence) {
        val newId = evidenceDao.insert(evidence)
        googleApiService?.addEvidenceToCase(evidence.spreadsheetId, evidence.copy(id = newId.toInt()))
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        evidenceDao.update(evidence)
        googleApiService?.updateEvidenceInCase(evidence.spreadsheetId, evidence)
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        evidenceDao.delete(evidence)
        googleApiService?.deleteEvidenceFromCase(evidence.spreadsheetId, evidence.id)
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        evidenceDao.updateCommentary(id, commentary)
    }
}
