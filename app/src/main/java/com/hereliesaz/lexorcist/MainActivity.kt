package com.hereliesaz.lexorcist

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
            val credential = GoogleAccountCredential.usingOAuth2(
                this, setOf(
                    "https://www.googleapis.com/auth/drive.file",
                    "https://www.googleapis.com/auth/spreadsheets",
                    "https://www.googleapis.com/auth/script.projects"
                )
            )
            credential.selectedAccountName = account.account?.name
            val googleApiService = GoogleApiService(credential, getString(R.string.app_name))
            viewModel.onSignInSuccess(googleApiService)
        } catch (e: ApiException) {
            viewModel.onSignInFailed()
            Toast.makeText(this, "Sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/spreadsheets"),
                Scope("https://www.googleapis.com/auth/script.projects")
            )
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            LexorcistTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSignIn = { signIn() },
                    onCreateMasterTemplate = { createMasterTemplate() },
                    onCreateCase = { caseName -> createCase(caseName) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && viewModel.googleApiService.value == null) {
            val credential = GoogleAccountCredential.usingOAuth2(this, setOf(
                "https://www.googleapis.com/auth/drive.file",
                "https://www.googleapis.com/auth/spreadsheets",
                "https://www.googleapis.com/auth/script.projects"
            ))
            credential.selectedAccountName = account.account?.name
            val googleApiService = GoogleApiService(credential, getString(R.string.app_name))
            viewModel.onSignInSuccess(googleApiService)
        } else if (account == null) {
            viewModel.onSignOut()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun getMasterTemplateId(): String? {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getString("masterTemplateId", null)
    }

    private fun createMasterTemplate() {
        lifecycleScope.launch {
            val templateId = viewModel.createMasterTemplate()
            if (templateId != null) {
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("masterTemplateId", templateId)
                    apply()
                }
                Toast.makeText(this@MainActivity, "Master Template created successfully.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to create master template.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createCase(caseName: String) {
        val masterTemplateId = getMasterTemplateId()
        if (masterTemplateId == null) {
            Toast.makeText(this, "Please create a master template first.", Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            val spreadsheetId = viewModel.createSpreadsheet(caseName)
            if (spreadsheetId != null) {
                Toast.makeText(this@MainActivity, "Case created: $spreadsheetId", Toast.LENGTH_LONG).show()
                viewModel.attachScript(spreadsheetId, masterTemplateId)
            } else {
                Toast.makeText(this@MainActivity, "Failed to create case.", Toast.LENGTH_LONG).show()
            }
        }
    }
}