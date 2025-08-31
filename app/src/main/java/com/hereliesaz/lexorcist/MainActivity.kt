package com.hereliesaz.lexorcist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
// import androidx.compose.runtime.collectAsState // Now handled in MainScreen
// import androidx.compose.runtime.getValue // Now handled in MainScreen
// Material components like Text, CircularProgressIndicator, Box, etc. are used in MainScreen or AuthViewModel state handling
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
// import com.hereliesaz.lexorcist.model.SignInState // AuthViewModel exposes this, MainScreen consumes
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oneTapClient = Identity.getSignInClient(this)

        val signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    authViewModel.onSignInResult(credential)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error getting credential from intent", e)
                    authViewModel.onSignInError(e)
                }
            } else {
                // User cancelled the sign-in flow or there was another issue.
                // AuthViewModel's getSignInRequest() already set state to InProgress.
                // If user cancels, we should revert to Idle or show specific cancellation error.
                authViewModel.onSignInError(Exception("Sign-in cancelled or failed by user."))
            }
        }

        setContent {
            LexorcistTheme {
                val navController = rememberNavController()

                MainScreen(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    onSignInClick = {
                        authViewModel.clearSignInError() // Clear previous errors
                        val signInRequest = authViewModel.getSignInRequest() // This sets state to InProgress
                        val pendingIntent = signInRequest.pendingIntent
                        if (pendingIntent != null) {
                            signInLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                        } else {
                            Log.e("MainActivity", "PendingIntent for sign-in was null.")
                            // Notify AuthViewModel about this failure so UI can react
                            authViewModel.onSignInError(Exception("Failed to prepare sign-in request (null PendingIntent)."))
                        }
                    },
                    onSignOutClick = {
                        authViewModel.signOut()
                    }
                )
            }
        }
    }
}
