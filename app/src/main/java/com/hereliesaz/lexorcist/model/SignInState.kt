package com.hereliesaz.lexorcist.model

// Data class to hold basic user information
data class UserInfo(
    val displayName: String?,
    val email: String?,
    // Can be a String URL or Uri
    val photoUrl: String?,
)

sealed class SignInState {
    // Initial state or after sign out
    object Idle : SignInState()

    // Sign-in process has started
    object InProgress : SignInState()

    // Sign-in successful
    data class Success(
        val userInfo: UserInfo?,
    ) : SignInState()

    // Sign-in failed
    data class Error(
        val message: String,
        val exception: Exception? = null,
    ) : SignInState()
    // SignedOut can be represented by Idle, or explicitly if needed for specific UI
}
