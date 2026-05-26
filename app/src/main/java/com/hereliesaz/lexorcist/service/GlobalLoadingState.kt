package com.hereliesaz.lexorcist.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalLoadingState @Inject constructor() {

    private var loadingCount = 0

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Synchronized so the counter update and the StateFlow update are atomic together,
    // preventing interleaved push/pop from leaving isLoading inconsistent with the count.
    @Synchronized
    fun pushLoading() {
        loadingCount++
        _isLoading.value = loadingCount > 0
    }

    @Synchronized
    fun popLoading() {
        if (loadingCount > 0) loadingCount--
        _isLoading.value = loadingCount > 0
    }
}
