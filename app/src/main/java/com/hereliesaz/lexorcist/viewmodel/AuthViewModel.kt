package com.hereliesaz.lexorcist.viewmodel

import android.accounts.Account
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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.di.IODispatcher
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.model.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
        private val firebaseAuth: FirebaseAuth, // Injected FirebaseAuth
        @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
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

                try {
                    // Check if already signed in with Firebase for silent sign-in
                    if (silent) {
                        val currentUser = firebaseAuth.currentUser
                        val storedUserEmail = sharedPreferences.getString(PREF_USER_EMAIL_KEY, null)
                        if (currentUser != null && currentUser.email == storedUserEmail) {
                            Log.d(TAG, "User already signed in with Firebase. Email: ${currentUser.email}")
                            onSignInSuccess(currentUser)
                            return@launch
                        } else if (storedUserEmail.isNullOrBlank()) {
                            // No stored email, so silent sign-in isn't possible if not already signed in
                            _signInState.value = SignInState.Idle
                            return@launch
                        }
                        // If storedUserEmail exists but doesn't match currentUser or currentUser is null,
                        // proceed with Google sign-in to refresh or establish Firebase session.
                        Log.d(TAG, "Attempting silent Google sign-in for stored email: $storedUserEmail")
                    }

                    val rawNonce = UUID.randomUUID().toString()
                    val hashedNonce = generateHashedNonce(rawNonce) // Corrected function name

                    val googleIdOptionBuilder =
                        GetGoogleIdOption
                            .Builder()
                            .setServerClientId(application.getString(R.string.default_web_client_id))
                            .setNonce(hashedNonce)
                            .setFilterByAuthorizedAccounts(silent && sharedPreferences.contains(PREF_USER_EMAIL_KEY))

                    val request: GetCredentialRequest =
                        GetCredentialRequest
                            .Builder()
                            .addCredentialOption(googleIdOptionBuilder.build())
                            .build()

                    Log.d(TAG, "Requesting Google ID token...")
                    val result = credentialManager.getCredential(activity, request)
                    val credentialFromResult = result.credential

                    if (credentialFromResult is CustomCredential &&
                        credentialFromResult.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialFromResult.data)
                        val idToken = googleIdTokenCredential.idToken
                        if (idToken.isNullOrBlank()) {
                            Log.e(TAG, "Google ID token is null or blank.")
                            onSignInFailure(Exception("Failed to get Google ID token."))
                            return@launch
                        }
                        Log.d(TAG, "Successfully obtained Google ID token. Signing into Firebase...")
                        // Switch to IO dispatcher for Firebase network call
                        val firebaseUser =
                            withContext(ioDispatcher) {
                                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                                firebaseAuth.signInWithCredential(firebaseCredential).await()?.user
                            }

                        if (firebaseUser != null) {
                            Log.d(TAG, "Firebase sign-in successful. User: ${firebaseUser.email}")
                            onSignInSuccess(firebaseUser)
                        } else {
                            Log.e(TAG, "Firebase sign-in successful but user is null.")
                            onSignInFailure(Exception("Firebase sign-in successful but no user data received."))
                        }
                    } else {
                        Log.e(TAG, "Unexpected credential type: ${credentialFromResult.type}")
                        onSignInFailure(Exception("Unexpected credential type from CredentialManager"))
                    }
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "GetCredentialException", e)
                    if (!silent) {
                        onSignInFailure(e)
                    } else {
                        _signInState.value = SignInState.Idle
                    }
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Received an invalid google id token response", e)
                    onSignInFailure(e)
                } catch (e: Exception) {
                    // Catch other exceptions like Firebase related ones
                    Log.e(TAG, "Sign-in failed with generic exception", e)
                    onSignInFailure(e)
                }
            }
        }

        // Renamed for clarity
        private fun onSignInSuccess(firebaseUser: FirebaseUser) {
            val userEmail = firebaseUser.email
            Log.d(TAG, "FirebaseUser details: Email='$userEmail', DisplayName='${firebaseUser.displayName}', UID='${firebaseUser.uid}'")

            if (userEmail.isNullOrBlank()) {
                Log.e(TAG, "Firebase user email is null or blank. Cannot proceed with Google API setup.")
                onSignInFailure(Exception("Firebase user email is missing. This is required for Google API access."))
                return
            }

            val userInfo =
                UserInfo(
                    displayName = firebaseUser.displayName,
                    email = userEmail,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                )
            _signInState.value = SignInState.Success(userInfo)

            sharedPreferences.edit { putString(PREF_USER_EMAIL_KEY, userEmail) }
            Log.d(TAG, "User email saved to SharedPreferences: '$userEmail'")

            val scopes = listOf(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS, "profile", "email")
            val accountCredential = GoogleAccountCredential.usingOAuth2(application, scopes)

            // Create an Account object explicitly
            val googleAccount = Account(userEmail, "com.google") // userEmail is known to be non-null here
            Log.d(TAG, "Attempting to set GoogleAccountCredential selectedAccount to: ${googleAccount.name}")
            accountCredential.setSelectedAccount(googleAccount) // Use setSelectedAccount with the Account object

            credentialHolder.credential = accountCredential
            // Removed assignment to credentialHolder.googleApiService as it's a val with a custom getter
            Log.d(TAG, "GoogleApiService will be initialized by CredentialHolder when accessed.")
        }

        // Renamed for clarity
        private fun onSignInFailure(error: Exception) {
            _signInState.value = SignInState.Error("Sign-in attempt failed. ${error.message}", error)
            Log.e(TAG, "Sign-in error reported to UI: ", error) // More detailed log for reported errors
        }

        fun signOut() {
            viewModelScope.launch {
                Log.d(TAG, "Signing out...")
                try {
                    withContext(ioDispatcher) {
                        firebaseAuth.signOut()
                    }
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    _signInState.value = SignInState.Idle
                    sharedPreferences.edit { remove(PREF_USER_EMAIL_KEY) }
                    Log.d(TAG, "User email cleared from SharedPreferences.")
                    credentialHolder.credential = null
                    // Removed assignment to credentialHolder.googleApiService as it's a val with a custom getter
                    Log.d(TAG, "Sign out complete.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during sign out: ", e)
                    // Optionally update UI with sign-out error
                }
            }
        }

        fun clearSignInError() {
            if (_signInState.value is SignInState.Error) {
                _signInState.value = SignInState.Idle
            }
        }

        // Helper function to hash the nonce
        private fun generateHashedNonce(nonce: String): String { // Corrected function name
            val bytes = nonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun storeDropboxAccessToken(accessToken: String) {
            sharedPreferences.edit {
                putString("dropbox_access_token", accessToken)
            }
        }
    }