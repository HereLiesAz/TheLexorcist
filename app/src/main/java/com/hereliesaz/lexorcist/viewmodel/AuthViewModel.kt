package com.hereliesaz.lexorcist.viewmodel

import android.app.Application // Keep if it extends AndroidViewModel, otherwise remove
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel // Use ViewModel if not needing Application context
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.CaseRepository // Interface
import com.hereliesaz.lexorcist.data.EvidenceRepository // Interface
// Remove Impl imports if not directly used:
// import com.hereliesaz.lexorcist.data.CaseRepositoryImpl
// import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder // If still used
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext // For Context if needed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context, // Example if Context is needed
    private var injectedGoogleApiService: GoogleApiService? // Changed to nullable, and named to avoid clash
) : ViewModel() { // Changed to ViewModel, use AndroidViewModel if app context is truly needed for other things

    private val tag = "AuthViewModel"

    private val _credential = MutableStateFlow<GoogleAccountCredential?>(null)
    val credential: StateFlow<GoogleAccountCredential?> = _credential.asStateFlow()

    private val _currentGoogleApiService = MutableStateFlow<GoogleApiService?>(null)
    val currentGoogleApiService: StateFlow<GoogleApiService?> = _currentGoogleApiService.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _signOutEvent = MutableSharedFlow<Unit>()
    val signOutEvent = _signOutEvent.asSharedFlow()
    
    init {
        _currentGoogleApiService.value = injectedGoogleApiService
        _isSignedIn.value = injectedGoogleApiService != null
        if (injectedGoogleApiService != null) {
             GoogleApiServiceHolder.googleApiService = injectedGoogleApiService // If still using holder
        }
    }


    fun onSignInResult(idToken: String?, email: String?, applicationName: String) { // Context removed from params, use injected
        viewModelScope.launch {
            if (email != null && idToken != null) {
                try {
                    val newCredential = GoogleAccountCredential
                        .usingOAuth2(applicationContext, setOf(
                            "https.www.googleapis.com/auth/spreadsheets", 
                            "https.www.googleapis.com/auth/drive.file"
                            // Add other scopes like Drive, Docs, Script if needed
                            // "https://www.googleapis.com/auth/drive",
                            // "https://www.googleapis.com/auth/documents",
                            // "https://www.googleapis.com/auth/script.projects"
                        ))
                    newCredential.selectedAccountName = email
                    
                    val service = GoogleApiService(newCredential, applicationName)
                    Log.d(tag, "GoogleApiService instance created for $email.")
                    onSignInSuccess(service, newCredential)
                } catch (e: Exception) {
                    Log.e(tag, "Error during sign-in or GoogleApiService creation", e)
                    onSignInFailed()
                }
            } else {
                Log.w(tag, "Sign-in result missing email or idToken.")
                onSignInFailed()
            }
        }
    }

    private fun onSignInSuccess(apiService: GoogleApiService, newCredential: GoogleAccountCredential) {
        _currentGoogleApiService.value = apiService
        _credential.value = newCredential
        _isSignedIn.value = true
        GoogleApiServiceHolder.googleApiService = apiService // Update static holder if still used
        Log.d(tag, "onSignInSuccess: Signed in. GoogleApiService is now available.")
    }

    private fun onSignInFailed() {
        _currentGoogleApiService.value = null
        _credential.value = null
        _isSignedIn.value = false
        GoogleApiServiceHolder.googleApiService = null // Clear static holder if still used
        Log.d(tag, "onSignInFailed: Sign in failed. GoogleApiService is null.")
    }

    fun onSignOut() {
        onSignInFailed() // Clear current service and state
        viewModelScope.launch {
            _signOutEvent.emit(Unit)
        }
        Log.d(tag, "onSignOut: Signed out.")
    }
}
