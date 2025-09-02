package com.hereliesaz.lexorcist.model

// Data class to hold basic user information
data class UserInfo(
    val displayName: String?,
    val email: String?,
    val photoUrl: String? // Can be a String URL or Uri
)

sealed class SignInState {
    // Initial state or after sign out
    object Idle : SignInState()
    object InProgress : SignInState() // Sign-in process has started
    data class Success(val userInfo: UserInfo?) : SignInState() // Sign-in successful
    data class Error(val message: String, val exception: Exception? = null) : SignInState() // Sign-in failed
    // SignedOut can be represented by Idle, or explicitly if needed for specific UI
}
