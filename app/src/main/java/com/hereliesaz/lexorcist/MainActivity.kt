package com.hereliesaz.lexorcist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
// import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential // No longer created here
import com.hereliesaz.lexorcist.ui.MainScreen
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
// import kotlinx.coroutines.launch // May not be needed anymore

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
            // ViewModel now handles GoogleApiService creation
            account?.let { viewModel.onSignInResult(it, this) }
        } catch (e: ApiException) {
            // viewModel.onSignInFailed() // Method no longer exists
            Toast.makeText(this, "Sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Pass URI directly to ViewModel
            viewModel.addEvidence(it, this)
        }
    }

    private var imageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let {
                // Pass URI directly to ViewModel
                viewModel.addEvidence(it, this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/spreadsheets"),
                Scope("https://www.googleapis.com/auth/script.projects"),
                Scope("https://www.googleapis.com/auth/documents")
            )
            .requestEmail() // Requesting email, as ViewModel might use it via GoogleSignInAccount
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            LexorcistTheme {
                val navController = rememberNavController()
                MainScreen(
                    navController = navController,
                    mainViewModel = viewModel,
                    onSignInClick = { signIn() },
                    onExportClick = { viewModel.exportToSheet() } // Assuming MainViewModel has exportToSheet
                )
            }
        }
    }

    private fun takePicture() {
        val file = java.io.File(filesDir, "new_image.jpg")
        imageUri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        imageUri?.let { takePictureLauncher.launch(it) }
    }


    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // If account exists, ensure ViewModel is initialized with it
            viewModel.onSignInResult(account, this)
        } else {
            // viewModel.onSignOut() // Method no longer exists in MainViewModel
            // Handle sign-out UI or logic here if needed, or add a method to ViewModel
            // For now, if no account, nothing specific is done with ViewModel onStart
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // selectImage function seems to be unused now as MainScreen doesn't have a button for it directly.
    // Keeping it for now in case it's called from elsewhere, but it could be removed if not.
    private fun selectImage() {
        selectImageLauncher.launch("image/*")
    }

}
