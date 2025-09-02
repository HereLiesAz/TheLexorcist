package com.hereliesaz.lexorcist

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class GetContentWithMultiFilter(
    private val mimeTypes: Array<String>,
) : ActivityResultContracts.GetContent() {
    override fun createIntent(
        context: Context,
        input: String,
    ): Intent {
        // 'input' is the single MIME type string required by the superclass method.
        // We'll use the 'input' provided at launch for super.createIntent(),
        // and then specify the richer set of MIME types via EXTRA_MIME_TYPES from our constructor.
        return super.createIntent(context, input).apply {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }
}
