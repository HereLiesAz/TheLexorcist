package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
// import com.microsoft.graph.identity.client.AcquireTokenSilentParameters // TODO: Re-enable MSAL
// import com.microsoft.graph.core.authentication.identity.client.AuthenticationCallback // TODO: Re-enable MSAL
// import com.microsoft.identity.client.IAccount // TODO: Re-enable MSAL
// import com.microsoft.identity.client.IAuthenticationResult // TODO: Re-enable MSAL
// import com.microsoft.identity.client.IPublicClientApplication // TODO: Re-enable MSAL
// import com.microsoft.identity.client.ISingleAccountPublicClientApplication // TODO: Re-enable MSAL
// import com.microsoft.identity.client.PublicClientApplication // TODO: Re-enable MSAL
// import com.microsoft.identity.client.SignInParameters // TODO: Re-enable MSAL
// import com.microsoft.identity.client.exception.MsalException // TODO: Re-enable MSAL
import com.hereliesaz.lexorcist.R
import javax.inject.Singleton

@Singleton
class OneDriveAuthManager constructor(
    context: Context
) {
    // TODO: Re-enable MSAL
    // private var msalApplication: ISingleAccountPublicClientApplication? = null
    // private var authenticationResult: IAuthenticationResult? = null
    // private val scopes = arrayOf("Files.ReadWrite.All")

    init {
        // TODO: Re-enable MSAL initialization and fix R.raw.msal_config if missing
        // PublicClientApplication.createSingleAccountPublicClientApplication(
        //     context,
        //     R.raw.msal_config, // Ensure this resource exists or handle its absence
        //     object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
        //         override fun onCreated(application: ISingleAccountPublicClientApplication) {
        //             msalApplication = application
        //         }
        //
        //         override fun onError(exception: MsalException?) {
        //             // Handle exception
        //         }
        //     })
    }

    // TODO: Re-enable MSAL and use proper AuthenticationCallback type
    fun signIn(activity: Activity, callback: Any /* AuthenticationCallback */) {
        // TODO: Implement MSAL signIn logic
        // val wrappedCallback = object : AuthenticationCallback {
        //     override fun onSuccess(result: IAuthenticationResult?) {
        //         authenticationResult = result
        //         (callback as? AuthenticationCallback)?.onSuccess(result)
        //     }
        //
        //     override fun onError(exception: MsalException?) {
        //         (callback as? AuthenticationCallback)?.onError(exception)
        //     }
        //
        //     override fun onCancel() {
        //         (callback as? AuthenticationCallback)?.onCancel()
        //     }
        // }
        // val signInParameters = SignInParameters.builder()
        //     .withActivity(activity)
        //     .withScopes(scopes.toMutableList())
        //     .withCallback(wrappedCallback)
        //     .build()
        // msalApplication?.signIn(signInParameters)
    }

    // TODO: Re-enable MSAL and use proper ISingleAccountPublicClientApplication.SignOutCallback type
    fun signOut(callback: Any /* ISingleAccountPublicClientApplication.SignOutCallback */) {
        // TODO: Implement MSAL signOut logic
        // authenticationResult = null
        // msalApplication?.signOut(callback as? ISingleAccountPublicClientApplication.SignOutCallback)
    }

    // TODO: Re-enable MSAL and use proper AuthenticationCallback type
    fun acquireTokenSilent(callback: Any /* AuthenticationCallback */) {
        // TODO: Implement MSAL acquireTokenSilent logic
        // msalApplication?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
        //     override fun onAccountLoaded(activeAccount: IAccount?) {
        //         if (activeAccount != null) {
        //             val authority = activeAccount.authority
        //             val parameters = AcquireTokenSilentParameters.Builder()
        //                 .withScopes(scopes.toList())
        //                 .forAccount(activeAccount)
        //                 .fromAuthority(authority)
        //                 .withCallback(callback as? AuthenticationCallback)
        //                 .build()
        //             msalApplication?.acquireTokenSilentAsync(parameters)
        //         } else {
        //             // Handle case where there is no account
        //              (callback as? AuthenticationCallback)?.onError(MsalException("No active account"))
        //         }
        //     }
        //
        //     override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
        //         // Handle account change
        //     }
        //
        //     override fun onError(exception: MsalException) {
        //         // Handle exception
        //         (callback as? AuthenticationCallback)?.onError(exception)
        //     }
        // })
    }

    fun getAccessToken(): String? {
        // TODO: Re-enable MSAL
        // return authenticationResult?.accessToken
        return null // Placeholder
    }
}
