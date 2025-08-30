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
import com.hereliesaz.lexorcist.viewmodel.TranscriptionViewModel
import com.hereliesaz.lexorcist.viewmodel.TranscriptionViewModelFactory
import com.hereliesaz.lexorcist.viewmodel.TranscriptionState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import android.Manifest
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.collectLatest
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appDatabase by lazy { AppDatabase.getDatabase(this) }
    private val evidenceRepository by lazy { EvidenceRepositoryImpl(appDatabase.evidenceDao()) }
    private val caseRepository by lazy { CaseRepositoryImpl(applicationContext, null) }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application, evidenceRepository, caseRepository)
    }
    private val caseViewModel: CaseViewModel by viewModels {
        CaseViewModelFactory(application, caseRepository)
    }
    private val evidenceViewModel: EvidenceViewModel by viewModels {
        EvidenceViewModelFactory(application, caseRepository, evidenceRepository)
    }
    private val ocrViewModel: OcrViewModel by viewModels {
        OcrViewModelFactory(application)
    }

    private val transcriptionViewModel: TranscriptionViewModel by viewModels {
        TranscriptionViewModelFactory(application)
    }


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
    private var audioUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let {
                ocrViewModel.startImageReview(it, this)
            }
        }
    }

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            audioUri = it
            transcriptionViewModel.transcribeAudio(it)
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: java.io.File? = null
    private var isRecording = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    // Corrected: Provide the mimeTypes array to the constructor
    private val selectDocumentLauncher = registerForActivityResult(
        GetContentWithMultiFilter(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
    ) { uri: Uri? ->
        uri?.let {
            caseViewModel.selectedCase.value?.let { case ->
                evidenceViewModel.addDocumentEvidence(case.id, it, this)
            }
        }
    }

    // Corrected: Provide the mimeTypes array to the constructor
    private val selectSpreadsheetLauncher = registerForActivityResult(
        GetContentWithMultiFilter(arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    ) { uri: Uri? ->
        uri?.let {
            caseViewModel.selectedCase.value?.let { case ->
                evidenceViewModel.addSpreadsheetEvidence(case.id, it, this)
            }
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
            val credential by authViewModel.credential.collectAsState()

            LaunchedEffect(credential) {
                credential?.let {
                    transcriptionViewModel.setCredential(it)
                }
            }

            val transcriptionState by transcriptionViewModel.transcriptionState.collectAsState()

            LaunchedEffect(transcriptionState) {
                when (val state = transcriptionState) {
                    is TranscriptionState.Success -> {
                        val caseId = caseViewModel.selectedCase.value?.id
                        if (caseId != null) {
                            val newEvidence = evidenceViewModel.addAudioEvidence(caseId, state.transcript)
                            mainViewModel.runScriptOnEvidence(newEvidence)
                            audioUri?.let { mainViewModel.uploadAudioFile(it, this@MainActivity) }
                            Toast.makeText(this@MainActivity, "Audio evidence added and processed.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "No case selected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is TranscriptionState.Error -> {
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        // Idle or Loading
                    }
                }
            }

            LexorcistTheme {
                // val navController = rememberNavController() // Commented out as MainScreen doesn't take navController
                MainScreen(
                    authViewModel = authViewModel,
                    caseViewModel = caseViewModel,
                    evidenceViewModel = evidenceViewModel,
                    ocrViewModel = ocrViewModel,
                    onSignIn = { signIn() },
                    onSelectImage = { selectImage() },
                    onTakePicture = { takePicture() },
                    onAddDocument = { selectDocument() },
                    onAddSpreadsheet = { selectSpreadsheet() },
                    onRecordAudio = { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onImportAudio = { importAudio() },
                    startRecording = { startRecording() },
                    stopRecording = { stopRecording() },
                    isRecording = isRecording.value
                )
            }
        }
    }

    private fun takePicture() {
        val file = java.io.File(filesDir, "new_image.jpg") // Consider using getExternalFilesDir for broader access if needed
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
                    Log.e(APP_TAG, "Couldn\'t start One Tap UI: ${e.localizedMessage}", e)
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
                            Log.e(APP_TAG, "Couldn\'t start One Tap UI (sign-up flow): ${e.localizedMessage}", e)
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
        // Corrected: Launch with a single primary MIME type string. 
        // The GetContentWithMultiFilter contract will use this and also apply the mimeTypes from its constructor.
        selectDocumentLauncher.launch("*/*") // Or a more specific primary type like "application/pdf"
    }

    private fun selectSpreadsheet() {
        // Corrected: Launch with a single primary MIME type string.
        selectSpreadsheetLauncher.launch("*/*") // Or a more specific primary type like "application/vnd.ms-excel"
    }

    private fun importAudio() {
        selectAudioLauncher.launch("audio/*")
    }

    private fun startRecording() {
        audioFile = java.io.File(filesDir, "new_recording.3gp")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
            } catch (e: java.io.IOException) {
                Log.e(APP_TAG, "prepare() failed")
            }
            start()
            isRecording.value = true
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording.value = false
        audioFile?.let {
            transcriptionViewModel.transcribeAudio(Uri.fromFile(it))
        }
    }
}
