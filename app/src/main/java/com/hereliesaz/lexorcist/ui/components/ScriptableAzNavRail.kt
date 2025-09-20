package com.hereliesaz.lexorcist.ui.components

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.azMenuItem
import com.hereliesaz.aznavrail.azRailItem
import com.hereliesaz.lexorcist.model.ScriptedMenuItem

@Composable
fun ScriptableAzNavRail(
    navController: NavHostController,
    scriptedMenuItems: List<ScriptedMenuItem>,
    onScriptedMenuItemClick: (ScriptedMenuItem) -> Unit,
    onLogout: () -> Unit
) {
    AzNavRail {
        azRailItem(id = "cases", text = "Cases", onClick = { navController.navigate("cases") })
        azRailItem(id = "evidence", text = "Evidence", onClick = { navController.navigate("evidence") })
        azRailItem(
            id = "case_allegations_item",
            text = "Allegations",
            onClick = { navController.navigate("case_allegations_route") },
        )
        azRailItem(id = "templates", text = "Templates", onClick = { navController.navigate("templates") })
        azRailItem(id = "script_builder", text = "Script", onClick = { navController.navigate("script_builder") })
        azRailItem(id = "timeline", text = "Timeline", onClick = { navController.navigate("timeline") })
        azRailItem(id = "data_review", text = "Review", onClick = { navController.navigate("data_review") })

        scriptedMenuItems.forEach { item ->
            if (item.isVisible) {
                // The onClick for the scripted item now calls the lambda passed from MainScreen,
                // which is responsible for triggering the ViewModel's action handler.
                // Note: The original 'item.onClick' was incorrect as it was a String.
                azMenuItem(id = item.id, text = item.text, onClick = { onScriptedMenuItemClick(item) })
            }
        }

        azMenuItem(id = "extras", text = "Extras", onClick = { navController.navigate("extras") })
        azMenuItem(id = "settings", text = "Settings", onClick = { navController.navigate("settings") })
    }
}
