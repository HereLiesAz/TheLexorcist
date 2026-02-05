package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

/**
 * Utility class for extracting EXIF metadata from media files.
 */
object ExifUtils {
    /**
     * Extracts the original creation date from the image metadata.
     *
     * @param context The application context.
     * @param uri The URI of the image file.
     * @return The timestamp in milliseconds, or null if not found or on error.
     */
    fun getExifDate(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
                    val format = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                    format.parse(it)?.time
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Extracts a set of common EXIF tags from the image metadata.
     *
     * @param context The application context.
     * @param uri The URI of the image file.
     * @return A map of EXIF tag names to their values.
     */
    fun getExifData(context: Context, uri: Uri): Map<String, String> {
        val exifData = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val tags = arrayOf(
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_IMAGE_LENGTH,
                    ExifInterface.TAG_IMAGE_WIDTH,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL
                )
                for (tag in tags) {
                    val value = exifInterface.getAttribute(tag)
                    if (value != null) {
                        exifData[tag] = value
                    }
                }
            }
        } catch (e: IOException) {
            // Log error or ignore
        }
        return exifData
    }

    /**
     * Retrieves the file size of the content at the given URI.
     *
     * @param context The application context.
     * @param uri The URI of the file.
     * @return The file size in bytes, or 0 if not determined.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
