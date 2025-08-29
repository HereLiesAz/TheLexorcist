package com.hereliesaz.lexorcist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hereliesaz.lexorcist.ui.ScriptEditorScreen
import com.hereliesaz.lexorcist.ui.theme.TheLexorcistTheme
import com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder
import com.hereliesaz.lexorcist.viewmodel.ScriptEditorViewModel

class ScriptEditorActivity : ComponentActivity() {

    private val viewModel: ScriptEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scriptId = intent.getStringExtra("scriptId")
        val googleApiService = GoogleApiServiceHolder.googleApiService

        if (scriptId == null || googleApiService == null) {
            // Handle error, maybe show a toast and finish
            finish()
            return
        }

        viewModel.initialize(googleApiService, scriptId)

        setContent {
            TheLexorcistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScriptEditorScreen(viewModel)
                }
            }
        }
    }
}
