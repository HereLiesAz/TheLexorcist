package com.hereliesaz.lexorcist.viewmodel

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.auth.OneDriveAuthManager
import com.hereliesaz.lexorcist.model.OneDriveSignInState
// import com.microsoft.identity.client.AuthenticationCallback // TODO: Re-enable MSAL
// import com.microsoft.identity.client.IAuthenticationResult // TODO: Re-enable MSAL
// import com.microsoft.identity.client.ISingleAccountPublicClientApplication // TODO: Re-enable MSAL
// import com.microsoft.identity.client.exception.MsalException // TODO: Re-enable MSAL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OneDriveViewModel @Inject constructor(
    private val oneDriveAuthManager: OneDriveAuthManager,
    private val sharedPreferences: SharedPreferences,
    application: Application
) : AndroidViewModel(application) {

    private val _oneDriveSignInState = MutableStateFlow<OneDriveSignInState>(OneDriveSignInState.Idle)
    val oneDriveSignInState: StateFlow<OneDriveSignInState> = _oneDriveSignInState.asStateFlow()

    fun connectToOneDrive(activity: Activity) {
        _oneDriveSignInState.value = OneDriveSignInState.InProgress
        // TODO: Re-enable MSAL and use proper AuthenticationCallback type
        // oneDriveAuthManager.signIn(activity, object : Any /* AuthenticationCallback */ {
        //     fun onSuccess(authenticationResult: Any? /* IAuthenticationResult? */) {
        //         // val account = (authenticationResult as? IAuthenticationResult)?.account // TODO: Re-enable MSAL
        //         // if (account != null) {
        //         //     _oneDriveSignInState.value = OneDriveSignInState.Success(account.username)
        //         //     (authenticationResult as? IAuthenticationResult)?.accessToken?.let { storeOneDriveAccessToken(it) }
        //         // } else {
        //         //     _oneDriveSignInState.value = OneDriveSignInState.Error("Authentication successful but no account information received.", null)
        //         // }
        //         _oneDriveSignInState.value = OneDriveSignInState.Error("MSAL Connect to OneDrive: Not implemented", null) // Placeholder
        //     }
        //
        //     fun onError(exception: Any? /* MsalException? */) {
        //         _oneDriveSignInState.value = OneDriveSignInState.Error("OneDrive sign-in failed.", exception as? Exception)
        //     }
        //
        //     fun onCancel() {
        //         _oneDriveSignInState.value = OneDriveSignInState.Idle
        //     }
        // })
         _oneDriveSignInState.value = OneDriveSignInState.Error("MSAL Connect to OneDrive: Not implemented", null) // Placeholder
    }

    private fun storeOneDriveAccessToken(accessToken: String) {
        sharedPreferences.edit {
            putString("onedrive_access_token", accessToken)
        }
    }

    fun disconnectFromOneDrive() {
        // TODO: Re-enable MSAL and use proper ISingleAccountPublicClientApplication.SignOutCallback type
        // oneDriveAuthManager.signOut(object : Any /* ISingleAccountPublicClientApplication.SignOutCallback */ {
        //     fun onSignOut() {
        //         _oneDriveSignInState.value = OneDriveSignInState.Idle
        //         clearOneDriveAccessToken()
        //     }
        //
        //     fun onError(exception: Any? /* MsalException */) {
        //         _oneDriveSignInState.value = OneDriveSignInState.Error("OneDrive sign-out failed.", exception as? Exception)
        //     }
        // })
        _oneDriveSignInState.value = OneDriveSignInState.Idle // Placeholder
        clearOneDriveAccessToken() // Placeholder
    }

    private fun clearOneDriveAccessToken() {
        sharedPreferences.edit {
            remove("onedrive_access_token")
        }
    }
}
