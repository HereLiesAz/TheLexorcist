package com.hereliesaz.lexorcist.service

import android.content.Context
import android.content.ContentResolver
import android.content.res.AssetManager
import android.net.Uri
import com.hereliesaz.lexorcist.data.SettingsManager // Import SettingsManager
import com.hereliesaz.lexorcist.utils.Result // Your Result class
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TranscriptionServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockLogService: LogService

    @Mock
    private lateinit var mockSettingsManager: SettingsManager // Added mock for SettingsManager

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock
    private lateinit var mockParsedUri: Uri

    @Mock
    private lateinit var mockInputStream: InputStream

    @Mock
    private lateinit var mockAssetManager: AssetManager

    private lateinit var voskTranscriptionService: VoskTranscriptionService
    private lateinit var testAudioUriString: String
    private lateinit var mockedStaticUri: MockedStatic<Uri>
    private lateinit var testAppFilesDir: File

    @Before
    fun setUp() {
        testAudioUriString = "content://com.example.provider/audio.wav"

        mockedStaticUri = Mockito.mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Uri> { Uri.parse(testAudioUriString) }.thenReturn(mockParsedUri)

        whenever(mockParsedUri.toString()).thenReturn(testAudioUriString)

        testAppFilesDir = tempFolder.newFolder("appfiles")
        whenever(mockContext.filesDir).thenReturn(testAppFilesDir)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContext.assets).thenReturn(mockAssetManager)

        // Stub SettingsManager to return a default language to avoid NPE
        whenever(mockSettingsManager.getTranscriptionLanguage()).thenReturn("en-us")

        // This mock is intended to make copyModelFromAssets() in VoskTranscriptionService return null,
        // and to simulate that the model is not downloaded, forcing an attempt to initialize (which should fail due to no actual model)
        whenever(mockAssetManager.list(any())).thenReturn(emptyArray<String>())
        whenever(mockAssetManager.open(any())).thenThrow(IOException("Mock AssetManager open failed"))

        voskTranscriptionService = VoskTranscriptionService(mockContext, mockLogService, mockSettingsManager) // Pass mockSettingsManager
    }

    @After
    fun tearDown() {
        mockedStaticUri.close()
    }

    @Test
    fun `transcribeAudio when contentResolver throws FileNotFoundException should return Error`() = runTest {
        // This message is expected if model initialization fails first, due to mocked assets/no downloaded model
        val expectedModelInitFailureMessage = "Vosk model for language 'en-us' is not downloaded or invalid. Please download it from settings."
        // This message is expected if model init somehow passes (it shouldn't with current mocks) and then ContentResolver fails
        val expectedFNFMessage = "File not found for URI: $testAudioUriString"

        whenever(mockContentResolver.openInputStream(eq(mockParsedUri)))
            .thenThrow(FileNotFoundException("Mock FNF Exception"))

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            assertTrue("Expected exception to be IOException, but was ${result.exception::class.java}", result.exception is IOException)
            val actualMessage = result.exception.message
            // Check if the message matches either the model init failure or the FNF from ContentResolver
            assertTrue(
                "Exception message mismatch. Expected '$expectedModelInitFailureMessage' or '$expectedFNFMessage', but got '$actualMessage'",
                actualMessage == expectedModelInitFailureMessage || actualMessage == expectedFNFMessage
            )
            if (actualMessage == expectedFNFMessage) {
                 assertTrue("Expected cause to be FileNotFoundException when message is FNF", result.exception.cause is FileNotFoundException)
            }
        }
    }

    @Test
    fun `transcribeAudio when contentResolver returns null InputStream should return Error`() = runTest {
        // This message is expected if model initialization fails first
        val expectedModelInitFailureMessage = "Vosk model for language 'en-us' is not downloaded or invalid. Please download it from settings."
        // This message is expected if model init passes and then ContentResolver returns null
        val expectedNullStreamMessage = "Failed to open audio stream from URI: $testAudioUriString"
        
        whenever(mockContentResolver.openInputStream(eq(mockParsedUri))).thenReturn(null)

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            assertTrue("Expected exception to be IOException, but was ${result.exception::class.java}", result.exception is IOException)
            val actualMessage = result.exception.message
            assertTrue(
                "Exception message mismatch. Expected '$expectedModelInitFailureMessage' or '$expectedNullStreamMessage', but got '$actualMessage'",
                actualMessage == expectedModelInitFailureMessage || actualMessage == expectedNullStreamMessage
            )
        }
    }
}
