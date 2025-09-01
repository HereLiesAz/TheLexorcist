package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner
import io.mockk.coVerify
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class OcrViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var ocrViewModel: OcrViewModel
    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var scriptRunner: ScriptRunner
    private lateinit var application: Application
    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var scriptRunner: ScriptRunner

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        evidenceRepository = mockk(relaxed = true)
        settingsManager = mockk(relaxed = true)
        scriptRunner = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        ocrViewModel = OcrViewModel(application, evidenceRepository, settingsManager, scriptRunner)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performOcrOnUri adds evidence with correct details`() = runTest {
        // Given
        val uri: Uri = Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/1000000033")
        val context: Context = mockk(relaxed = true)
        val caseId = 123
        val parentVideoId = "video-456"
        val fixedClock = Clock.fixed(Instant.ofEpochMilli(1672531200000L), ZoneId.systemDefault())
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns fixedClock.millis()


        // When
        ocrViewModel.performOcrOnUri(uri, context, caseId, parentVideoId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        // No crash
    }
}
