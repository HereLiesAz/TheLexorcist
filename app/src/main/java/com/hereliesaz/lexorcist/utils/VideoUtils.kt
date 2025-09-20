package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
// REMOVED: import com.google.common.collect.Multimaps.index
import com.hereliesaz.whisper.utils.WaveUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

object VideoUtils {

    @Throws(IOException::class)
    fun extractAudio(context: Context, videoUri: Uri): String {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, videoUri, null)

        val trackIndex = selectAudioTrack(extractor)
        if (trackIndex < 0) {
            throw IOException("No audio track found in video")
        }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)
        val codec = MediaCodec.createDecoderByType(mime!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmFile = File.createTempFile("audio", ".pcm", context.cacheDir)
        val fos = FileOutputStream(pcmFile)

        // REMOVED: val inputBuffers = codec.getInputBuffer(index())
        // REMOVED: val outputBuffers = codec.getOutputBuffer(index())
        val info = MediaCodec.BufferInfo()
        var isEOS = false

        while (!isEOS) {
            val inIndex = codec.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                val buffer: ByteBuffer? = codec.getInputBuffer(inIndex)
                if (buffer != null) { 
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                } else {
                    // TODO log an error if buffer is null, though inIndex >= 0 should mean it's valid
                }
            }

            var outIndex = codec.dequeueOutputBuffer(info, 10000)
            while (outIndex >= 0) {
                val buffer: ByteBuffer? = codec.getOutputBuffer(outIndex)
                if (buffer != null) { 
                    val chunk = ByteArray(info.size)
                    buffer.get(chunk)
                    buffer.clear()
                    fos.write(chunk)
                    codec.releaseOutputBuffer(outIndex, false)
                } else {
                     // TODO log an error if buffer is null
                }
                outIndex = codec.dequeueOutputBuffer(info, 10000)
            }
        }

        fos.close()
        codec.stop()
        codec.release()
        extractor.release()

        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val bitsPerSample = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            when (format.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
                android.media.AudioFormat.ENCODING_PCM_16BIT -> 16
                android.media.AudioFormat.ENCODING_PCM_8BIT -> 8
                android.media.AudioFormat.ENCODING_PCM_FLOAT -> 32
                else -> 16 // default
            }
        } else {
            16 // default
        }

        val wavFile = File(context.cacheDir, "temp_audio.wav")
        WaveUtil.convertPcmToWav(pcmFile, wavFile, numChannels, sampleRate, bitsPerSample)

        pcmFile.delete()

        return wavFile.absolutePath
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    @Throws(IOException::class)
    fun extractFrames(context: Context, videoUri: Uri): List<Uri> {
        val frameUris = mutableListOf<Uri>()
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationStr?.toLong() ?: 0

        for (i in 0 until duration step 1000) { // iterate every second (duration is in ms)
            val bitmap = retriever.getFrameAtTime(i * 1000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bitmap != null) {
                val frameFile = File.createTempFile("frame_", ".jpg", context.cacheDir)
                val fos = FileOutputStream(frameFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                fos.close()
                frameUris.add(Uri.fromFile(frameFile))
                bitmap.recycle() // Recycle bitmap after use
            }
        }

        retriever.release()
        return frameUris
    }
}
