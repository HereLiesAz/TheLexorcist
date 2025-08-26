package com.hereliesaz.lexorcist.components

import androidx.compose.runtime.Composable

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    // TODO: Fix AzNavRail dependency and uncomment this code
    //    AzNavRail(
    //        menuSections = listOf(
    //            NavRailMenuSection(
    //                title = "Main",
    //                items = listOf(
    //                    NavItem(
    //                        text = "Home",
    //                        data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
    //                        showOnRail = true
    //                    ),
    //                    NavItem(
    //                        text = "Cases",
    //                        data = NavItemData.Action(onClick = { onNavigate("cases") }),
    //                        showOnRail = true
    //                    ),
    //                    NavItem(
    //                        text = "Add Evidence",
    //                        data = NavItemData.Action(onClick = { onNavigate("add_evidence") }),
    //                        showOnRail = true
    //                    ),
    //                    NavItem(
    //                        text = "Timeline",
    //                        data = NavItemData.Action(onClick = { onNavigate("timeline") }),
    //                        showOnRail = true
    //                    ),
    //                    NavItem(
    //                        text = "Settings",
    //                        data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS),
    //                        showOnRail = true
    //                    )
    //                )
    //            )
    //        ),
    //        onPredefinedAction = { action ->
    //            when (action) {
    //                PredefinedAction.HOME -> onNavigate("home")
    //                PredefinedAction.SETTINGS -> onNavigate("settings")
    //                else -> {}
    //            }
    //        }
    //    )
}
