package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl @Inject constructor(
    private val evidenceDao: EvidenceDao,
    private val googleApiService: GoogleApiService?,
    private val applicationContext: Context
) : EvidenceRepository { // Added interface implementation

    // --- EvidenceRepository Interface Implementation ---

    override suspend fun addEvidence(evidence: Evidence) {
        evidenceDao.insert(evidence)
        // TODO: Consider syncing with Google Sheets if needed
    }

    override suspend fun getEvidence(id: Int): Evidence? {
        return evidenceDao.getEvidenceById(id) // Assumes EvidenceDao has getEvidenceById
    }

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> { // Renamed and overriden
        return evidenceDao.getEvidenceForCase(caseId)
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        evidenceDao.update(evidence) // Assumes EvidenceDao has update
        // TODO: Consider syncing with Google Sheets if needed
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        evidenceDao.delete(evidence) // Assumes EvidenceDao has delete
        // TODO: Consider syncing with Google Sheets if needed
    }

    override suspend fun updateCommentary(evidenceId: Int, commentary: String) {
        evidenceDao.updateCommentary(evidenceId, commentary)
        // TODO: Sync commentary update with Google Sheets
    }

    // --- Existing Helper Methods (can be kept if distinct from interface) ---

    suspend fun addTextEvidence(caseId: Long, text: String): Result<Unit> { // Removed googleApiService param for now
        val newEvidence = Evidence(caseId = caseId, type = "text", data = text, commentary = "")
        // Use the interface method for consistency, which will call evidenceDao.insert()
        addEvidence(newEvidence)
        // TODO: Sync with Google Sheets if googleApiService is not null
        return Result.Success(Unit)
    }

    suspend fun addImageEvidence(
        caseId: Long,
        imageUri: String,
        commentary: String
    ): Result<Unit> { // Removed googleApiService param for now
        val newEvidence = Evidence(caseId = caseId, type = "image", data = imageUri, commentary = commentary)
        // Use the interface method for consistency
        addEvidence(newEvidence)
        // TODO: Upload image to Drive and sync with Google Sheets
        return Result.Success(Unit)
    }

    suspend fun addFileEvidence(
        caseId: Long,
        fileUri: String,
        commentary: String
    ): Result<Unit> { // Removed googleApiService param for now
        val newEvidence = Evidence(caseId = caseId, type = "file", data = fileUri, commentary = commentary)
        // Use the interface method for consistency
        addEvidence(newEvidence)
        // TODO: Upload file to Drive and sync with Google Sheets
        return Result.Success(Unit)
    }
}
