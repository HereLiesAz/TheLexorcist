package com.hereliesaz.lexorcist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Context
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat // Added import
import androidx.navigation.compose.rememberNavController
import com.dropbox.core.android.Auth
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.MainScreen // Added import
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsManager: SettingsManager

    private var currentLanguage: String? = null
    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val caseViewModel: CaseViewModel by viewModels()
    private val scriptedMenuViewModel: ScriptedMenuViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val settingsManager = SettingsManager(newBase)
        val lang = settingsManager.getLanguage()
        val locale = Locale.forLanguageTag(lang) // CORRECTED
        val context = newBase.createConfigurationContext(newBase.resources.configuration.apply { setLocale(locale) })
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false) // Added for edge-to-edge
        super.onCreate(savedInstanceState)
        currentLanguage = settingsManager.getLanguage()

        authViewModel.signIn(this, mainViewModel, silent = true)

        setContent {
            val themeMode by caseViewModel.themeMode.collectAsState()
            val signInState by authViewModel.signInState.collectAsState()

            LaunchedEffect(signInState) {
                if (signInState is com.hereliesaz.lexorcist.model.SignInState.Success) {
                    caseViewModel.loadCasesFromRepository() // Removed mainViewModel
                }
            }

            // Collect the user recoverable auth intent from CaseViewModel
            val userRecoverableIntent by caseViewModel.userRecoverableAuthIntent.collectAsState()
            LaunchedEffect(userRecoverableIntent) {
                userRecoverableIntent?.let {
                    startActivity(it) // Launch the intent directly
                    caseViewModel.clearUserRecoverableAuthIntent() // Clear the intent after launching
                }
            }

            LexorcistTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                MainScreen(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    scriptedMenuViewModel = scriptedMenuViewModel,
                    onSignInClick = {
                        authViewModel.signIn(this, mainViewModel)
                    },
                    onSignOutClick = {
                        authViewModel.signOut(mainViewModel)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentLanguage != settingsManager.getLanguage()) {
            recreate()
        }
        val accessToken = Auth.getOAuth2Token()
        if (accessToken != null) {
            authViewModel.storeDropboxAccessToken(accessToken)
        }
    }
}
