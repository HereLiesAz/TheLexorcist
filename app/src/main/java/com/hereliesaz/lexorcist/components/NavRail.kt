package com.hereliesaz.lexorcist.components

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail
import com.hereliesaz.lexorcist.R

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    AzNavRail(
        appName = "The Lexorcist",
        header = NavRailHeader(
            onClick = { /* TODO: Handle click */ }
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "App Icon"
            )
        },
        menuSections = listOf(
            NavRailMenuSection(
                title = "Main",
                items = listOf(
                    NavItem(
                        text = "Home",
                        icon = Icons.Default.Home,
                        data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Cases",
                        icon = Icons.Default.List,
                        data = NavItemData.Action(onClick = { onNavigate("cases") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Add Evidence",
                        icon = Icons.Default.Add,
                        data = NavItemData.Action(onClick = { onNavigate("add_evidence") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Timeline",
                        icon = Icons.Default.Timeline,
                        data = NavItemData.Action(onClick = { onNavigate("timeline") }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Settings",
                        icon = Icons.Default.Settings,
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
