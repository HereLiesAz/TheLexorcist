package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface EvidenceDao {
    suspend fun getEvidenceById(id: Int): Evidence? // Renaming from getEvidence to avoid conflict for now
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>
    fun getEvidenceFlow(id: Int): Flow<Evidence> // Renamed to avoid overload with suspend fun
    suspend fun insert(evidence: Evidence)
    suspend fun update(evidence: Evidence)
    suspend fun delete(evidence: Evidence)
    suspend fun updateCommentary(id: Int, commentary: String)
}
