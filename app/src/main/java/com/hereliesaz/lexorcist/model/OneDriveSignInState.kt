package com.hereliesaz.lexorcist.model

sealed class OneDriveSignInState {
    object Idle : OneDriveSignInState()
    object InProgress : OneDriveSignInState()
    data class Success(val accountName: String) : OneDriveSignInState()
    data class Error(val message: String, val exception: Exception?) : OneDriveSignInState()
}
