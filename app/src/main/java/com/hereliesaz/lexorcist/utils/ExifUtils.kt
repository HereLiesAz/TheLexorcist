package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

object ExifUtils {
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
            // Handle exception
        }
        return exifData
    }

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
