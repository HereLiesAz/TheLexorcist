package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hereliesaz.lexorcist.model.Evidence
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {

    @Query("SELECT * FROM evidence WHERE caseId = :caseId ORDER BY timestamp DESC")
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evidence: List<Evidence>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: Evidence)

    @Update
    suspend fun update(evidence: Evidence)

    @Query("DELETE FROM evidence WHERE id = :evidenceId")
    suspend fun delete(evidenceId: Long)
}
