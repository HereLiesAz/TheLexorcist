package com.hereliesaz.lexorcist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Context
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.dropbox.core.android.Auth
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.MainScreen
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * The single Activity for the application, hosting the Jetpack Compose UI.
 *
 * This Activity is responsible for:
 * 1. Setting up the base context for localization (language selection).
 * 2. initializing the Compose content hierarchy via [setContent].
 * 3. Managing high-level authentication flows (silent sign-in, Dropbox token retrieval).
 * 4. Handling system UI configuration (Edge-to-Edge).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Manager for user preferences, used here specifically for language settings.
     */
    @Inject
    lateinit var settingsManager: SettingsManager

    // Tracks the current language to detect changes in onResume and recreate the activity if needed.
    private var currentLanguage: String? = null

    // ViewModels scoped to the Activity.
    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val caseViewModel: CaseViewModel by viewModels()
    private val scriptedMenuViewModel: ScriptedMenuViewModel by viewModels()

    /**
     * Intercepts context attachment to apply the user's selected locale.
     * This ensures that the Activity starts with the correct language configuration before `onCreate`.
     *
     * @param newBase The base context to attach.
     */
    override fun attachBaseContext(newBase: Context) {
        // Manually create an instance of SettingsManager because injection hasn't happened yet at this lifecycle stage.
        val settingsManager = SettingsManager(newBase)
        val lang = settingsManager.getLanguage()
        val locale = Locale.forLanguageTag(lang)
        // Create a new configuration with the selected locale.
        val context = newBase.createConfigurationContext(newBase.resources.configuration.apply { setLocale(locale) })
        super.attachBaseContext(context)
    }

    /**
     * Initializes the Activity.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Configure the window to fit system windows (Edge-to-Edge display).
        // This allows drawing behind status and navigation bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // Cache the current language to check for changes later.
        currentLanguage = settingsManager.getLanguage()

        // Attempt to silently sign in the user (e.g., refreshing existing tokens).
        authViewModel.signIn(this, mainViewModel, silent = true)

        setContent {
            // Collect theme mode from CaseViewModel (which might load from settings).
            val themeMode by caseViewModel.themeMode.collectAsState()

            // Collect sign-in state, though logic for redirection is largely handled within lower-level screens.
            val signInState by authViewModel.signInState.collectAsState()

            // The case loading logic has been moved to CasesScreen.kt to ensure it's
            // loaded every time the screen is displayed and is not dependent on sign-in state.

            // Collect the user recoverable auth intent from CaseViewModel.
            // This is triggered when a Google API call fails with a UserRecoverableAuthIOException (e.g., consent needed).
            val userRecoverableIntent by caseViewModel.userRecoverableAuthIntent.collectAsState()

            LaunchedEffect(userRecoverableIntent) {
                userRecoverableIntent?.let {
                    // Launch the intent provided by Google Play Services to resolve the auth issue.
                    startActivity(it)
                    // Clear the intent from the ViewModel to prevent re-launching.
                    caseViewModel.clearUserRecoverableAuthIntent()
                }
            }

            // Apply the app theme.
            LexorcistTheme(themeMode = themeMode) {
                // Create the NavController for Compose Navigation.
                val navController = rememberNavController()

                // Render the main screen, passing down necessary ViewModels and callbacks.
                MainScreen(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    scriptedMenuViewModel = scriptedMenuViewModel,
                    onSignInClick = {
                        // Trigger explicit sign-in flow.
                        authViewModel.signIn(this, mainViewModel)
                    },
                    onSignOutClick = {
                        authViewModel.signOut(mainViewModel)
                    },
                )
            }
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    override fun onResume() {
        super.onResume()

        // Check if the language setting has changed while the app was in the background (or from a settings change).
        if (currentLanguage != settingsManager.getLanguage()) {
            // Recreate the activity to apply the new locale via attachBaseContext.
            recreate()
        }

        // Handle Dropbox OAuth callback.
        // If the user just returned from the Dropbox auth flow, the token should be available.
        val accessToken = Auth.getOAuth2Token()
        if (accessToken != null) {
            authViewModel.storeDropboxAccessToken(accessToken)
        }
    }
}
