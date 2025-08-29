@file:Suppress("deprecation")
package com.hereliesaz.lexorcist

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.hereliesaz.lexorcist.ui.theme.TheLexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var oneTapClient: SignInClient
    private lateinit var signUpRequest: BeginSignInRequest
    private lateinit var signInRequest: BeginSignInRequest

    private val APP_TAG = "MainActivity"

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

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.addEvidenceToUiList(it, this)
        }
    }

    private var imageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let {
                viewModel.addEvidenceToUiList(it, this)
            }
        }
    }

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
            TheLexorcistTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSignIn = { signIn() },
                    onSelectImage = { selectImage() },
                    onTakePicture = { takePicture() },
                    onAddDocument = { /* TODO */ },
                    onAddSpreadsheet = { /* TODO */ }
                )
            }
        }
    }

    private fun selectImage() {
        selectImageLauncher.launch("image/*")
    }

    private fun takePicture() {
        val file = java.io.File(filesDir, "new_image.jpg")
        imageUri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        imageUri?.let { takePictureLauncher.launch(it) }
    }

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
}
