package com.hereliesaz.lexorcist

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LexorcistApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration // Changed to property
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
