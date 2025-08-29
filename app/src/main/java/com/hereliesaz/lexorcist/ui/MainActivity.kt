@file:Suppress("DEPRECATION")
package com.hereliesaz.lexorcist.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.common.api.ApiException
import com.hereliesaz.lexorcist.R // For R.string.default_web_client_id
import com.hereliesaz.lexorcist.screens.SettingsScreen // Correct import is already here
import com.hereliesaz.lexorcist.screens.DataReviewScreen
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.DataReviewViewModel

// Placeholder screens
@Composable
fun PlaceholderCasesScreen(navController: NavController) {
    Text("Cases Screen - Placeholder")
}

@Composable
fun PlaceholderTimelineScreen(navController: NavController) {
    Text("Timeline Screen - Placeholder")
}

@Composable
fun PlaceholderEvidenceScreen(navController: NavController) {
    Text("Evidence Screen - Placeholder")
}

@Composable
fun PlaceholderAddEvidenceScreen(navController: NavController) {
    Text("Add Evidence Screen - Placeholder")
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels() // This is com.hereliesaz.lexorcist.viewmodel.MainViewModel
    private val dataReviewViewModel: DataReviewViewModel by viewModels()

    private lateinit var oneTapClient: SignInClient
    private lateinit var signUpRequest: BeginSignInRequest
    private lateinit var signInRequest: BeginSignInRequest

    private val APP_TAG = "AuthUiMainActivity" // Differentiated tag

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                val email = credential.id // Or credential.email if available and preferred
                val displayName = credential.displayName

                Toast.makeText(this, "Signed in as ${email ?: displayName}", Toast.LENGTH_SHORT).show()

                val applicationName = applicationInfo.loadLabel(packageManager).toString()
                mainViewModel.onSignInResult(idToken, email, this, applicationName)

            } catch (e: ApiException) {
                Log.e(APP_TAG, "Sign-in failed after result: ${e.statusCode}", e)
                Toast.makeText(this, "Sign in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(APP_TAG, "Sign-in flow was cancelled or failed. Result code: ${result.resultCode}")
            Toast.makeText(this, "Sign in cancelled or failed", Toast.LENGTH_SHORT).show()
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
            LexorcistTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            navController = navController,
                            mainViewModel = mainViewModel,
                            onSignInClick = { signIn() },
                            onExportClick = { mainViewModel.exportToSheet() }
                        )
                    }
                    composable("data_review") {
                        DataReviewScreen(mainViewModel = mainViewModel, dataReviewViewModel = dataReviewViewModel)
                    }
                    composable("cases") { PlaceholderCasesScreen(navController) }
                    composable("timeline") { PlaceholderTimelineScreen(navController) }
                    composable("evidence") { PlaceholderEvidenceScreen(navController) }
                    composable("add_evidence") { PlaceholderAddEvidenceScreen(navController) }
                    // Corrected: Pass mainViewModel to SettingsScreen
                    composable("settings") { SettingsScreen(viewModel = mainViewModel) } 
                }
            }
        }
    }

    private fun signIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    signInLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    Log.e(APP_TAG, "Couldn't start One Tap UI (sign-in): ${e.localizedMessage}", e)
                    Toast.makeText(this, "Sign in action failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.w(APP_TAG, "beginSignIn (signInRequest) failed: ${e.localizedMessage}. Trying signUpRequest.", e)
                oneTapClient.beginSignIn(signUpRequest)
                    .addOnSuccessListener { result ->
                        try {
                            val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                            signInLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            Log.e(APP_TAG, "Couldn't start One Tap UI (sign-up): ${e.localizedMessage}", e)
                            Toast.makeText(this, "Sign in action failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e2 ->
                        Log.e(APP_TAG, "beginSignIn (signUpRequest) also failed: ${e2.localizedMessage}", e2)
                        Toast.makeText(this, "Sign in failed completely: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}
