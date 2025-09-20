package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager // Added missing import
import com.hereliesaz.lexorcist.service.ScriptRunner // Added missing import
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
// import dagger.hilt.android.testing.UninstallModules // Removed this line
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltAndroidTest
// @UninstallModules(TestAppModule::class) // This line was removed
class OcrViewModelInstrumentationTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var evidenceRepository: EvidenceRepository

    @Inject // Added inject for SettingsManager
    lateinit var settingsManager: SettingsManager

    @Inject // Added inject for ScriptRunner
    lateinit var scriptRunner: ScriptRunner

    private lateinit var ocrViewModel: OcrViewModel
    private lateinit var application: Application

    @Before
    fun setup() {
        hiltRule.inject()
        application = ApplicationProvider.getApplicationContext()
        // Constructor now uses injected dependencies
        ocrViewModel = OcrViewModel(application, evidenceRepository, settingsManager, scriptRunner)
    }

    @Test
    fun performOcrOnUri_addsEvidenceWithCorrectDetails() =
        runTest {
            // Given
            val uri: Uri = Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/1000000033")
            val context: Context = application.applicationContext
            val caseId = 123
            val spreadsheetId = "testSpreadsheetId" // Added for consistency with ViewModel call
            val parentVideoId = "video-456"
            // val expectedTimestamp = 1672531200000L // Timestamps are hard to test reliably here
            // val expectedDocumentDate = 1672521200000L

            // When
            // Updated to pass spreadsheetId and align with a likely ViewModel method signature
            ocrViewModel.performOcrOnUri(uri, context, caseId.toLong(), spreadsheetId, parentVideoId)

            // Then
            verify {
                evidenceRepository.addEvidence(
                    withArg { evidence ->
                        org.junit.Assert.assertEquals(uri.toString(), evidence.sourceDocument)
                        org.junit.Assert.assertEquals(caseId.toLong(), evidence.caseId)
                        org.junit.Assert.assertEquals(parentVideoId, evidence.parentVideoId)
                        org.junit.Assert.assertEquals(spreadsheetId, evidence.spreadsheetId) // Verify spreadsheetId
                        // Timestamps will be checked in the unit test where we can control the clock
                    },
                )
            }
        }
}
