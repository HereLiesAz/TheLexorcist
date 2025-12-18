package com.hereliesaz.lexorcist.service.nlp

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterFactory
import java.io.ByteArrayInputStream
import java.io.FileDescriptor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@RunWith(MockitoJUnitRunner::class)
class LegalBertServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var assetManager: AssetManager

    @Mock
    private lateinit var interpreter: InterpreterApi

    @Mock
    private lateinit var assetFileDescriptor: AssetFileDescriptor

    @Mock
    private lateinit var fileDescriptor: FileDescriptor

    private lateinit var legalBertService: LegalBertService

    private var interpreterFactoryMock: MockedConstruction<InterpreterFactory>? = null
    private var fileInputStreamMock: MockedConstruction<FileInputStream>? = null

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        `when`(context.assets).thenReturn(assetManager)

        // Mock vocab.txt
        val vocabContent = "[PAD]\n[CLS]\n[SEP]\n[UNK]\nhello\nworld\n"
        `when`(assetManager.open("vocab.txt")).thenReturn(ByteArrayInputStream(vocabContent.toByteArray()))

        // Mock legal_bert.tflite
        `when`(assetManager.openFd("legal_bert.tflite")).thenReturn(assetFileDescriptor)
        `when`(assetFileDescriptor.fileDescriptor).thenReturn(fileDescriptor)
        `when`(assetFileDescriptor.startOffset).thenReturn(0)
        `when`(assetFileDescriptor.declaredLength).thenReturn(10)

        // Mock InterpreterFactory Construction
        interpreterFactoryMock = Mockito.mockConstruction(InterpreterFactory::class.java) { mock, _ ->
            `when`(mock.create(any(ByteBuffer::class.java), any(InterpreterApi.Options::class.java)))
                .thenReturn(interpreter)
        }

        // Mock FileInputStream Construction
        fileInputStreamMock = Mockito.mockConstruction(FileInputStream::class.java) { mock, _ ->
            val mockChannel = mock(FileChannel::class.java)
            val mappedByteBuffer = mock(MappedByteBuffer::class.java)
            `when`(mock.channel).thenReturn(mockChannel)
            `when`(mockChannel.map(any(), anyLong(), anyLong())).thenReturn(mappedByteBuffer)
        }

        // Initialize service
        legalBertService = LegalBertService(context)
    }

    @After
    fun tearDown() {
        interpreterFactoryMock?.close()
        fileInputStreamMock?.close()
    }

    @Test
    fun `getEmbedding should return correct float array`() {
        val text = "hello world"

        // Setup mock output behavior for interpreter
        Mockito.doAnswer { invocation ->
            val outputs = invocation.arguments[1] as MutableMap<Int, Any>
            val outputArray = outputs[0] as Array<Array<FloatArray>>
            outputArray[0][0][0] = 0.5f
            null
        }.`when`(interpreter).runForMultipleInputsOutputs(any(), any())

        val result = legalBertService.getEmbedding(text)

        assertNotNull(result)
        assertEquals(768, result.size)
        assertEquals(0.5f, result[0], 0.0001f)

        // Verify inputs
        val inputsCaptor = ArgumentCaptor.forClass(Array<Any>::class.java)
        verify(interpreter).runForMultipleInputsOutputs(inputsCaptor.capture(), any())

        val inputs = inputsCaptor.value
        assertEquals(3, inputs.size)

        val inputIds = inputs[0] as ByteBuffer
        assertEquals(128 * 4, inputIds.capacity())
    }
}
