

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppScreen()
        }
    }
}


@Composable
fun TestAppScreen() {
    val context = LocalContext.current
    val appName = context.packageManager.getApplicationLabel(context.applicationInfo).toString()

    // In your screen's Composable, e.g., inside a Row
    AzNavRail(
        appName = appName,
        useAppIconAsHeader = true,
        header = NavRailHeader { /* ... */ },
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.HOME -> { /* Navigate to Home */
                }
                PredefinedAction.ABOUT -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hereliesaz/$appName"))
                    context.startActivity(intent)
                }
                PredefinedAction.FEEDBACK -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:hereliesaz@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "$appName - Feedback")
                    }
                    context.startActivity(intent)
                }
                else -> {}
            }
        },
        menuSections = listOf(
            NavRailMenuSection(
                title = "",
                items = listOf(
                    NavItem(
                        text = "Home",
                        data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Online",
                        data = NavItemData.Toggle(
                            initialIsChecked = true,
                            onStateChange = { isOnline -> /* ... */ }
                        ),
                        showOnRail = true,
                        railButtonText = "On"
                    ),
                )
            )
        ),
        footerItems = listOf(
            NavItem(
                text = "About",
                data = NavItemData.Action(predefinedAction = PredefinedAction.ABOUT)
            ),
            NavItem(
                text = "Feedback",
                data = NavItemData.Action(predefinedAction = PredefinedAction.FEEDBACK)
            )
        )
    )
}