package com.hereliesaz.lexorcist.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail
import androidx.core.net.toUri

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val appName = context.packageManager.getApplicationLabel(context.applicationInfo).toString()

    val items = listOf("home", "cases", "add_evidence", "", "")
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
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/hereliesaz/$appName".toUri()
                    )
                    context.startActivity(intent)
                }
                PredefinedAction.FEEDBACK -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:hereliesaz@gmail.com".toUri()
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
                        text = "Cases",
                        data = NavItemData.Toggle(
                            data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                            showOnRail = true
                        ),
                        NavItem(
                            text = "Evidence",
                            data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                            showOnRail = true
                        ),
                        NavItem(
                            text = "Timeline",
                            data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                            showOnRail = false
                        ),
                        NavItem(
                            text = "settings",
                            data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                            showOnRail = false
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