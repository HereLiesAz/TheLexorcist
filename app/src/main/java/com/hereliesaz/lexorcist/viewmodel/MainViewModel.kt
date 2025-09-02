package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val caseRepository: CaseRepository,
    private val googleApiService: GoogleApiService?
) : AndroidViewModel(application) {

    fun createAllegationsSheet() {
        viewModelScope.launch {
            googleApiService?.createAllegationsSheet()
        }
    }

    fun populateAllegationsSheet(spreadsheetId: String) {
        viewModelScope.launch {
            googleApiService?.populateAllegationsSheet(spreadsheetId)
        }
    }
}
