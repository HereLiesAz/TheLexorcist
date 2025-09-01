package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context // Added
import android.content.IntentSender
import android.content.SharedPreferences // Added
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
    private val application: Application,
    private val sharedPreferences: SharedPreferences // Added
) : AndroidViewModel(application) {

    private val oneTapClient: SignInClient = Identity.getSignInClient(application)

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    private val _pendingIntentSenderToLaunch = MutableStateFlow<IntentSender?>(null)
    val pendingIntentSenderToLaunch: StateFlow<IntentSender?> = _pendingIntentSenderToLaunch.asStateFlow()

    companion object {
        private const val TAG = "AuthViewModel"
        const val PREF_USER_EMAIL_KEY = "user_email" // Added for SharedPreferences
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
                viewModelScope.launch {
                    _pendingIntentSenderToLaunch.value = result.pendingIntent.intentSender
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Silent sign-in attempt failed. User may need to sign in manually.", e)
                if (_signInState.value == SignInState.InProgress) {
                    _signInState.value = SignInState.Idle
                }
            }
    }

    fun requestManualSignIn() {
        _signInState.value = SignInState.InProgress
        val manualSignInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(application.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(manualSignInRequest)
            .addOnSuccessListener { result ->
                 viewModelScope.launch {
                    _pendingIntentSenderToLaunch.value = result.pendingIntent.intentSender
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Manual sign-in request failed", e)
                onSignInError(e)
            }
    }

    fun onSignInResult(credential: SignInCredential) {
        val userInfo = UserInfo(
            displayName = credential.displayName,
            email = credential.id, // This is the user's email/ID
            photoUrl = credential.profilePictureUri?.toString()
        )
        _signInState.value = SignInState.Success(userInfo)
        // Save email to SharedPreferences
        sharedPreferences.edit().putString(PREF_USER_EMAIL_KEY, credential.id).apply()
        Log.d(TAG, "User email saved to SharedPreferences: ${credential.id}")
    }

    fun onSignInError(error: Exception) {
        _signInState.value = SignInState.Error("Sign-in attempt failed. Please try again.", error)
    }

    fun signOut() {
        oneTapClient.signOut().addOnCompleteListener {
            Log.i(TAG, "OneTapClient sign out complete.")
            _signInState.value = SignInState.Idle
            // Clear email from SharedPreferences
            sharedPreferences.edit().remove(PREF_USER_EMAIL_KEY).apply()
            Log.d(TAG, "User email cleared from SharedPreferences.")
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
