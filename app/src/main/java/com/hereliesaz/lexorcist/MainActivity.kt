package com.hereliesaz.lexorcist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
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
                    authViewModel.onSignInError(e)
                }
            }
        }

        setContent {
            LexorcistTheme {
                val isSignedIn by authViewModel.isSignedIn.collectAsState()
                val navController = rememberNavController()
                MainScreen(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    onSignInClick = {
                        authViewModel.getSignInRequest()?.let { signInRequest ->
                            signInLauncher.launch(IntentSenderRequest.Builder(signInRequest.pendingIntent).build())
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
