package com.hereliesaz.lexorcist.service

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hereliesaz.lexorcist.data.CaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val caseRepository: CaseRepository,
    private val applicationScope: CoroutineScope
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        applicationScope.launch(Dispatchers.IO) {
            caseRepository.synchronize()
        }
    }
}
