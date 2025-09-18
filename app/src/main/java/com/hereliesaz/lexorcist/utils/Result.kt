package com.hereliesaz.lexorcist.utils

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException

sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<out T>(
        val data: T,
    ) : Result<T>()

    data class Error(
        val exception: Exception,
    ) : Result<Nothing>()

    data class UserRecoverableError(
        val exception: UserRecoverableAuthIOException,
    ) : Result<Nothing>()
}
