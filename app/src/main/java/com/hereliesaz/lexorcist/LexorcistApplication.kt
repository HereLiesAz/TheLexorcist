package com.hereliesaz.lexorcist

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager // Added import
import com.hereliesaz.lexorcist.service.AppLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LexorcistApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var lifecycleObserver: AppLifecycleObserver

    // Keep the getter for Configuration.Provider if other parts of your app might query it directly
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.DEBUG) // Added for more verbose WorkManager logs
                .build()

    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(this, workManagerConfiguration)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }
}
