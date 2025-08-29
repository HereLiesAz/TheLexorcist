@file:Suppress("deprecation") // Using lowercase "deprecation"
package com.hereliesaz.lexorcist

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.activity.viewModels
import androidx.compose.runtime.Composable // Keep: General Compose import
import androidx.navigation.NavController // Keep: MainScreen might use it internally
import androidx.navigation.compose.rememberNavController // Keep: MainScreen or theme might use it
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.common.api.ApiException
import com.hereliesaz.lexorcist.MainScreen
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
// Removed DataReviewViewModel and placeholder/settings screen imports as they belonged to the NavHost block

class MainActivity : ComponentActivity() {

    // This is the "top" block viewModel
    private val viewModel: MainViewModel by viewModels()
    
    // These are the "top" block client/request declarations
    private lateinit var oneTapClient: SignInClient 
    private lateinit var signUpRequest: BeginSignInRequest
    private lateinit var signInRequest: BeginSignInRequest

    // This is the "top" block APP_TAG
    private val APP_TAG = "MainActivity"

    // This is the "top" block signInLauncher
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                val email = credential.id 
                val displayName = credential.displayName
                Toast.makeText(this, "Signed in as ${email ?: displayName}", Toast.LENGTH_SHORT).show()
                val applicationName = applicationInfo.loadLabel(packageManager).toString()
                viewModel.onSignInResult(idToken, email, this, applicationName)
            } catch (e: ApiException) {
                Log.e(APP_TAG, "Sign-in failed after result: ${e.statusCode}", e)
                Toast.makeText(this, "Sign in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(APP_TAG, "Sign-in flow was cancelled or failed. Result code: ${result.resultCode}")
            Toast.makeText(this, "Sign in cancelled or failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Kept from "top" block
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.addEvidenceToUiList(it, this)
        }
    }

    // Kept from "top" block
    private var imageUri: Uri? = null

    // Kept from "top" block
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let {
                viewModel.addEvidenceToUiList(it, this)
            }
        }
    }

    // This is the "top" block onCreate
    override fun onCreate(savedInstanceState: Bundle?) { 
        super.onCreate(savedInstanceState)
        oneTapClient = Identity.getSignInClient(this)
        val serverClientId = getString(R.string.default_web_client_id) 
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()

        setContent { 
            // The SettingsManager and theme logic from the "top" block's setContent is used here.
            val settingsManager = com.hereliesaz.lexorcist.data.SettingsManager(this)
            val theme = settingsManager.getTheme()
            val darkTheme = when (theme) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            LexorcistTheme(darkTheme = darkTheme) {
                // The navController declared here was not used in the original MainScreen call from the "top" block.
                // If MainScreen does not actually require a NavController, this line and the rememberNavController import might be removable.
                // For now, it's kept as it was in the "top" block's setContent.
                val navController = rememberNavController()
                MainScreen(
                    viewModel = viewModel,
                    onSignIn = { signIn() },
                    onSelectImage = { selectImage() },
                    onTakePicture = { takePicture() },
                    onAddDocument = { /*TODO*/ },
                    onAddSpreadsheet = { /*TODO*/ }
                )
            }
        }
    }

    // Kept from "top" block
    private fun takePicture() {
        val file = java.io.File(filesDir, "new_image.jpg")
        imageUri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        imageUri?.let { takePictureLauncher.launch(it) }
    }

    // Kept from "top" block
    override fun onStart() {
        super.onStart()
        // The old GoogleSignIn.getLastSignedInAccount(this) is deprecated.
        // Silent sign-in or checking a stored token would be the new approach.
        // For this migration, we'll rely on the user clicking the sign-in button.
        // You can implement silent sign-in by calling oneTapClient.beginSignIn with signInRequest
        // and handling the result, potentially without launching the IntentSender if already signed in.
    }

    // This is the "top" block signIn method
    private fun signIn() { 
        oneTapClient.beginSignIn(signInRequest) 
            .addOnSuccessListener { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    signInLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    Log.e(APP_TAG, "Couldn't start One Tap UI: ${e.localizedMessage}", e)
                    Toast.makeText(this, "Sign in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(APP_TAG, "Google Sign-In 'beginSignIn' (attempting one-tap/existing) failed: ${e.localizedMessage}", e)
                oneTapClient.beginSignIn(signUpRequest) 
                    .addOnSuccessListener { result ->
                         try {
                            val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                            signInLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            Log.e(APP_TAG, "Couldn't start One Tap UI (sign-up flow): ${e.localizedMessage}", e)
                            Toast.makeText(this, "Sign in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e2 ->
                        Log.e(APP_TAG, "Google Sign-In 'beginSignIn' (sign-up/general) failed: ${e2.localizedMessage}", e2)
                        Toast.makeText(this, "Sign in failed completely: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    // Kept from "top" block
    private fun selectImage() {
        selectImageLauncher.launch("image/*")
    }

    // The "bottom" block of code (with NavHost and placeholder screens) has been removed.
}
