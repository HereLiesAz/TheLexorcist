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
                    val storedUserEmail = sharedPreferences.getString(PREF_USER_EMAIL_KEY, null)
                    if (storedUserEmail != null) {
                        googleIdOptionBuilder.setFilterByAuthorizedAccounts(true)
                    } else {
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
                        Log.e(TAG, "Unexpected credential type: ${credential.type}")
                        onSignInError(Exception("Unexpected credential type"))
                    }
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "GetCredentialException", e)
                    if (!silent) {
                        onSignInError(e)
                    } else {
                        _signInState.value = SignInState.Idle 
                    }
                }
            }
        }

        private fun onSignInResult(googleIdTokenCredential: GoogleIdTokenCredential) {
            val userId = googleIdTokenCredential.id // .id is the non-nullable unique user identifier (subject claim)

            // Ensure the userId is not blank, as an empty string is not a valid account name.
            if (userId.isBlank()) { 
                Log.e(TAG, "User ID (sub) is blank from GoogleIdTokenCredential. Cannot proceed.")
                onSignInError(Exception("User ID not available or is blank. Please ensure ID permission is granted or try again."))
                return
            }

            val userInfo =
                UserInfo(
                    displayName = googleIdTokenCredential.displayName,
                    // While 'id' is the unique identifier, the 'email' field is typically what's displayed and used for account association.
                    // However, for GoogleAccountCredential's 'selectedAccountName', a non-empty string is required. 'id' serves this purpose.
                    email = userId, // Using userId (sub) as the primary identifier here as per original logic.
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                )
            _signInState.value = SignInState.Success(userInfo)
            
            // Store the validated userId (which is the 'id'/'sub' from the token)
            sharedPreferences.edit { putString(PREF_USER_EMAIL_KEY, userId) }
            Log.d(TAG, "User ID (sub) saved to SharedPreferences: $userId")

            // Create and store the GoogleAccountCredential
            val scopes = listOf(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS)
            val accountCredential = GoogleAccountCredential.usingOAuth2(application, scopes)
            // Use the validated, non-blank userId for selectedAccountName
            accountCredential.selectedAccountName = userId 
            credentialHolder.credential = accountCredential
            credentialHolder.googleApiService =
                com.hereliesaz.lexorcist.service
                    .GoogleApiService(accountCredential, application.getString(R.string.app_name))
        }

        fun onSignInError(error: Exception) {
            _signInState.value = SignInState.Error("Sign-in attempt failed. Details: ${error.message}", error)
        }

        fun signOut() {
            viewModelScope.launch {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                _signInState.value = SignInState.Idle
                sharedPreferences.edit { remove(PREF_USER_EMAIL_KEY) }
                Log.d(TAG, "User ID (sub) cleared from SharedPreferences.")
                credentialHolder.credential = null
                credentialHolder.googleApiService = null
            }
        }

        fun clearSignInError() {
            if (_signInState.value is SignInState.Error) {
                _signInState.value = SignInState.Idle
            }
        }
    }
