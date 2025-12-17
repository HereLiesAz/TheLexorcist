package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalClientException
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
        try {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        msalApplication = application
                        Log.d("OneDriveAuthManager", "MSAL application created successfully")
                    }

                    override fun onError(exception: MsalException?) {
                        Log.e("OneDriveAuthManager", "Error creating MSAL application", exception)
                    }
                })
        } catch (e: Exception) {
            Log.e("OneDriveAuthManager", "Exception initializing MSAL", e)
        }
    }

    fun signIn(activity: Activity, callback: AuthenticationCallback) {
        // Implementation of MSAL sign-in logic
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

        msalApplication?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount != null) {
                    val authority = activeAccount.authority
                    val parameters = AcquireTokenSilentParameters.Builder()
                        .withScopes(scopes.toList())
                        .forAccount(activeAccount)
                        .fromAuthority(authority)
                        .withCallback(wrappedCallback)
                        .build()
                    msalApplication?.acquireTokenSilentAsync(parameters)
                } else {
                    // Handle case where there is no account
                     callback.onError(MsalClientException("no_account", "No active account"))
                }
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                // Handle account change
            }

            override fun onError(exception: MsalException) {
                // Handle exception
                callback.onError(exception)
            }
        })
    }

    fun getAccessToken(): String? {
        return authenticationResult?.accessToken
    }
}
