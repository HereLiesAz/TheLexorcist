package com.hereliesaz.lexorcist

import android.app.PendingIntent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.BeginSignInRequest // Added import
import com.google.android.gms.auth.api.identity.Identity
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
                        authViewModel.clearSignInError()
                        val signInRequest: BeginSignInRequest = authViewModel.getSignInRequest()
                        oneTapClient.beginSignIn(signInRequest)
                            .addOnSuccessListener { result ->
                                try {
                                    val intentSenderRequest =
                                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                    signInLauncher.launch(intentSenderRequest)
                                } catch (e: IntentSender.SendIntentException) {
                                    Log.e("MainActivity", "Couldn't start One Tap UI: ${e.localizedMessage}")
                                    // Show user-facing feedback for sign-in error
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Sign-in failed: Couldn't start One Tap UI.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("MainActivity", "Sign-in failed: ${e.localizedMessage}")
                                // Show user-facing feedback for sign-in error
                                Toast.makeText(
                                    this@MainActivity,
                                    "Sign-in failed: ${e.localizedMessage ?: "Unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()
                                authViewModel.onSignInError(e)
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
