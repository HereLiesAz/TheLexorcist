package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
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
        // Assuming EvidenceDao.getEvidenceForCase expects a Long and handles mapping to Evidence.caseId (Int) if necessary internally or via query conversion.
        // Or, that the Case ID in the database is indeed stored as Long and Evidence.caseId (Int) is for the model object after retrieval.
        return evidenceDao.getEvidenceForCase(caseId.toLong())
    }

    override suspend fun refreshEvidenceForCase(spreadsheetId: String, caseId: Int) {
        try {
            googleApiService?.let {
                val evidenceList = it.getEvidenceForCase(spreadsheetId, caseId) // This now returns List<data.Evidence>
                evidenceDao.deleteAll() // Consider deleting only for the specific caseId if applicable
                evidenceDao.insertAll(evidenceList)
            }
        } catch (e: Exception) {
            // Handle error, e.g., log it or emit an error state
        }
    }

    override suspend fun addEvidence(spreadsheetId: String, evidence: Evidence) {
        googleApiService?.addEvidenceToCase(spreadsheetId, evidence)
        refreshEvidenceForCase(spreadsheetId, evidence.caseId) // evidence.caseId is Int
    }

    override suspend fun updateEvidence(spreadsheetId: String, evidence: Evidence) {
        googleApiService?.updateEvidenceInCase(spreadsheetId, evidence)
        refreshEvidenceForCase(spreadsheetId, evidence.caseId) // evidence.caseId is Int
    }

    override suspend fun deleteEvidence(spreadsheetId: String, evidence: Evidence) {
        googleApiService?.deleteEvidenceFromCase(spreadsheetId, evidence.id) // evidence.id is Int
        refreshEvidenceForCase(spreadsheetId, evidence.caseId) // evidence.caseId is Int
    }
}
