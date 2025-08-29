package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hereliesaz.lexorcist.data.Evidence // Corrected import
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence WHERE caseId = :caseId")
    fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evidence: List<Evidence>)

    @Query("DELETE FROM evidence")
    suspend fun deleteAll()
}
