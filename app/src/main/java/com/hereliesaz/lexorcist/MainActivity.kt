package com.hereliesaz.lexorcist

// import android.app.PendingIntent // No longer directly used here
import android.content.IntentSender
import android.credentials.CredentialManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect // Added import
import androidx.compose.runtime.collectAsState // Added import
import androidx.compose.runtime.getValue // Added import
import androidx.navigation.compose.rememberNavController
// import com.google.android.gms.auth.api.identity.BeginSignInRequest // No longer built here
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential // Added for getFromIntent
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    // private val oneTapClient by lazy { Identity.getSignInClient(this) } // No longer needed for getSignInCredentialFromIntent

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        private val signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credentialManager = CredentialManager.create(context)
                    authViewModel.onSignInResult(credential)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting credential from intent", e)
                    authViewModel.onSignInError(e)
                }
            } else {
                // Handle cancellation or failure from the IntentSender UI
                Log.w(TAG, "Sign-in flow was cancelled or failed. Result code: ${result.resultCode}")
                // authViewModel.onSignInError(Exception("Sign-in cancelled or failed."))
                // Potentially update UI or clear InProgress state if coming from a specific flow
                if (authViewModel.signInState.value is com.hereliesaz.lexorcist.model.SignInState.InProgress) {
                     authViewModel.clearSignInError() // Or set to Idle to allow retry
                }
            }
        }

        // Attempt silent sign-in when the app starts
        authViewModel.attemptSilentSignIn()

        setContent {
            LexorcistTheme {
                val navController = rememberNavController()
                val pendingIntentSender by authViewModel.pendingIntentSenderToLaunch.collectAsState()

                LaunchedEffect(pendingIntentSender) {
                    pendingIntentSender?.let {
                        try {
                            val intentSenderRequest =
                                IntentSenderRequest.Builder(it).build()
                            signInLauncher.launch(intentSenderRequest)
                            authViewModel.consumedPendingIntentSender() // Clear after launching
                        } catch (e: IntentSender.SendIntentException) {
                            Log.e(TAG, "Couldn't start One Tap UI from pending intent: ${e.localizedMessage}")
                            authViewModel.onSignInError(e) // Report error
                            authViewModel.consumedPendingIntentSender() // Clear even on error
                        }
                    }
                }

                MainScreen(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    onSignInClick = {
                        authViewModel.clearSignInError() // Clear previous errors before new attempt
                        authViewModel.requestManualSignIn() // ViewModel now handles request building and result
                    },
                    onSignOutClick = {
                        authViewModel.signOut()
                    }
                )
            }
        }
    }
}
