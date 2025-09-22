package com.hereliesaz.lexorcist.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalLoadingState @Inject constructor() {

    private val loadingCount = AtomicInteger(0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun pushLoading() {
        if (loadingCount.incrementAndGet() > 0) {
            _isLoading.value = true
        }
    }

    fun popLoading() {
        if (loadingCount.decrementAndGet() <= 0) {
            _isLoading.value = false
        }
    }
}
