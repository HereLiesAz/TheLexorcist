package com.hereliesaz.lexorcist.components

import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    AzNavRail(
        appName = "The Lexorcist",
        header = NavRailHeader { }, // Or a Spacer if you want some minimal space// header = { Spacer(modifier = Modifier.height(0.dp)) },
              menuSections = listOf(
            NavRailMenuSection(
                title = "Main",
                items = listOf(
                    NavItem(
                        text = "Home",
                        data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Cases",
                        data = NavItemData.Action(onClick = { onNavigate("cases") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Add Evidence",
                        data = NavItemData.Action(onClick = { onNavigate("add_evidence") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Timeline",
                        data = NavItemData.Action(onClick = { onNavigate("timeline") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Settings",
                        data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS),
                        showOnRail = true
                    )
                )
            )
        ),
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.HOME -> onNavigate("home")
                PredefinedAction.SETTINGS -> onNavigate("settings")
                else -> {}
            }
        }
    )
}
