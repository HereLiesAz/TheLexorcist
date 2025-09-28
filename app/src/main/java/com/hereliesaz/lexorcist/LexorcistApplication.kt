package com.hereliesaz.lexorcist

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.hereliesaz.lexorcist.data.StorageService
import com.hereliesaz.lexorcist.service.AppLifecycleObserver
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LexorcistApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var lifecycleObserver: AppLifecycleObserver

    @Inject
    lateinit var storageService: StorageService

    @Inject
    @com.hereliesaz.lexorcist.di.qualifiers.ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    override val workManagerConfiguration: Configuration // Changed to property
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()

        // Per AGENTS.md, synchronize on app load to ensure local data is up-to-date.
        applicationScope.launch(dispatcherProvider.io()) {
            storageService.synchronize()
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }
}
