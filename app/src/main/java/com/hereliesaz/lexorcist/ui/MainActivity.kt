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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.common.api.ApiException
// import com.google.android.gms.common.api.Scope // No longer needed for this type of sign-in request building
import com.hereliesaz.lexorcist.R // For R.string.default_web_client_id
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.DataReviewViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
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

        // IMPORTANT: Replace R.string.default_web_client_id with your actual Web Client ID from Google Cloud Console
        val serverClientId = getString(R.string.default_web_client_id)

        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false) // For sign-UP, don't filter by existing accounts
                    .build()
            )
            .build()

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(true) // For sign-IN, filter by existing accounts
                    .build()
            )
            .setAutoSelectEnabled(true) // Enable One Tap
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
                        DataReviewScreen(viewModel = dataReviewViewModel)
                    }
                }
            }
        }
    }

    private fun signIn() {
        oneTapClient.beginSignIn(signInRequest) // Try one-tap sign-in first
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
                // Fallback to a more general sign-in/sign-up flow if one-tap/existing account fails
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
