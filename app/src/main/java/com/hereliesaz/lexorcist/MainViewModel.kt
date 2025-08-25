package com.hereliesaz.lexorcist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

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

    suspend fun createSpreadsheet(title: String): String? {
        return _googleApiService.value?.createSpreadsheet(title)
    }

    suspend fun attachScript(spreadsheetId: String, masterTemplateId: String) {
        _googleApiService.value?.attachScript(spreadsheetId, masterTemplateId)
    }
}