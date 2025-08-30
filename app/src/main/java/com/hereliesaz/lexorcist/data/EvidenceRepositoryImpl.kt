package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

class EvidenceRepositoryImpl(
    private val evidenceDao: EvidenceDao
) : EvidenceRepository {
    private var googleApiService: GoogleApiService? = null

    override fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> {
        return evidenceDao.getEvidenceForCase(caseId)
    }

    override fun getEvidence(id: Int): Flow<Evidence> {
        return evidenceDao.getEvidence(id)
    }

    override suspend fun addEvidence(evidence: Evidence) {
        evidenceDao.insert(evidence)
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        evidenceDao.update(evidence)
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        evidenceDao.delete(evidence)
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        evidenceDao.updateCommentary(id, commentary)
    }
}
