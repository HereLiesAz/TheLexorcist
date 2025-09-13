package com.hereliesaz.lexorcist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val caseViewModel: CaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authViewModel.signIn(this, silent = true)

        setContent {
            val themeMode by caseViewModel.themeMode.collectAsState()
            val signInState by authViewModel.signInState.collectAsState()

            LaunchedEffect(signInState) {
                if (signInState is com.hereliesaz.lexorcist.model.SignInState.Success) {
                    caseViewModel.loadCasesFromRepository()
                }
            }

            LexorcistTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                MainScreen(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    onSignInClick = {
                        authViewModel.signIn(this)
                    },
                    onSignOutClick = {
                        authViewModel.signOut()
                    },
                )
            }
        }
    }
}
