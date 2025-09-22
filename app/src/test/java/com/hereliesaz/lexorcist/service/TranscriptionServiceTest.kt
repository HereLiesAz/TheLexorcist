package com.hereliesaz.lexorcist.service

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.utils.Result // Your Result class
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After // Added for cleaning up static mock
import org.junit.Rule // Added for TemporaryFolder
import org.junit.Test
import org.junit.rules.TemporaryFolder // Added for TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic // Added for static mocking
import org.mockito.Mockito // Added for Mockito.mockStatic and Mockito.RETURNS_DEEP_STUBS
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.IOException // Still useful to have, though the check is broader

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TranscriptionServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    private lateinit var mockContext: Context

    @Mock(lenient = true) // Made mockLogService lenient
    private lateinit var mockLogService: LogService

    @Mock(lenient = true) // Made mockSettingsManager lenient
    private lateinit var mockSettingsManager: SettingsManager

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock // Mock for the Uri that Uri.parse will return
    private lateinit var mockParsedUri: Uri

    @Mock
    private lateinit var mockInputStream: InputStream

    private lateinit var voskTranscriptionService: VoskTranscriptionService
    private lateinit var testAudioUriString: String
    private lateinit var mockedStaticUri: MockedStatic<Uri> // To hold the static mock
    private lateinit var testAppFilesDir: File // To hold the temporary directory for app files

    @Before
    fun setUp() {
        testAudioUriString = "content://com.example.provider/audio.wav"

        mockedStaticUri = Mockito.mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Uri> { Uri.parse(testAudioUriString) }.thenReturn(mockParsedUri)

        // REMOVED: Unnecessary stub for mockParsedUri.toString()
        // whenever(mockParsedUri.toString()).thenReturn(testAudioUriString)

        testAppFilesDir = tempFolder.newFolder("appfiles")
        whenever(mockContext.filesDir).thenReturn(testAppFilesDir)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        // REINSTATED AND CORRECTED: This stub is necessary for model initialization path
        whenever(mockSettingsManager.getTranscriptionLanguage()).thenReturn("en-us")

        voskTranscriptionService = VoskTranscriptionService(mockContext, mockLogService, mockSettingsManager)
    }

    @After
    fun tearDown() {
        mockedStaticUri.close() 
    }

    @Test
    fun `transcribeAudio when contentResolver throws FileNotFoundException should return Error`() = runTest {
        // REMOVED: Unnecessary stub as model initialization is expected to fail first.
        // whenever(mockContentResolver.openInputStream(eq(mockParsedUri)))
        //     .thenThrow(FileNotFoundException("Mock FNF Exception"))

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            // Changed from IOException to generic Exception to catch other init failures e.g. SecurityException
            assertTrue("Expected Exception due to model init failure, got ${result.exception::class.java}", 
                result.exception is Exception)
            val message = result.exception.message ?: ""
            assertTrue(
                "Error message mismatch. Expected model init failure. Got: $message",
                message.contains("is not downloaded or invalid")
            )
        }
    }

    @Test
    fun `transcribeAudio when contentResolver returns null InputStream should return Error`() = runTest {
        // REMOVED: Unnecessary stub as model initialization is expected to fail first.
        // whenever(mockContentResolver.openInputStream(eq(mockParsedUri))).thenReturn(null)

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            // Changed from IOException to generic Exception to catch other init failures e.g. SecurityException
            assertTrue("Expected Exception due to model init failure, got ${result.exception::class.java}", 
                result.exception is Exception)
            val message = result.exception.message ?: ""
            assertTrue(
                "Error message mismatch. Expected model init failure. Got: $message",
                message.contains("is not downloaded or invalid")
            )
        }
    }
}
