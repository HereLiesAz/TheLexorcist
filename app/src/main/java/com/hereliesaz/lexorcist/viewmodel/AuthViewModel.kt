package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.model.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    private val oneTapClient: SignInClient = Identity.getSignInClient(application)

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    private val _pendingIntentSenderToLaunch = MutableStateFlow<IntentSender?>(null)
    val pendingIntentSenderToLaunch: StateFlow<IntentSender?> = _pendingIntentSenderToLaunch.asStateFlow()

    companion object {
        private const val TAG = "AuthViewModel"
    }

    // Call this on app start to attempt silent sign-in
    fun attemptSilentSignIn() {
        if (_signInState.value is SignInState.Success) return // Already signed in

        _signInState.value = SignInState.InProgress // Indicate an attempt is happening
        val silentSignInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(application.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(true) // Key for silent/automatic sign-in attempt
            .build()

        oneTapClient.beginSignIn(silentSignInRequest)
            .addOnSuccessListener { result ->
                viewModelScope.launch { // Use viewModelScope for coroutine context if needed, though not strictly here
                    _pendingIntentSenderToLaunch.value = result.pendingIntent.intentSender
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Silent sign-in attempt failed. User may need to sign in manually.", e)
                // If silent fails, go back to Idle so manual sign-in button can be used
                // Don't set to Error here as it might preempt manual sign-in UI
                if (_signInState.value == SignInState.InProgress) { // Check if still InProgress from this attempt
                    _signInState.value = SignInState.Idle
                }
            }
    }

    // Call this for user-initiated sign-in (e.g., button click)
    fun requestManualSignIn() {
        _signInState.value = SignInState.InProgress
        val manualSignInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(application.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true) // Filter by accounts that already authorized your app
                    .build()
            )
            // setAutoSelectEnabled is typically false or omitted for manual sign-in prompt
            .build()

        oneTapClient.beginSignIn(manualSignInRequest)
            .addOnSuccessListener { result ->
                 viewModelScope.launch {
                    _pendingIntentSenderToLaunch.value = result.pendingIntent.intentSender
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Manual sign-in request failed", e)
                onSignInError(e) // Use existing error handler
            }
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
        // Here you would also typically clear any local session data or tokens if you were using them
        // And call oneTapClient.signOut() if needed, though state reset is primary here
        oneTapClient.signOut().addOnCompleteListener {
            Log.i(TAG, "OneTapClient sign out complete.")
            _signInState.value = SignInState.Idle
        }
    }

    fun clearSignInError() {
        if (_signInState.value is SignInState.Error) {
            _signInState.value = SignInState.Idle
        }
    }

    fun consumedPendingIntentSender() {
        _pendingIntentSenderToLaunch.value = null
    }
}
