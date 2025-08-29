package com.hereliesaz.lexorcist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.ui.ScriptEditorScreen
import com.hereliesaz.lexorcist.viewmodel.ScriptEditorViewModel

class ScriptEditorActivity : AppCompatActivity() {

    private var spreadsheetId: String? = null

    private val viewModel: ScriptEditorViewModel by viewModels {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(
                DriveScopes.DRIVE_FILE,
                SheetsScopes.SPREADSHEETS,
                "https://www.googleapis.com/auth/script.projects"
            )
        )
        // TODO: Replace with actual account name
        credential.selectedAccountName = "user@example.com"
        val googleApiService = GoogleApiService(credential)
        ScriptEditorViewModelFactory(googleApiService)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spreadsheetId = intent.getStringExtra("spreadsheetId")

        if (spreadsheetId == null) {
            Toast.makeText(this, "Spreadsheet ID not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel.loadScript(spreadsheetId!!)

        setContent {
            val scriptContent by viewModel.scriptContent.collectAsState()
            ScriptEditorScreen(
                scriptContent = scriptContent,
                onScriptContentChange = { viewModel.onScriptContentChange(it) },
                onSaveClick = { viewModel.saveScript(spreadsheetId!!) }
            )
        }
    }
}

class ScriptEditorViewModelFactory(private val googleApiService: GoogleApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScriptEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScriptEditorViewModel(googleApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
