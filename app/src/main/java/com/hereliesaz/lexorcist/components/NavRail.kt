package com.hereliesaz.lexorcist.components

import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.NavItem
import com.hereliesaz.aznavrail.NavItemData
import com.hereliesaz.aznavrail.NavRailMenuSection
import com.hereliesaz.aznavrail.PredefinedAction

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    AzNavRail(
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
