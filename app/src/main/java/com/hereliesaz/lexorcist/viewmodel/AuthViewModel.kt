package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val evidenceRepository: EvidenceRepository,
    private val caseRepository: CaseRepository,
    private val googleApiService: GoogleApiService
) : ViewModel() {

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

                googleApiService.setCredential(credential)
                onSignInSuccess()
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
        com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder.googleApiService = apiService
    }

    private fun onSignInFailed() {
        _googleApiService.value = null
        _credential.value = null
        _isSignedIn.value = false
        evidenceRepository.setGoogleApiService(null)
        caseRepository.setGoogleApiService(null)
        com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder.googleApiService = null
    }

    fun onSignOut() {
        onSignInFailed()
        // Here you might want to add logic to clear other related data/state
        // For example, if there are other ViewModels holding user-specific data,
        // you could have a shared service or use some other mechanism to notify them.
    }
}
