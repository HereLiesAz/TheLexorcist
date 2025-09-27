package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
import com.hereliesaz.lexorcist.R
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class OutlookAuthManager @Inject constructor(
    private val context: Context
) {

    private var msalInstance: ISingleAccountPublicClientApplication? = null

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    msalInstance = application
                }

                override fun onError(exception: MsalException) {
                    // Handle error
                }
            })
    }

    suspend fun acquireToken(activity: Activity): IAuthenticationResult? = suspendCoroutine { continuation ->
        val scopes = listOf("https://graph.microsoft.com/.default")
        val signInParameters = SignInParameters.builder()
            .withActivity(activity)
            .withLoginHint(null)
            .withScopes(scopes)
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    continuation.resume(authenticationResult)
                }

                override fun onError(exception: MsalException) {
                    continuation.resume(null)
                }

                override fun onCancel() {
                    continuation.resume(null)
                }
            })
            .build()
        msalInstance?.signIn(signInParameters)
    }

    suspend fun signOut(): Boolean = suspendCoroutine { continuation ->
        msalInstance?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                continuation.resume(true)
            }

            override fun onError(exception: MsalException) {
                continuation.resume(false)
            }
        })
    }
}