package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import com.whispercpp.whisper.WhisperContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject

class WhisperTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    private var whisperContext: WhisperContext? = null

    private suspend fun initialize() {
        if (whisperContext == null) {
            withContext(dispatcherProvider.io) {
                val modelsDir = File(context.filesDir, "models")
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs()
                }
                val modelFile = File(modelsDir, "ggml-base.en.bin")
                if (!modelFile.exists()) {
                    val modelUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin"
                    URL(modelUrl).openStream().use { input ->
                        modelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                whisperContext = WhisperContext.createContextFromFile(modelFile.absolutePath)
            }
        }
    }

    suspend fun transcribeVideo(uri: Uri): String {
        initialize()
        val audioData = extractAudioData(uri)
        return whisperContext?.transcribeData(audioData) ?: ""
    }

    private suspend fun extractAudioData(uri: Uri): FloatArray {
        return withContext(dispatcherProvider.io) {
            val extractor = android.media.MediaExtractor()
            extractor.setDataSource(context, uri, null)

            var audioTrack = -1
            var format: android.media.MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(android.media.MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrack = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrack == -1 || format == null) {
                extractor.release()
                return@withContext FloatArray(0)
            }

            extractor.selectTrack(audioTrack)

            val codec = android.media.MediaCodec.createDecoderByType(format.getString(android.media.MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = android.media.MediaCodec.BufferInfo()
            val outputBuffer = mutableListOf<Byte>()
            var isEOS = false

            while (!isEOS) {
                val inputBufferId = codec.dequeueInputBuffer(10000)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferId, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000)
                while (outputBufferId >= 0) {
                    val outputBufferData = codec.getOutputBuffer(outputBufferId)!!
                    val data = ByteArray(bufferInfo.size)
                    outputBufferData.get(data)
                    outputBuffer.addAll(data.toList())
                    codec.releaseOutputBuffer(outputBufferId, false)
                    outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000)
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            // Convert byte array to float array
            val shortArray = ShortArray(outputBuffer.size / 2)
            java.nio.ByteBuffer.wrap(outputBuffer.toByteArray()).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
            val floatArray = FloatArray(shortArray.size)
            for (i in shortArray.indices) {
                floatArray[i] = shortArray[i] / 32768.0f
            }
            floatArray
        }
    }


    fun release() {
        whisperContext?.release()
        whisperContext = null
    }
}
