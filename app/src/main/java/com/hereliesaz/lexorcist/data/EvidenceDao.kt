package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hereliesaz.lexorcist.data.Evidence // Corrected import
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence WHERE id = :id")
    suspend fun getEvidenceById(id: Int): Evidence?

    @Query("SELECT * FROM evidence WHERE caseId = :caseId")
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>

    @Query("SELECT * FROM evidence WHERE id = :id")
    fun getEvidence(id: Int): Flow<Evidence>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: Evidence)

    @androidx.room.Update
    suspend fun update(evidence: Evidence)

    @androidx.room.Delete
    suspend fun delete(evidence: Evidence)

    @Query("UPDATE evidence SET commentary = :commentary WHERE id = :id")
    suspend fun updateCommentary(id: Int, commentary: String)
}
