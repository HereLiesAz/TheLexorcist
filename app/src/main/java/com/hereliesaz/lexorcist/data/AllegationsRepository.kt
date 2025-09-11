package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.StateFlow

interface AllegationsRepository {
    // For case-specific allegations
    suspend fun getAllegations(caseId: String): List<Allegation>

    // For master allegations list
    val isMasterSheetIdInitialized: StateFlow<Boolean>
    suspend fun initializeMasterSheetId(id: String)
    suspend fun getMasterAllegations(): List<MasterAllegation>
    suspend fun addAllegationToMasterList(allegation: MasterAllegation): Boolean
}
