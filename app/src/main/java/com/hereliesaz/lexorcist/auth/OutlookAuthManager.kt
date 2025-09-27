package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.hereliesaz.lexorcist.R
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
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

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    msalInstance = application
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Error creating MSAL instance", exception)
                }
            })
    }

    suspend fun acquireToken(activity: Activity): IAuthenticationResult = suspendCoroutine { continuation ->
        val scopes = listOf("Mail.Read")
        val signInParameters = SignInParameters.builder()
            .withActivity(activity)
            .withLoginHint(null)
            .withScopes(scopes)
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    continuation.resume(authenticationResult)
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "MSAL Authentication onError", exception)
                    continuation.resumeWithException(exception)
                }

                override fun onCancel() {
                    Log.d(TAG, "MSAL Authentication onCancel")
                    continuation.resumeWithException(Exception("Authentication was cancelled by the user."))
                }
            })
            .build()
        msalInstance?.signIn(signInParameters) ?: continuation.resumeWithException(IllegalStateException("MSAL instance not initialized."))
    }

    suspend fun signOut() {
        suspendCoroutine<Unit> { continuation ->
            msalInstance?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    Log.d(TAG, "MSAL Sign out successful")
                    continuation.resume(Unit)
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "MSAL Sign out onError", exception)
                    continuation.resumeWithException(exception)
                }
            }) ?: continuation.resumeWithException(IllegalStateException("MSAL instance not initialized."))
        }
    }
}