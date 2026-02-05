package com.hereliesaz.lexorcist.di

import javax.inject.Qualifier

/**
 * Qualifier annotation for injecting the IO CoroutineDispatcher.
 * Use this when you specifically need the standard IO dispatcher (e.g. for network calls),
 * distinguishing it from other Dispatchers (like Default or Main).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IODispatcher
