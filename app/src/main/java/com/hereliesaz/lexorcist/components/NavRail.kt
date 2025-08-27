package com.hereliesaz.lexorcist.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail
import com.hereliesaz.lexorcist.R

import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavRail() {
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
                        showOnRail = false
                    ),
                    NavItem(
                        text = "Timeline",
                        data = NavItemData.Action(onClick = { onNavigate("timeline") }),
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
