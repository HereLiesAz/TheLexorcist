package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException
import com.hereliesaz.lexorcist.R
import javax.inject.Singleton

@Singleton
class OneDriveAuthManager constructor(
    context: Context
) {
    private var msalApplication: ISingleAccountPublicClientApplication? = null
    private var authenticationResult: IAuthenticationResult? = null
    private val scopes = arrayOf("Files.ReadWrite.All")

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.msal_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
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
        val signInParameters = SignInParameters.builder()
            .withActivity(activity)
            .withScopes(scopes.toMutableList())
            .withCallback(wrappedCallback)
            .build()
        msalApplication?.signIn(signInParameters)
    }

    fun signOut(callback: ISingleAccountPublicClientApplication.SignOutCallback) {
        authenticationResult = null
        msalApplication?.signOut(callback)
    }

    fun acquireTokenSilent(callback: AuthenticationCallback) {
        msalApplication?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: com.microsoft.identity.client.IAccount?) {
                if (activeAccount != null) {
                    val authority = activeAccount.authority
                    val parameters = com.microsoft.identity.client.AcquireTokenSilentParameters.Builder()
                        .withScopes(scopes.toList())
                        .forAccount(activeAccount)
                        .fromAuthority(authority)
                        .withCallback(callback)
                        .build()
                    msalApplication?.acquireTokenSilentAsync(parameters)
                } else {
                    // Handle case where there is no account
                }
            }

            override fun onAccountChanged(priorAccount: com.microsoft.identity.client.IAccount?, currentAccount: com.microsoft.identity.client.IAccount?) {
                // Handle account change
            }

            override fun onError(exception: MsalException) {
                // Handle exception
            }
        })
    }

    fun getAccessToken(): String? {
        return authenticationResult?.accessToken
    }
}
