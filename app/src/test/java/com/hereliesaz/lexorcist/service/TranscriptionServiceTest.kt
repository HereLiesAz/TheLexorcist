package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayInputStream
import java.io.InputStream

class TranscriptionServiceTest {

    private lateinit var context: Context
    private lateinit var service: TranscriptionService

    private val logService: LogService = mock()

    @Before
    fun setup() {
        context = mock()
        service = TranscriptionService(context, logService)
    }

    @Test
    fun testTranscriptionLogic() = runBlocking {
        // Given
        val uri: Uri = mock()
        val inputStream: InputStream = ByteArrayInputStream("test".toByteArray())
        whenever(context.contentResolver.openInputStream(uri)).thenReturn(inputStream)

        val recognizer: Recognizer = mock()
        whenever(recognizer.acceptWaveForm(any(), any())).thenReturn(true, true, false)
        whenever(recognizer.result).thenReturn("""{"partial" : "hello"}""", """{"partial" : " world"}""")
        whenever(recognizer.finalResult).thenReturn("""{"text" : "hello world"}""")

        val model: Model = mock()
        val serviceWithMockedRecognizer = object : TranscriptionService(context, logService) {
            override suspend fun transcribeAudio(uri: Uri): Pair<String, String?> {
                val result = transcribeInputStream(recognizer, inputStream)
                return Pair(result, null)
            }
        }

        // When
        val (transcript, error) = serviceWithMockedRecognizer.transcribeAudio(uri)

        // Then
        assertEquals("hello world", transcript)
        assertEquals(null, error)
    }
}
