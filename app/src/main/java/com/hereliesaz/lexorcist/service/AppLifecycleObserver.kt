package com.hereliesaz.lexorcist.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hereliesaz.lexorcist.di.qualifiers.ApplicationScope // Import the qualifier
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val storageService: com.hereliesaz.lexorcist.data.StorageService,
    private val settingsManager: com.hereliesaz.lexorcist.data.SettingsManager,
    @ApplicationScope private val applicationScope: CoroutineScope // Added qualifier
) : DefaultLifecycleObserver {

    private companion object {
        private const val TAG = "AppLifecycleObserver"
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (settingsManager.getCloudSyncEnabled()) {
            applicationScope.launch(Dispatchers.IO) {
                try {
                    when (val result = storageService.synchronize()) {
                        is Result.Error ->
                            Log.e(TAG, "Cloud sync failed", result.exception)
                        is Result.UserRecoverableError ->
                            Log.w(TAG, "Cloud sync needs user action (e.g. re-auth)", result.exception)
                        else ->
                            Log.i(TAG, "Cloud sync completed")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Cloud sync threw an unexpected exception", e)
                }
            }
        }
    }
}
