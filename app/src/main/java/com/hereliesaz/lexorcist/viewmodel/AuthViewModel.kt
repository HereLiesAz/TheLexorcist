package com.hereliesaz.lexorcist.viewmodel

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.model.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val application: Application,
        private val sharedPreferences: SharedPreferences,
        private val credentialManager: CredentialManager,
        private val credentialHolder: CredentialHolder,
    ) : AndroidViewModel(application) {
        private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
        val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

        companion object {
            private const val TAG = "AuthViewModel"
            const val PREF_USER_EMAIL_KEY = "user_email"
        }

        fun signIn(
            activity: Activity,
            silent: Boolean = false,
        ) {
            viewModelScope.launch {
                if (!silent) {
                    _signInState.value = SignInState.InProgress
                }

                // Generate a nonce
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val nonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOptionBuilder =
                    GetGoogleIdOption
                        .Builder()
                        .setServerClientId(application.getString(R.string.default_web_client_id))
                        .setNonce(nonce)

                if (silent) {
                    val userEmail = sharedPreferences.getString(PREF_USER_EMAIL_KEY, null)
                    if (userEmail != null) {
                        googleIdOptionBuilder.setFilterByAuthorizedAccounts(true)
                    } else {
                        // No user to silently sign in, so we just exit
                        _signInState.value = SignInState.Idle
                        return@launch
                    }
                } else {
                    googleIdOptionBuilder.setFilterByAuthorizedAccounts(false)
                }

                val request: GetCredentialRequest =
                    GetCredentialRequest
                        .Builder()
                        .addCredentialOption(googleIdOptionBuilder.build())
                        .build()

                try {
                    val result = credentialManager.getCredential(activity, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            onSignInResult(googleIdTokenCredential)
                        } catch (e: GoogleIdTokenParsingException) {
                            Log.e(TAG, "Received an invalid google id token response", e)
                            onSignInError(e)
                        }
                    } else {
                        onSignInError(Exception("Unexpected credential type"))
                    }
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "GetCredentialException", e)
                    // If silent sign-in fails, we don't need to show an error, just go to manual sign-in
                    if (!silent) {
                        onSignInError(e)
                    } else {
                        _signInState.value = SignInState.Idle
                    }
                }
            }
        }

        private fun onSignInResult(credential: GoogleIdTokenCredential) {
            val userInfo =
                UserInfo(
                    displayName = credential.displayName,
                    email = credential.id, // This is the user's email/ID
                    photoUrl = credential.profilePictureUri?.toString(),
                )
            _signInState.value = SignInState.Success(userInfo)
            sharedPreferences.edit { putString(PREF_USER_EMAIL_KEY, credential.id) }
            Log.d(TAG, "User email saved to SharedPreferences: ${credential.id}")

            // Create and store the GoogleAccountCredential
            val scopes = listOf(DriveScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
            val accountCredential = GoogleAccountCredential.usingOAuth2(application, scopes)
            accountCredential.selectedAccountName = credential.id
            credentialHolder.credential = accountCredential
        }

        fun onSignInError(error: Exception) {
            _signInState.value = SignInState.Error("Sign-in attempt failed. Please try again.", error)
        }

        fun signOut() {
            viewModelScope.launch {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                _signInState.value = SignInState.Idle
                sharedPreferences.edit { remove(PREF_USER_EMAIL_KEY) }
                Log.d(TAG, "User email cleared from SharedPreferences.")
                credentialHolder.credential = null
            }
        }

        fun clearSignInError() {
            if (_signInState.value is SignInState.Error) {
                _signInState.value = SignInState.Idle
            }
        }
    }
