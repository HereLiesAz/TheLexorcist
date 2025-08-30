package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.CaseRepositoryImpl
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application,
    private val evidenceRepository: EvidenceRepositoryImpl,
    private val caseRepository: CaseRepositoryImpl
) : AndroidViewModel(application) {

    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService.asStateFlow()

    private val _credential = MutableStateFlow<GoogleAccountCredential?>(null)
    val credential: StateFlow<GoogleAccountCredential?> = _credential.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    fun onSignInResult(idToken: String?, email: String?, context: Context, applicationName: String) {
        viewModelScope.launch {
            if (email != null && idToken != null) {
                val credential = GoogleAccountCredential
                    .usingOAuth2(context, setOf("https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive.file"))
                credential.selectedAccountName = email

                val service = GoogleApiService(credential, applicationName)
                onSignInSuccess(service, credential)
            } else {
                onSignInFailed()
            }
        }
    }

    private fun onSignInSuccess(apiService: GoogleApiService, credential: GoogleAccountCredential) {
        _googleApiService.value = apiService
        _credential.value = credential
        _isSignedIn.value = true
        evidenceRepository.setGoogleApiService(apiService)
        caseRepository.setGoogleApiService(apiService)
    }

    private fun onSignInFailed() {
        _googleApiService.value = null
        _credential.value = null
        _isSignedIn.value = false
        evidenceRepository.setGoogleApiService(null)
        caseRepository.setGoogleApiService(null)
    }

    fun onSignOut() {
        onSignInFailed()
    }
}
