package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.service.GoogleApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AllegationsRepositoryImpl @Inject constructor(
    private val googleApiService: GoogleApiService?,
) : AllegationsRepository {

    private var masterAllegationsSheetId: String? = null
    private val _isMasterSheetIdInitialized = MutableStateFlow(false) // Initialize here
    override val isMasterSheetIdInitialized: StateFlow<Boolean> = _isMasterSheetIdInitialized.asStateFlow()

    companion object {
        private const val TAG = "AllegationsRepository"
    }

    override suspend fun initializeMasterSheetId(id: String) {
        masterAllegationsSheetId = id
        _isMasterSheetIdInitialized.value = true
        Log.d(TAG, "Master Allegations Sheet ID initialized: $id")
    }

    override suspend fun getAllegations(caseId: String): List<Allegation> {
        // This part remains for case-specific allegations, needs its own spreadsheet ID logic
        // For now, it might be using a hardcoded or differently managed ID.
        // This function is not the cause of the current crash related to master allegations.
        if (googleApiService == null) {
            Log.e(TAG, "GoogleApiService is null in getAllegations. Cannot fetch case allegations.")
            return emptyList()
        }
        val spreadsheetId = "1TN9MLnzpCJjcO9bwEhTeOjon3mRunYs5_tSxII6LizA" // Example, ensure this is correct
        val sheetData = googleApiService.readSpreadsheet(spreadsheetId)
        if (sheetData.isNullOrEmpty()) {
            return emptyList()
        }
        val sheet = sheetData.values.firstOrNull() ?: return emptyList()
        return sheet.mapIndexedNotNull { index, row ->
            if (row.size >= 3) {
                Allegation(
                    id = index,
                    spreadsheetId = spreadsheetId,
                    text = row[2].toString(),
                )
            } else {
                null
            }
        }
    }

    override suspend fun getMasterAllegations(): List<MasterAllegation> {
        if (googleApiService == null) {
            Log.e(TAG, "GoogleApiService is null. Cannot fetch master allegations.")
            return emptyList()
        }
        val currentSheetId = masterAllegationsSheetId
        if (currentSheetId.isNullOrBlank()) {
            Log.e(TAG, "Master allegations sheet ID is not initialized. Cannot fetch allegations.")
            return emptyList()
        }

        try {
            val sheetData = googleApiService.readSpreadsheet(currentSheetId)
            val allegationsSheet = sheetData?.get("Sheet1") // Assuming data is in "Sheet1"
            if (allegationsSheet.isNullOrEmpty() || allegationsSheet.size < 2) { // Expecting header + data
                Log.d(TAG, "Master allegations sheet is empty or has no data beyond header.")
                return emptyList()
            }
            // Skip header row (index 0)
            return allegationsSheet.subList(1, allegationsSheet.size).mapNotNull { row ->
                if (row.size >= 4) {
                    MasterAllegation(
                        type = row[0].toString(),
                        category = row[1].toString(),
                        name = row[2].toString(),
                        description = row[3].toString()
                    )
                } else {
                    Log.w(TAG, "Skipping row in master allegations due to insufficient columns: $row")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching master allegations from sheet ID $currentSheetId", e)
            return emptyList()
        }
    }

    override suspend fun addAllegationToMasterList(allegation: MasterAllegation): Boolean {
        if (googleApiService == null) {
            Log.e(TAG, "GoogleApiService is null. Cannot add allegation to master list.")
            return false
        }
        val currentSheetId = masterAllegationsSheetId
        if (currentSheetId.isNullOrBlank()) {
            Log.e(TAG, "Master allegations sheet ID is not initialized. Cannot add allegation.")
            return false
        }

        val values = listOf(
            listOf(
                allegation.type,
                allegation.category,
                allegation.name,
                allegation.description
            )
        )
        return try {
            // Appending to the first available range, assuming Sheet1 exists
            val response = googleApiService.appendData(currentSheetId, "Sheet1!A1", values)
            val success = response?.updates != null
            if (success) {
                Log.d(TAG, "Successfully added allegation to master list in sheet $currentSheetId")
            } else {
                Log.e(TAG, "Failed to add allegation to master list in sheet $currentSheetId. Response was null or had no updates.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error adding allegation to master list in sheet ID $currentSheetId", e)
            false
        }
    }
}
