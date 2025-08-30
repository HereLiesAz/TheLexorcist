package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.flow.Flow
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl @Inject constructor(
    private val evidenceDao: EvidenceDao,
    private val googleApiService: GoogleApiService
) : EvidenceRepository {

    private var googleApiService: GoogleApiService? = null

    override fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> {
        return evidenceDao.getEvidenceForCase(caseId)
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        return evidenceDao.getEvidenceById(id)
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
