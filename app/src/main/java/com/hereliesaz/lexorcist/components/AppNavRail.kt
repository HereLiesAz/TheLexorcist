package com.hereliesaz.lexorcist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail
import android.content.Intent
import android.net.Uri

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val appName = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
    AzNavRail(
        appName = "The Lexorcist",
        useAppIconAsHeader = true,
        header = NavRailHeader {        },
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.HOME -> onNavigate("home")
                PredefinedAction.SETTINGS -> onNavigate("settings")
                PredefinedAction.ABOUT -> {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/hereliesaz/$appName")
                    )
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
                        text = "Cases",
                        data = NavItemData.Action(onClick = { onNavigate("cases") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Timeline",
                        data = NavItemData.Action(onClick = { onNavigate("timeline") }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = "Data Review",
                        data = NavItemData.Action(onClick = { onNavigate("data_review") }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = "Visualization",
                        data = NavItemData.Action(onClick = { onNavigate("visualization") }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = "Evidence",
                        data = NavItemData.Action(onClick = { onNavigate("evidence") }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = "Add",
                        data = NavItemData.Action(onClick = { onNavigate("add_evidence") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Settings",
                        data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS),
                        showOnRail = false
                    )
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