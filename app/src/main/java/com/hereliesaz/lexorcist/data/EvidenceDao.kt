package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface EvidenceDao {
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>
    fun getEvidenceFlow(id: Int): Flow<Evidence>
    suspend fun getEvidenceById(id: Int): Evidence?
    suspend fun insert(evidence: Evidence): Long
    suspend fun update(evidence: Evidence)
    suspend fun delete(evidence: Evidence)
    suspend fun updateCommentary(id: Int, commentary: String)
}
