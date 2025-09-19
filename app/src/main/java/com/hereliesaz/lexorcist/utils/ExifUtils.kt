package com.hereliesaz.lexorcist.utils // Corrected package

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File // Import File
import java.io.FileInputStream // Import FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

object ExifUtils {

    fun getExifData(context: Context, uri: Uri): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val allTags = listOf(
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_IMAGE_WIDTH,
                    ExifInterface.TAG_IMAGE_LENGTH
                )

                allTags.forEach { tag ->
                    exifInterface.getAttribute(tag)?.let { value ->
                        metadata[tag] = value
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return metadata
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: IOException) {
            0L
        }
    }

    fun getExifDate(
        context: Context,
        uri: Uri,
    ): Long? {
        val inputStream = try {
            when (uri.scheme) {
                "content" -> context.contentResolver.openInputStream(uri)
                "file" -> uri.path?.let { FileInputStream(File(it)) }
                else -> {
                    if (uri.path != null) {
                        FileInputStream(File(uri.path!!))
                    } else {
                        null
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } catch (e: SecurityException) {
            e.printStackTrace()
            return null
        }

        return inputStream?.use { stream ->
            try {
                val exifInterface = ExifInterface(stream)
                val dateString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                if (dateString != null) {
                    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    format.parse(dateString)?.time
                } else {
                    null
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}
