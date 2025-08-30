package com.hereliesaz.lexorcist.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

object ExifUtils {

    fun getExifDate(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val dateString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                if (dateString != null) {
                    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    format.parse(dateString)?.time
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
