package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class PackagingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend fun createArchive(files: List<File>, destinationUri: Uri) {
        withContext(dispatcherProvider.io()) {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        files.forEach { file ->
                            zos.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            } catch (e: Exception) {
                // Remove the partially-written archive so the user isn't left with a corrupt file.
                try {
                    DocumentsContract.deleteDocument(context.contentResolver, destinationUri)
                } catch (_: Exception) { /* best-effort cleanup */ }
                throw e
            }
        }
    }
}
