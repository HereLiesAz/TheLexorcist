package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.hereliesaz.lexorcist.R
import javax.inject.Singleton

@Singleton
class OneDriveAuthManager constructor(
    context: Context
) {
    private var msalApplication: IPublicClientApplication? = null
    private var authenticationResult: IAuthenticationResult? = null
    private val scopes = arrayOf("Files.ReadWrite.All")

    init {
        PublicClientApplication.create(
            context,
            R.raw.msal_config,
            object : IPublicClientApplication.ApplicationCreatedListener {
                override fun onCreated(application: IPublicClientApplication) {
                    msalApplication = application
                }

                override fun onError(exception: MsalException?) {
                    // Handle exception
                }
            })
    }

    fun signIn(activity: Activity, callback: AuthenticationCallback) {
        val wrappedCallback = object : AuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult?) {
                authenticationResult = result
                callback.onSuccess(result)
            }

            override fun onError(exception: MsalException?) {
                callback.onError(exception)
            }

            override fun onCancel() {
                callback.onCancel()
            }
        }
        msalApplication?.signIn(activity, null, scopes, wrappedCallback)
    }

    fun signOut() {
        authenticationResult = null
        msalApplication?.signOut(object : IPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                // Handle sign out
            }

            override fun onError(exception: MsalException?) {
                // Handle exception
            }
        })
    }

    fun acquireTokenSilent(callback: AuthenticationCallback) {
        val authority = msalApplication?.configuration?.defaultAuthority?.authorityURL?.toString()
        val account = msalApplication?.accounts?.firstOrNull()
        if (account != null && authority != null) {
            val wrappedCallback = object : AuthenticationCallback {
                override fun onSuccess(result: IAuthenticationResult?) {
                    authenticationResult = result
                    callback.onSuccess(result)
                }

                override fun onError(exception: MsalException?) {
                    callback.onError(exception)
                }

                override fun onCancel() {
                    callback.onCancel()
                }
            }
            val parameters = com.microsoft.identity.client.AcquireTokenSilentParameters.Builder()
                .withScopes(scopes.toList())
                .forAccount(account)
                .fromAuthority(authority)
                .withCallback(wrappedCallback)
                .build()
            msalApplication?.acquireTokenSilentAsync(parameters)
        } else {
            // Handle case where there is no account or authority
        }
    }

    fun getAccessToken(): String? {
        return authenticationResult?.accessToken
    }
}
