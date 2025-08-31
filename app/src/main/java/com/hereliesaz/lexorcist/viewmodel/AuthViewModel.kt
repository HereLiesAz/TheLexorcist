package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInCredential
import com.hereliesaz.lexorcist.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn = _isSignedIn.asStateFlow()

    private val _signInError = MutableStateFlow<Exception?>(null)
    val signInError = _signInError.asStateFlow()

    fun getSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(application.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .build()
    }

    fun onSignInResult(credential: SignInCredential) {
        // Here you would typically exchange the ID token for your own session token with your backend
        // For this example, we'll just consider the sign-in successful
        _isSignedIn.value = true
    }

    fun onSignInError(error: Exception) {
        _signInError.value = error
    }

    fun signOut() {
        _isSignedIn.value = false
    }
}
