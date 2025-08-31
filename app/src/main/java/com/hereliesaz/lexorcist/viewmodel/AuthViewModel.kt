package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val tag = "AuthViewModel"

    private val _credential = MutableStateFlow<GoogleAccountCredential?>(null)
    val credential: StateFlow<GoogleAccountCredential?> = _credential.asStateFlow()

    private val _currentGoogleApiService = MutableStateFlow<GoogleApiService?>(null)
    val currentGoogleApiService: StateFlow<GoogleApiService?> = _currentGoogleApiService.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _signOutEvent = MutableSharedFlow<Unit>()
    val signOutEvent = _signOutEvent.asSharedFlow()

    fun onSignInResult(idToken: String?, email: String?, applicationName: String) {
        viewModelScope.launch {
            if (email != null && idToken != null) {
                try {
                    val newCredential = GoogleAccountCredential
                        .usingOAuth2(applicationContext, setOf(
                            "https.www.googleapis.com/auth/spreadsheets",
                            "https.www.googleapis.com/auth/drive.file"
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
        GoogleApiServiceHolder.googleApiService = apiService
        Log.d(tag, "onSignInSuccess: Signed in. GoogleApiService is now available.")
    }

    private fun onSignInFailed() {
        _currentGoogleApiService.value = null
        _credential.value = null
        _isSignedIn.value = false
        GoogleApiServiceHolder.googleApiService = null
        Log.d(tag, "onSignInFailed: Sign in failed. GoogleApiService is null.")
    }

    fun onSignOut() {
        onSignInFailed()
        viewModelScope.launch {
            _signOutEvent.emit(Unit)
        }
        Log.d(tag, "onSignOut: Signed out.")
    }
}
