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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.common.api.ApiException
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceDetailsViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptEditorViewModel
import com.hereliesaz.lexorcist.viewmodel.TranscriptionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val caseViewModel: CaseViewModel by viewModels()
    private val evidenceViewModel: EvidenceViewModel by viewModels()
    private val ocrViewModel: OcrViewModel by viewModels()
    private val evidenceDetailsViewModel: EvidenceDetailsViewModel by viewModels()
    private val transcriptionViewModel: TranscriptionViewModel by viewModels()
    private val scriptEditorViewModel: ScriptEditorViewModel by viewModels()

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
                authViewModel.onSignInResult(idToken, email, this, applicationName)
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
            ocrViewModel.startImageReview(it, this)
        }
    }

    private var imageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let {
                ocrViewModel.startImageReview(it, this)
            }
        }
    }

    private val selectDocumentLauncher = registerForActivityResult(
        GetContentWithMultiFilter(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
    ) { uri: Uri? ->
        uri?.let {
            evidenceViewModel.addEvidenceToUiList(it, this)
        }
    }

    private val selectSpreadsheetLauncher = registerForActivityResult(
        GetContentWithMultiFilter(arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    ) { uri: Uri? ->
        uri?.let {
            evidenceViewModel.addEvidenceToUiList(it, this)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
            LexorcistTheme {
                val navController = rememberNavController()
                MainScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    caseViewModel = caseViewModel,
                    evidenceViewModel = evidenceViewModel,
                    evidenceDetailsViewModel = evidenceDetailsViewModel,
                    ocrViewModel = ocrViewModel,
                    scriptEditorViewModel = scriptEditorViewModel,
                    onSignIn = { signIn() },
                    onSelectImage = { selectImage() },
                    onTakePicture = { takePicture() },
                    onAddDocument = { selectDocument() },
                    onAddSpreadsheet = { selectSpreadsheet() },
                    onRecordAudio = { /*TODO*/ },
                    onImportAudio = { /*TODO*/ },
                    startRecording = { /*TODO*/ },
                    stopRecording = { /*TODO*/ },
                    isRecording = false
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
        // Silent sign-in logic can be added here if desired
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
                // Fall back to sign-up flow if sign-in fails (e.g., no authorized accounts)
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

    private fun selectImage() {
        selectImageLauncher.launch("image/*")
    }

    private fun selectDocument() {
        selectDocumentLauncher.launch("*/*")
    }

    private fun selectSpreadsheet() {
        selectSpreadsheetLauncher.launch("*/*")
    }
}
