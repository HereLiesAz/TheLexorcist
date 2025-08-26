package com.hereliesaz.lexorcist

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
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

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
            viewModel.onImageSelected(bitmap)
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
                    onSelectImage = { selectImage() }
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

    private fun selectImage() {
        selectImageLauncher.launch("image/*")
    }

}