package com.hereliesaz.lexorcist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.WorkManager
import com.hereliesaz.lexorcist.service.LogService
import com.hereliesaz.lexorcist.utils.SETTINGS_DATASTORE_NAME
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

object Lexorcist {
    const val SHARED_PREFS_NAME = "LexorcistPrefs"
    const val DATABASE_NAME = "lexorcist.db"
    const val TAG = "Lexorcist"

    // Define a directory for case files within the app's persistent storage
    fun getCaseDirectory(context: Context, caseId: String): File {
        return File(context.filesDir, "cases/$caseId")
    }

    // Define the path for the Excel file within a specific case directory
    fun getExcelFilePath(context: Context, caseId: String): String {
        return File(getCaseDirectory(context, caseId), "$caseId.xlsx").absolutePath
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LexorcistEntryPoint { // RENAMED from LexorcistApp
        fun getDataStore(): DataStore<Preferences>
        fun getWorkManager(): WorkManager
        fun getLogService(): LogService
    }

    fun getAppEntryPoint(applicationContext: Context): LexorcistEntryPoint {
        return EntryPointAccessors.fromApplication(
            applicationContext,
            LexorcistEntryPoint::class.java // UPDATED to LexorcistEntryPoint
        )
    }
}
