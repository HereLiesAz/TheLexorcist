package com.hereliesaz.lexorcist.service

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import com.hereliesaz.lexorcist.utils.Result // Your Result class
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After // Added for cleaning up static mock
import org.junit.Test
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

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TranscriptionServiceTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockLogService: LogService

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock
    private lateinit var mockFilesDir: File

    @Mock // Mock for the Uri that Uri.parse will return
    private lateinit var mockParsedUri: Uri

    @Mock
    private lateinit var mockInputStream: InputStream

    private lateinit var voskTranscriptionService: VoskTranscriptionService
    private lateinit var testAudioUriString: String
    private lateinit var mockedStaticUri: MockedStatic<Uri> // To hold the static mock

    @Before
    fun setUp() {
        testAudioUriString = "content://com.example.provider/audio.wav"

        // Mock the static Uri.parse method
        // This needs to be done before Uri.parse is called if it's part of field initialization.
        // However, we are initializing mockParsedUri via Uri.parse inside the try-with-resources for safety.
        mockedStaticUri = Mockito.mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Uri> { Uri.parse(testAudioUriString) }.thenReturn(mockParsedUri)

        whenever(mockContext.filesDir).thenReturn(mockFilesDir)
        whenever(mockFilesDir.absolutePath).thenReturn("/data/user/0/com.hereliesaz.lexorcist/files")
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)

        voskTranscriptionService = VoskTranscriptionService(mockContext, mockLogService)
    }

    @After
    fun tearDown() {
        mockedStaticUri.close() // Close the static mock
    }

    @Test
    fun `transcribeAudio when contentResolver throws FileNotFoundException should return Error`() = runTest {
        whenever(mockContentResolver.openInputStream(eq(mockParsedUri)))
            .thenThrow(FileNotFoundException("Mock FNF Exception"))

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri) // Use the mocked Uri instance

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            val message = result.exception.message ?: ""
            assertTrue(
                "Error message mismatch. Got: $message",
                message.contains("Mock FNF Exception") || 
                message.contains("Failed to initialize Vosk model or recognizer") ||
                message.contains("Failed to open audio stream") 
            )
        }
    }

    @Test
    fun `transcribeAudio when contentResolver returns null InputStream should return Error`() = runTest {
        whenever(mockContentResolver.openInputStream(eq(mockParsedUri))).thenReturn(null)

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri) // Use the mocked Uri instance

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            val message = result.exception.message ?: ""
            assertTrue(
                "Error message mismatch. Got: $message",
                message.contains("Failed to open audio stream") ||
                message.contains("Failed to initialize Vosk model or recognizer")
            )
        }
    }
}
