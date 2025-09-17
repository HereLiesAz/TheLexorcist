package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.data.objectbox.SettingsObjectBox
import dagger.hilt.android.qualifiers.ApplicationContext
import io.objectbox.BoxStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val boxStore: BoxStore
) {

    private val settingsBox = boxStore.boxFor(SettingsObjectBox::class.java)

    init {
        if (settingsBox.isEmpty) {
            settingsBox.put(SettingsObjectBox())
        }
    }

    private fun getSettings(): SettingsObjectBox {
        return settingsBox.get(1) ?: SettingsObjectBox()
    }

    fun saveStorageLocation(uri: String) {
        val settings = getSettings().copy(storageLocation = uri)
        settingsBox.put(settings)
    }

    fun getStorageLocation(): String? = getSettings().storageLocation

    fun saveScript(script: String) {
        val settings = getSettings().copy(userScript = script)
        settingsBox.put(settings)
    }

    fun getScript(): String = getSettings().userScript

    fun saveTheme(theme: String) {
        val settings = getSettings().copy(theme = theme)
        settingsBox.put(settings)
    }

    fun getTheme(): String {
        return getSettings().theme
    }

    fun saveExportFormat(format: String) {
        val settings = getSettings().copy(exportFormat = format)
        settingsBox.put(settings)
    }

    fun getExportFormat(): String {
        return getSettings().exportFormat
    }

    fun saveCaseFolderPath(path: String) {
        val settings = getSettings().copy(caseFolderPath = path)
        settingsBox.put(settings)
    }

    fun getCaseFolderPath(): String? {
        return getSettings().caseFolderPath
    }

    fun saveCloudSyncEnabled(enabled: Boolean) {
        val settings = getSettings().copy(cloudSyncEnabled = enabled)
        settingsBox.put(settings)
    }

    fun getCloudSyncEnabled(): Boolean {
        return getSettings().cloudSyncEnabled
    }

    fun saveSelectedCloudProvider(provider: String) {
        val settings = getSettings().copy(selectedCloudProvider = provider)
        settingsBox.put(settings)
    }

    fun getSelectedCloudProvider(): String {
        return getSettings().selectedCloudProvider
    }
}
