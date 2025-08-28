package com.hereliesaz.lexorcist.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.DataReviewViewModel // Added import

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val dataReviewViewModel: DataReviewViewModel by viewModels() // Added DataReviewViewModel instance

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            mainViewModel.onSignInResult(account, this) // Assuming mainViewModel handles this
        } catch (e: ApiException) {
            // Handle sign in error
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        // Passed the dataReviewViewModel instance
                        DataReviewScreen(viewModel = dataReviewViewModel) 
                    }
                }
            }
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/spreadsheets"))
            .build()
        val signInClient = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(signInClient.signInIntent)
    }
}