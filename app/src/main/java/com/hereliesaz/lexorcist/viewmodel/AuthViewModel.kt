package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdTokenOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
    private val sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {

    private val credentialManager = CredentialManager.create(application)

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    // _pendingIntentSenderToLaunch and oneTapClient are removed

    companion object {
        private const val TAG = "AuthViewModel"
        const val PREF_USER_EMAIL_KEY = "user_email"
    }

    // Call this on app start to attempt silent sign-in
    fun attemptSilentSignIn() {
        if (_signInState.value is SignInState.Success) return // Already signed in

        _signInState.value = SignInState.InProgress
        viewModelScope.launch {
            try {
                val googleIdTokenOption = GetGoogleIdTokenOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(application.getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(true) // Key for silent sign-in
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdTokenOption)
                    .build()

                val result = credentialManager.getCredential(application, request)
                handleSignInSuccess(result.credential)
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Silent sign-in attempt failed. User may need to sign in manually.", e)
                // For silent sign-in, it's often okay to just revert to Idle without explicit error
                // unless it's a critical failure that needs reporting.
                if (_signInState.value == SignInState.InProgress) {
                     _signInState.value = SignInState.Idle
                }
            } catch (e: Exception) { // Catch any other unexpected errors
                Log.e(TAG, "Unexpected error during silent sign-in", e)
                onSignInError(e)
            }
        }
    }

    fun requestManualSignIn(activityContext: android.content.Context) { // Activity context is needed for getCredential
        _signInState.value = SignInState.InProgress
        viewModelScope.launch {
            try {
                val googleIdTokenOption = GetGoogleIdTokenOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // Show account chooser
                    .setServerClientId(application.getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdTokenOption)
                    .build()
                
                // For manual sign-in, getCredential should be called from an Activity context
                val result = credentialManager.getCredential(activityContext, request)
                handleSignInSuccess(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Manual sign-in request failed", e)
                onSignInError(e)
            } catch (e: Exception) { // Catch any other unexpected errors
                Log.e(TAG, "Unexpected error during manual sign-in", e)
                onSignInError(e)
            }
        }
    }

    private fun handleSignInSuccess(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val userInfo = UserInfo(
                    displayName = googleIdTokenCredential.displayName,
                    email = googleIdTokenCredential.id, // This is the user's email/ID
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                )
                _signInState.value = SignInState.Success(userInfo)
                sharedPreferences.edit().putString(PREF_USER_EMAIL_KEY, googleIdTokenCredential.id).apply()
                Log.d(TAG, "User email saved to SharedPreferences: ${googleIdTokenCredential.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse GoogleIdTokenCredential", e)
                onSignInError(e)
            }
        } else {
            Log.e(TAG, "Received credential is not a Google ID Token: ${credential.type}")
            onSignInError(Exception("Unexpected credential type: ${credential.type}"))
        }
    }
    
    // onSignInResult(credential: SignInCredential) is replaced by handleSignInSuccess

    fun onSignInError(error: Exception) {
        _signInState.value = SignInState.Error("Sign-in attempt failed. Please try again.", error)
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                Log.i(TAG, "CredentialManager sign out complete.")
            } catch (e: Exception) {
                Log.e(TAG, "CredentialManager sign out failed", e)
                // Optionally handle the error, e.g., show a message to the user
            } finally {
                _signInState.value = SignInState.Idle
                sharedPreferences.edit().remove(PREF_USER_EMAIL_KEY).apply()
                Log.d(TAG, "User email cleared from SharedPreferences.")
            }
        }
    }

    fun clearSignInError() {
        if (_signInState.value is SignInState.Error) {
            _signInState.value = SignInState.Idle
        }
    }

    // consumedPendingIntentSender() is removed
}
