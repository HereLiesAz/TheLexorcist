package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

class EvidenceRepositoryImpl(
    private val evidenceDao: EvidenceDao
) : EvidenceRepository {

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> {
        return evidenceDao.getEvidenceForCase(caseId)
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
}
