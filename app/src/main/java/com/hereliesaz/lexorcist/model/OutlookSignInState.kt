package com.hereliesaz.lexorcist.model

sealed class OutlookSignInState {
    object Idle : OutlookSignInState()
    object InProgress : OutlookSignInState()
    data class Success(val accountName: String?, val accessToken: String) : OutlookSignInState()
    data class Error(val message: String?, val exception: Throwable? = null) : OutlookSignInState()
}