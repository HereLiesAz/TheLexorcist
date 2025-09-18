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
    fun getExifDate(
        context: Context,
        uri: Uri,
    ): Long? { // Added explicit return type for clarity
        val inputStream = try {
            when (uri.scheme) {
                "content" -> context.contentResolver.openInputStream(uri)
                "file" -> uri.path?.let { FileInputStream(File(it)) }
                else -> {
                    // Fallback for URIs without a scheme, assuming it's a path
                    // This might happen if Uri.parse() was used on a raw path
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
        } catch (e: SecurityException) { // Catch potential security exceptions for file access
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
            } catch (e: IOException) { // Catch IOException from ExifInterface constructor or operations
                e.printStackTrace()
                null
            }
        }
    }
}
