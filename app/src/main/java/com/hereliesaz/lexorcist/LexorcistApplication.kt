package com.hereliesaz.lexorcist

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.hereliesaz.lexorcist.data.StorageService
import com.hereliesaz.lexorcist.service.AppLifecycleObserver
import com.hereliesaz.lexorcist.service.DefaultExtrasSeeder
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The main application class for the Lexorcist app.
 *
 * This class serves as the entry point for the application process and is responsible for:
 * 1. Initializing Hilt for Dependency Injection via `@HiltAndroidApp`.
 * 2. Configuring WorkManager for background tasks.
 * 3. Handling application-wide initialization logic (e.g., seeding data, synchronization).
 * 4. Registering lifecycle observers.
 */
@HiltAndroidApp
class LexorcistApplication :
    Application(),
    Configuration.Provider { // Implements Configuration.Provider to customize WorkManager initialization.

    /**
     * Factory for creating Hilt-injected Workers.
     * Required for injecting dependencies into Worker classes.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Observer to track application lifecycle events (foreground/background).
     * Used for managing resources or pausing/resuming tasks based on app visibility.
     */
    @Inject
    lateinit var lifecycleObserver: AppLifecycleObserver

    /**
     * Service responsible for handling data storage and synchronization logic.
     */
    @Inject
    lateinit var storageService: StorageService

    /**
     * A CoroutineScope tied to the application's lifecycle.
     * Operations launched here will survive Activity/Fragment destruction but are cancelled when the app process dies.
     * Injected with `@ApplicationScope` qualifier.
     */
    @Inject
    @com.hereliesaz.lexorcist.di.qualifiers.ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /**
     * Provider for Coroutine Dispatchers, ensuring easy swapping for testing (e.g., IO vs TestDispatcher).
     */
    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    /**
     * Service to populate the database or shared storage with default scripts/templates ("Extras").
     */
    @Inject
    lateinit var defaultExtrasSeeder: DefaultExtrasSeeder

    /**
     * Provides the configuration for WorkManager.
     * We use a custom configuration to plug in the [HiltWorkerFactory], allowing dependency injection in Workers.
     */
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    /**
     * Called when the application is starting, before any activity, service, or receiver objects have been created.
     */
    override fun onCreate() {
        super.onCreate()

        // Per AGENTS.md, synchronize on app load to ensure local data is up-to-date with cloud storage (if enabled).
        // This is launched in the application scope on the IO dispatcher to avoid blocking the main thread.
        applicationScope.launch(dispatcherProvider.io()) {
            storageService.synchronize()
        }

        // Seed the default extras (scripts, templates) to the shared spreadsheet or local storage if they haven't been already.
        // This ensures the user has the base set of tools available immediately.
        applicationScope.launch(dispatcherProvider.io()) {
            defaultExtrasSeeder.seedDefaultExtrasIfNeeded()
        }

        // Register the AppLifecycleObserver to listen for whole-application lifecycle events.
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }
}
