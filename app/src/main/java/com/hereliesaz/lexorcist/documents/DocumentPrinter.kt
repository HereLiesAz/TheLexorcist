package com.hereliesaz.lexorcist.documents

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

/**
 * Hands a generated document to Android's print service.
 *
 * The print dialog it opens includes "Save as PDF", so this is also how a
 * finished PDF is produced. Going through the platform rather than rendering
 * pages here means pagination, margins and paper size are handled by code that
 * does it correctly, and the user picks the destination.
 *
 * Needs an Activity context: the print dialog is a UI, and `PrintManager`
 * refuses an application context.
 */
object DocumentPrinter {

    /**
     * @param activityContext an Activity, not the application context.
     * @param onReady invoked once the print job has been handed off, or with
     *   the failure if the document could not be loaded.
     */
    fun print(
        activityContext: Context,
        document: GeneratedDocument,
        onReady: (Result<Unit>) -> Unit = {},
    ) {
        val printManager = activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            onReady(Result.failure(IllegalStateException("Printing is not available on this device")))
            return
        }

        val webView = WebView(activityContext)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    printManager.print(
                        document.title,
                        view.createPrintDocumentAdapter(document.title),
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                            .build(),
                    )
                    onReady(Result.success(Unit))
                } catch (e: Exception) {
                    onReady(Result.failure(e))
                }
            }
        }
        webView.loadDataWithBaseURL(
            null,
            readHtml(document.html),
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun readHtml(file: File): String = file.readText()
}
