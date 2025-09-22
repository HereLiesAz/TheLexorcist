package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

object HashingUtils {

    fun getHash(context: Context, uri: Uri, algorithm: String = "SHA-256"): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance(algorithm)
                val buffer = ByteArray(8192)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
                val hashBytes = digest.digest()
                // Convert byte array to hex string
                hashBytes.joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            // In a real app, you'd want to log this error more formally
            e.printStackTrace()
            null
        }
    }
}
