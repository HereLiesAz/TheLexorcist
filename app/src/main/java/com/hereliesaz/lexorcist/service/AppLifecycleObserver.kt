package com.hereliesaz.lexorcist.service

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.di.qualifiers.ApplicationScope // Import the qualifier
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

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (settingsManager.getCloudSyncEnabled()) {
            applicationScope.launch(Dispatchers.IO) {
                storageService.synchronize()
            }
        }
    }
}
