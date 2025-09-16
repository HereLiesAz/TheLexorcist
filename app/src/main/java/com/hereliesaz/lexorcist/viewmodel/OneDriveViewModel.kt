package com.hereliesaz.lexorcist.viewmodel

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.auth.OneDriveAuthManager
import com.hereliesaz.lexorcist.model.OneDriveSignInState
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException
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
        oneDriveAuthManager.signIn(activity, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                val account = authenticationResult?.account
                if (account != null) {
                    _oneDriveSignInState.value = OneDriveSignInState.Success(account.username)
                    authenticationResult.accessToken?.let { storeOneDriveAccessToken(it) }
                } else {
                    _oneDriveSignInState.value = OneDriveSignInState.Error("Authentication successful but no account information received.", null)
                }
            }

            override fun onError(exception: MsalException?) {
                _oneDriveSignInState.value = OneDriveSignInState.Error("OneDrive sign-in failed.", exception)
            }

            override fun onCancel() {
                _oneDriveSignInState.value = OneDriveSignInState.Idle
            }
        })
    }

    private fun storeOneDriveAccessToken(accessToken: String) {
        sharedPreferences.edit {
            putString("onedrive_access_token", accessToken)
        }
    }
}
