package com.hereliesaz.lexorcist

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _plaintiffs = MutableStateFlow("")
    val plaintiffs: StateFlow<String> = _plaintiffs

    private val _defendants = MutableStateFlow("")
    val defendants: StateFlow<String> = _defendants

    private val _court = MutableStateFlow("")
    val court: StateFlow<String> = _court

    init {
        loadCaseInfo()
    }

    fun onPlaintiffsChanged(name: String) {
        _plaintiffs.value = name
        saveCaseInfo()
    }

    fun onDefendantsChanged(name: String) {
        _defendants.value = name
        saveCaseInfo()
    }

    fun onCourtChanged(name: String) {
        _court.value = name
        saveCaseInfo()
    }

    private fun saveCaseInfo() {
        with(sharedPref.edit()) {
            putString("plaintiffs", _plaintiffs.value)
            putString("defendants", _defendants.value)
            putString("court", _court.value)
            apply()
        }
    }

    private fun loadCaseInfo() {
        _plaintiffs.value = sharedPref.getString("plaintiffs", "") ?: ""
        _defendants.value = sharedPref.getString("defendants", "") ?: ""
        _court.value = sharedPref.getString("court", "") ?: ""
    }


    fun onSignInSuccess(apiService: GoogleApiService) {
        _googleApiService.value = apiService
        _isSignedIn.value = true
    }

    fun onSignInFailed() {
        _googleApiService.value = null
        _isSignedIn.value = false
    }

    fun onSignOut() {
        _googleApiService.value = null
        _isSignedIn.value = false
    }

    suspend fun createMasterTemplate(): String? {
        return _googleApiService.value?.createMasterTemplate()
    }

    suspend fun createSpreadsheet(title: String, caseInfo: Map<String, String>): String? {
        return _googleApiService.value?.createSpreadsheet(title, caseInfo)
    }

    suspend fun attachScript(spreadsheetId: String, masterTemplateId: String) {
        _googleApiService.value?.attachScript(spreadsheetId, masterTemplateId)
    }
}