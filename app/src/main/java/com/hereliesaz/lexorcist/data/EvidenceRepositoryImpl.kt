package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.model.Evidence
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

class EvidenceRepositoryImpl(
    private val evidenceDao: EvidenceDao,
    private var googleApiService: GoogleApiService?
) : EvidenceRepository {

    fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> {
        return evidenceDao.getEvidenceForCase(caseId)
    }

    override suspend fun refreshEvidenceForCase(spreadsheetId: String, caseId: Int) {
        try {
            googleApiService?.let {
                val evidence = it.getEvidenceForCase(spreadsheetId, caseId)
                evidenceDao.deleteAll()
                evidenceDao.insertAll(evidence)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    override suspend fun addEvidence(spreadsheetId: String, evidence: Evidence) {
        googleApiService?.addEvidenceToCase(spreadsheetId, evidence)
        refreshEvidenceForCase(spreadsheetId, evidence.caseId.toInt())
    }

    override suspend fun updateEvidence(spreadsheetId: String, evidence: Evidence) {
        googleApiService?.updateEvidenceInCase(spreadsheetId, evidence)
        refreshEvidenceForCase(spreadsheetId, evidence.caseId.toInt())
    }

    override suspend fun deleteEvidence(spreadsheetId: String, evidence: Evidence) {
        googleApiService?.deleteEvidenceFromCase(spreadsheetId, evidence.id.toInt())
        refreshEvidenceForCase(spreadsheetId, evidence.caseId.toInt())
    }
}
