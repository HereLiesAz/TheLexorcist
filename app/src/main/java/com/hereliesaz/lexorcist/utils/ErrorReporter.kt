package com.hereliesaz.lexorcist.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorReporter @Inject constructor() {

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    fun reportError(throwable: Throwable) {
        _error.value = throwable
    }

    fun clearError() {
        _error.value = null
    }
}
