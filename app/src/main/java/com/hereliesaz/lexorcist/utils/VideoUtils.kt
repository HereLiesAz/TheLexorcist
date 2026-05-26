package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

object VideoUtils {

    fun extractFrames(context: Context, videoUri: Uri, intervalMs: Int): List<File> {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<File>()
        try {
            retriever.setDataSource(context, videoUri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0

            var currentTimeMs = 0L
            while (currentTimeMs < durationMs) {
                val bitmap = retriever.getFrameAtTime(currentTimeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    try {
                        frames.add(saveBitmapToFile(context, bitmap, "frame_${currentTimeMs}.jpg"))
                    } finally {
                        bitmap.recycle()
                    }
                }
                currentTimeMs += intervalMs
            }
        } finally {
            retriever.release()
        }
        return frames
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
        val outputDir = File(context.cacheDir, "frames")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val file = File(outputDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    fun extractAudio(context: Context, videoUri: Uri): String {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(videoUri, "r")
            ?: throw IOException("Could not open file descriptor for $videoUri")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        val outputFile = File.createTempFile("extracted_audio", ".m4a", context.cacheDir)
        try {
            extractor.setDataSource(parcelFileDescriptor.fileDescriptor)

            val audioTrackIndex = (0 until extractor.trackCount).firstOrNull {
                val format = extractor.getTrackFormat(it)
                val mime = format.getString(MediaFormat.KEY_MIME)
                mime?.startsWith("audio/") == true
            } ?: throw IOException("No audio track found in video")

            extractor.selectTrack(audioTrackIndex)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            val trackFormat = extractor.getTrackFormat(audioTrackIndex)
            val muxerTrackIndex = muxer.addTrack(trackFormat)
            muxer.start()

            while (!isEos) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    isEos = true
                } else {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }
            }

            muxer.stop()
            return outputFile.absolutePath
        } catch (e: Exception) {
            // Clean up the partial output file on failure.
            if (outputFile.exists()) outputFile.delete()
            throw e
        } finally {
            try { muxer?.release() } catch (_: Exception) { /* already released or never started */ }
            extractor.release()
            try { parcelFileDescriptor.close() } catch (_: Exception) { /* best-effort */ }
        }
    }
}
