package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.hereliesaz.whisper.utils.WaveUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

object VideoUtils {

    private const val TAG = "VideoUtils"

    @Throws(IOException::class)
    fun extractAudio(context: Context, videoUri: Uri): String {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, videoUri, null)

            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) {
                throw IOException("No audio track found in video: $videoUri")
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("Audio track MIME type is null for $videoUri")
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmFile = File.createTempFile("audio_", ".pcm", context.cacheDir ?: throw IOException("Cache directory is null"))
            
            FileOutputStream(pcmFile).use { fos ->
                val info = MediaCodec.BufferInfo()
                var isEOS = false

                while (!isEOS) {
                    val inIndex = codec.dequeueInputBuffer(10000L) // 10ms timeout
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
                            Log.w(TAG, "getInputBuffer returned null for index $inIndex")
                        }
                    }

                    var outIndex = codec.dequeueOutputBuffer(info, 10000L) // 10ms timeout
                    while (outIndex >= 0) {
                        val buffer: ByteBuffer? = codec.getOutputBuffer(outIndex)
                        if (buffer != null) {
                            val chunk = ByteArray(info.size)
                            buffer.get(chunk)
                            buffer.clear()
                            fos.write(chunk)
                            codec.releaseOutputBuffer(outIndex, false)
                        } else {
                            Log.w(TAG, "getOutputBuffer returned null for index $outIndex")
                        }
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isEOS = true // Ensure EOS is caught here as well
                        }
                        outIndex = codec.dequeueOutputBuffer(info, 10000L)
                    }
                }
            }


            codec.stop()
            codec.release()

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

            val wavFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
            WaveUtil.convertPcmToWav(pcmFile, wavFile, numChannels, sampleRate, bitsPerSample)

            if (!pcmFile.delete()) {
                Log.w(TAG, "Failed to delete temporary PCM file: ${pcmFile.absolutePath}")
            }

            return wavFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "IOException during audio extraction for $videoUri", e)
            throw e // Re-throw original IOException
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during audio extraction for $videoUri (codec issue likely)", e)
            throw IOException("Codec error during audio extraction for $videoUri: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during audio extraction for $videoUri", e)
            throw IOException("Failed to extract audio from $videoUri: ${e.message}", e)
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release MediaExtractor for $videoUri", e)
            }
        }
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
        val retriever = MediaMetadataRetriever()
        try {
            try {
                retriever.setDataSource(context, videoUri)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to setDataSource for $videoUri: Invalid URI or format not supported.", e)
                throw IOException("Failed to setDataSource for $videoUri: ${e.message}", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to setDataSource for $videoUri: Permission denied.", e)
                throw IOException("Failed to setDataSource for $videoUri due to security exception: ${e.message}", e)
            } catch (e: RuntimeException) { // Catch other potential runtime exceptions from setDataSource
                Log.e(TAG, "Failed to setDataSource for $videoUri: RuntimeException.", e)
                throw IOException("Failed to setDataSource for $videoUri: ${e.message}", e)
            }


            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull()
            if (durationMs == null || durationMs <= 0) {
                Log.w(TAG, "Could not extract valid duration for $videoUri. Duration string: '$durationStr'. Extracting a few initial frames attempt.")
                // Attempt to extract a few frames even if duration is unknown, common for some formats/streams
                for (i in 0 until 5) { // Try to get 5 frames at 1 second intervals
                     extractAndSaveFrame(context, retriever, i * 1000L * 1000L, videoUri, frameUris) // time in microseconds
                }
                if (frameUris.isEmpty()) {
                    throw IOException("Video duration is unknown or zero, and no initial frames could be extracted for $videoUri.")
                }
                return frameUris // Return whatever frames were extracted
            }
            
            // Iterate every second (duration is in ms, getFrameAtTime takes microseconds)
            val intervalUs = 1000 * 1000L // 1 second in microseconds
            for (timeUs in 0L until durationMs * 1000L step intervalUs) {
                extractAndSaveFrame(context, retriever, timeUs, videoUri, frameUris)
            }
            if (frameUris.isEmpty() && durationMs > 0) {
                 Log.w(TAG, "No frames extracted from $videoUri even though duration was $durationMs ms.")
            }

        } catch (e: IOException) {
            // Logged by extractAndSaveFrame or thrown directly
            throw e
        } catch (e: RuntimeException) {
            // Catch-all for other unexpected errors during the process
            Log.e(TAG, "Runtime error extracting frames from $videoUri", e)
            throw IOException("Failed to extract frames from $videoUri due to runtime error: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to release MediaMetadataRetriever for $videoUri", e)
                // Not re-throwing here as we want to preserve the original exception if one occurred
            }
        }
        return frameUris
    }

    private fun extractAndSaveFrame(
        context: Context,
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        videoUri: Uri,
        frameUris: MutableList<Uri>
    ) {
        var bitmap: Bitmap? = null
        try {
            bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bitmap != null) {
                val cacheDir = context.cacheDir ?: throw IOException("Cache directory is null, cannot save frame.")
                if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                    throw IOException("Cache directory does not exist and could not be created: ${cacheDir.absolutePath}")
                }
                val frameFile = File.createTempFile("frame_${System.nanoTime()}_", ".jpg", cacheDir)
                FileOutputStream(frameFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                frameUris.add(Uri.fromFile(frameFile))
            } else {
                Log.w(TAG, "getFrameAtTime returned null for $videoUri at ${timeUs}us")
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError while extracting frame at ${timeUs}us for $videoUri. Skipping frame.", e)
            // Optionally, try to recover, e.g., by reducing frame size or skipping, but for now just log and continue.
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException while extracting frame at ${timeUs}us for $videoUri. Skipping frame.", e)
            // This can happen with getFrameAtTime for various reasons.
        } catch (e: IOException) {
            Log.e(TAG, "IOException while saving frame at ${timeUs}us for $videoUri. Skipping frame.", e)
            // Re-throw critical IOExceptions like cache dir issues.
             if (e.message?.contains("Cache directory") == true) throw e
        } finally {
            bitmap?.recycle() // Recycle bitmap after use
        }
    }
}
