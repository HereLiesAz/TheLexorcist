package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.CaseRepositoryImpl
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application,
    private val evidenceRepository: EvidenceRepositoryImpl,
    private val caseRepository: CaseRepositoryImpl
) : AndroidViewModel(application) {

    private val tag = "AuthViewModel"

    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _signOutEvent = MutableSharedFlow<Unit>()
    val signOutEvent = _signOutEvent.asSharedFlow()

    fun onSignInResult(idToken: String?, email: String?, context: Context, applicationName: String) {
        viewModelScope.launch {
            if (email != null && idToken != null) {
                val credential = GoogleAccountCredential
                    .usingOAuth2(context, setOf("https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive.file"))
                credential.selectedAccountName = email

                val service = GoogleApiService(credential, applicationName)
                Log.d(tag, "GoogleApiService potentially initialized for $email.")
                onSignInSuccess(service)
            } else {
                Log.w(tag, "Sign-in result missing email or idToken.")
                onSignInFailed()
            }
        }
    }

    private fun onSignInSuccess(apiService: GoogleApiService) {
        evidenceRepository.setGoogleApiService(apiService)
        caseRepository.setGoogleApiService(apiService)
        _googleApiService.value = apiService
        _isSignedIn.value = true
        GoogleApiServiceHolder.googleApiService = apiService
        Log.d(tag, "onSignInSuccess: Signed in.")
    }

    private fun onSignInFailed() {
        evidenceRepository.setGoogleApiService(null)
        caseRepository.setGoogleApiService(null)
        _googleApiService.value = null
        _isSignedIn.value = false
        GoogleApiServiceHolder.googleApiService = null
        Log.d(tag, "onSignInFailed: Sign in failed.")
    }

    fun onSignOut() {
        onSignInFailed()
        viewModelScope.launch {
            _signOutEvent.emit(Unit)
        }
        Log.d(tag, "onSignOut: Signed out.")
    }
}
