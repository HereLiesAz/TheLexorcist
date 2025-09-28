package com.hereliesaz.lexorcist.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Interface to provide CoroutineDispatchers.
 * This makes it possible to inject dispatchers and replace them in tests.
 */
interface DispatcherProvider {
    fun main(): CoroutineDispatcher
    fun default(): CoroutineDispatcher
    fun io(): CoroutineDispatcher
    fun unconfined(): CoroutineDispatcher
}

/**
 * Standard implementation of [DispatcherProvider] that returns the default Coroutine dispatchers.
 */
class StandardDispatchers : DispatcherProvider {
    override fun main(): CoroutineDispatcher = Dispatchers.Main
    override fun default(): CoroutineDispatcher = Dispatchers.Default
    override fun io(): CoroutineDispatcher = Dispatchers.IO
    override fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
}