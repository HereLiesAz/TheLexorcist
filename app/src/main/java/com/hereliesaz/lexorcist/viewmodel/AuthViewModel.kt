package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
// import androidx.lifecycle.viewModelScope // Not strictly needed for current logic but good practice
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInCredential
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.SignInState // Added import
import com.hereliesaz.lexorcist.model.UserInfo // Added import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow // Added import
import kotlinx.coroutines.flow.asStateFlow
// import kotlinx.coroutines.launch // Not strictly needed for current logic
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    // Old states - will be removed by the refactor to use signInState
    // private val _isSignedIn = MutableStateFlow(false)
    // val isSignedIn = _isSignedIn.asStateFlow()
    // private val _signInError = MutableStateFlow<Exception?>(null)
    // val signInError = _signInError.asStateFlow()

    fun getSignInRequest(): BeginSignInRequest {
        _signInState.value = SignInState.InProgress
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(application.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true) // Consider if this is always desired
                    .build()
            )
            // .setAutoSelectEnabled(true) // Optional: attempt to sign in user silently
            .build()
    }

    fun onSignInResult(credential: SignInCredential) {
        val userInfo = UserInfo(
            displayName = credential.displayName,
            email = credential.id,
            photoUrl = credential.profilePictureUri?.toString()
        )
        _signInState.value = SignInState.Success(userInfo)
    }

    fun onSignInError(error: Exception) {
        _signInState.value = SignInState.Error("Sign-in attempt failed. Please try again.", error)
    }

    fun signOut() {
        // Here you would also typically clear any local session data or tokens
        _signInState.value = SignInState.Idle // Or a specific SignedOut state
    }

    // Helper to reset error state if user wants to retry without full sign-out
    fun clearSignInError() {
        if (_signInState.value is SignInState.Error) {
            _signInState.value = SignInState.Idle
        }
    }
}
