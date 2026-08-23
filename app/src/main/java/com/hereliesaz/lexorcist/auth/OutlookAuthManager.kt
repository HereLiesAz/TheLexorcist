package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.OutlookSignInState
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class OutlookAuthManager @Inject constructor(
    private val context: Context
) {

    private var msalInstance: ISingleAccountPublicClientApplication? = null
    private val TAG = "OutlookAuthManager"

    private val _outlookSignInState = MutableStateFlow<OutlookSignInState>(OutlookSignInState.Idle)
    val outlookSignInState: StateFlow<OutlookSignInState> = _outlookSignInState.asStateFlow()

    /**
     * True when this build ships the checked-in placeholder MSAL config.
     *
     * `auth_config_single_account.json` carries `"client_id": "YOUR_CLIENT_ID"`
     * in the repository, because the real value belongs to an Azure app
     * registration the maintainer holds. Without it Microsoft sign-in cannot
     * work at all, and MSAL's own failure is an opaque `MsalClientException`
     * that surfaced as "Authentication failed: ..." -- indistinguishable from
     * a wrong password. Detecting it here lets the UI say what is actually
     * wrong.
     */
    val isConfigured: Boolean by lazy {
        try {
            val config = context.resources.openRawResource(R.raw.auth_config_single_account)
                .bufferedReader()
                .use { it.readText() }
            PLACEHOLDER_MARKERS.none { config.contains(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read the MSAL configuration", e)
            false
        }
    }

    init {
        if (!isConfigured) {
            _outlookSignInState.value = OutlookSignInState.Error(
                "Microsoft sign-in is not configured in this build.",
                IllegalStateException("auth_config_single_account.json holds a placeholder client id"),
            )
        } else {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.auth_config_single_account,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        msalInstance = application
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Error creating MSAL instance", exception)
                        _outlookSignInState.value = OutlookSignInState.Error("MSAL initialization failed.", exception)
                    }
                })
        }
    }

    suspend fun acquireToken(activity: Activity) {
        if (!isConfigured) {
            _outlookSignInState.value = OutlookSignInState.Error(
                "Microsoft sign-in is not configured in this build.",
                IllegalStateException("auth_config_single_account.json holds a placeholder client id"),
            )
            return
        }
        _outlookSignInState.value = OutlookSignInState.InProgress
        suspendCoroutine<Unit> { continuation ->
            val scopes = listOf("Mail.Read")
            val signInParameters = SignInParameters.builder()
                .withActivity(activity)
                .withLoginHint(null)
                .withScopes(scopes)
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        _outlookSignInState.value = OutlookSignInState.Success(
                            authenticationResult.account.username,
                            authenticationResult.accessToken
                        )
                        continuation.resume(Unit)
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "MSAL Authentication onError", exception)
                        _outlookSignInState.value = OutlookSignInState.Error("Authentication failed: ${exception.message}", exception)
                        continuation.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        Log.d(TAG, "MSAL Authentication onCancel")
                        val exception = Exception("Authentication was cancelled by the user.")
                        _outlookSignInState.value = OutlookSignInState.Error(exception.message ?: "Cancelled", exception)
                        continuation.resumeWithException(exception)
                    }
                })
                .build()
            msalInstance?.signIn(signInParameters) ?: run {
                val exception = IllegalStateException("MSAL instance not initialized.")
                _outlookSignInState.value = OutlookSignInState.Error(exception.message ?: "Error", exception)
                continuation.resumeWithException(exception)
            }
        }
    }

    suspend fun signOut() {
        suspendCoroutine<Unit> { continuation ->
            msalInstance?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    Log.d(TAG, "MSAL Sign out successful")
                    _outlookSignInState.value = OutlookSignInState.Idle
                    continuation.resume(Unit)
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "MSAL Sign out onError", exception)
                    _outlookSignInState.value = OutlookSignInState.Error("Error during sign out.", exception)
                    continuation.resumeWithException(exception)
                }
            }) ?: run {
                val exception = IllegalStateException("MSAL instance not initialized.")
                _outlookSignInState.value = OutlookSignInState.Error(exception.message ?: "Error", exception)
                continuation.resumeWithException(exception)
            }
        }
    }

    private companion object {
        val PLACEHOLDER_MARKERS = listOf("YOUR_CLIENT_ID", "_PLACEHOLDER_CLIENT_ID_")
    }
}
