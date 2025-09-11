package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.AllegationsRepository // Added import
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.service.GoogleApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow // Added import
import kotlinx.coroutines.flow.asStateFlow // Added import
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val caseRepository: CaseRepository,
    private val allegationsRepository: AllegationsRepository, // Injected AllegationsRepository
    private val googleApiService: GoogleApiService?,
) : AndroidViewModel(application) {

    private val _masterAllegationsSheetId = MutableStateFlow<String?>(null)
    // val masterAllegationsSheetId = _masterAllegationsSheetId.asStateFlow() // Expose if needed elsewhere

    companion object {
        private const val TAG = "MainViewModel"
    }

    suspend fun createMasterAllegationsSheetAndInitializeRepository(): String? {
        if (googleApiService == null) {
            Log.e(TAG, "GoogleApiService is null. Cannot create master allegations sheet.")
            return null
        }
        return try {
            val sheetId = googleApiService.createAllegationsSheet() // This creates "Lexorcist - Allegations"
            if (sheetId != null) {
                _masterAllegationsSheetId.value = sheetId
                allegationsRepository.initializeMasterSheetId(sheetId)
                Log.d(TAG, "Master allegations sheet created/found and repository initialized with ID: $sheetId")
                sheetId
            } else {
                Log.e(TAG, "Failed to create or retrieve master allegations sheet ID.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while creating master allegations sheet and initializing repository", e)
            null
        }
    }

    fun populateMasterAllegationsSheetInitially(spreadsheetId: String) {
        if (googleApiService == null) {
            Log.e(TAG, "GoogleApiService is null. Cannot populate master allegations sheet.")
            return
        }
        if (spreadsheetId.isBlank()) {
            Log.e(TAG, "Spreadsheet ID is blank. Cannot populate master allegations sheet.")
            // This check is important as the caller (MainScreen) is responsible for a valid ID here.
            return
        }
        viewModelScope.launch {
            try {
                Log.d(TAG, "Populating master allegations sheet ID: $spreadsheetId initially.")
                googleApiService.populateAllegationsSheet(spreadsheetId)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while populating master allegations sheet initially for ID $spreadsheetId", e)
            }
        }
    }
}
