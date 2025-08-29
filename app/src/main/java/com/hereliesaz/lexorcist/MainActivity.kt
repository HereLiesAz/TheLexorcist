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
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.data.AppDatabase
import com.hereliesaz.lexorcist.data.CaseRepositoryImpl
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.AuthViewModelFactory
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModelFactory
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModelFactory
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import com.hereliesaz.lexorcist.viewmodel.OcrViewModelFactory

// Removed DataReviewViewModel and placeholder/settings screen imports as they belonged to the NavHost block

class MainActivity : ComponentActivity() {

    private val appDatabase by lazy { AppDatabase.getDatabase(this) }
    private val evidenceRepository by lazy { EvidenceRepositoryImpl(appDatabase.evidenceDao(), null) }
    private val caseRepository by lazy { CaseRepositoryImpl(applicationContext, null) }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application, evidenceRepository, caseRepository)
    }
    private val caseViewModel: CaseViewModel by viewModels {
        CaseViewModelFactory(application, caseRepository)
    }
    private val evidenceViewModel: EvidenceViewModel by viewModels {
        EvidenceViewModelFactory(application, evidenceRepository, caseViewModel.selectedCase.value)
    }
    private val ocrViewModel: OcrViewModel by viewModels {
        OcrViewModelFactory(application)
    }
    
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

    // Kept from "top" block
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            ocrViewModel.startImageReview(it, this)
        }
    }

    // Kept from "top" block
    private var imageUri: Uri? = null

    // Kept from "top" block
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let {
                ocrViewModel.startImageReview(it, this)
            }
        }
    }

    private val selectDocumentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            evidenceViewModel.addDocumentEvidence(it, this)
        }
    }

    private val selectSpreadsheetLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            evidenceViewModel.addSpreadsheetEvidence(it, this)
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
            LexorcistTheme {
                val navController = rememberNavController()
                MainScreen(
                    authViewModel = authViewModel,
                    caseViewModel = caseViewModel,
                    evidenceViewModel = evidenceViewModel,
                    ocrViewModel = ocrViewModel,
                    onSignIn = { signIn() },
                    onSelectImage = { selectImage() },
                    onTakePicture = { takePicture() },
                    onAddDocument = { selectDocument() },
                    onAddSpreadsheet = { selectSpreadsheet() }
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

    private fun selectDocument() {
        selectDocumentLauncher.launch("application/pdf")
    }

    private fun selectSpreadsheet() {
        selectSpreadsheetLauncher.launch("application/vnd.ms-excel")
    }

    // The "bottom" block of code (with NavHost and placeholder screens) has been removed.
}
