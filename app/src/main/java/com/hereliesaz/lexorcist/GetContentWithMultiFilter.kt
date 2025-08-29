package com.hereliesaz.lexorcist

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class GetContentWithMultiFilter : ActivityResultContracts.GetContent() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, "*/*").apply {
            putExtra(Intent.EXTRA_MIME_TYPES, input)
        }
    }
}
