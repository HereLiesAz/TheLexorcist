package com.hereliesaz.lexorcist.ui.components

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.hereliesaz.aznavrail.AzNavRail
// import com.hereliesaz.aznavrail.azMenuItem // Removed import
// import com.hereliesaz.aznavrail.azRailItem // Removed import
import com.hereliesaz.lexorcist.model.ScriptedMenuItem
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@Composable
fun ScriptableAzNavRail(
    navController: NavHostController,
    scriptedMenuItems: List<ScriptedMenuItem>,
    onLogout: () -> Unit
) {
    // Resolved here, in composable context, and passed into the AzNavRail
    // builder below: its lambda is a plain DSL builder, not a @Composable one,
    // so stringResource() cannot be called from inside it directly.
    val casesLabel = stringResource(R.string.cases)
    val evidenceLabel = stringResource(R.string.evidence_sheet_name)
    val allegationsLabel = stringResource(R.string.allegations)
    val exhibitsLabel = stringResource(R.string.default_exhibit_sheet_name)
    val scriptLabel = stringResource(R.string.script)
    val templatesLabel = stringResource(R.string.templates)
    val timelineLabel = stringResource(R.string.timeline)
    val dataReviewLabel = stringResource(R.string.data_review)
    val extrasLabel = stringResource(R.string.extras)
    val settingsLabel = stringResource(R.string.settings)

    AzNavRail {
        azSettings(
            displayAppNameInHeader = false,
            packRailButtons = true,
        )
        azRailItem(id = "cases", text = casesLabel, onClick = { navController.navigate("cases") })
        azRailItem(id = "evidence", text = evidenceLabel, onClick = { navController.navigate("evidence") })
        azRailItem(
            id = "case_allegations_item",
            text = allegationsLabel,
            onClick = { navController.navigate("case_allegations_route") },
        )
        azRailItem(id = "exhibits", text = exhibitsLabel, onClick = { navController.navigate("exhibits") })
        azRailItem(id = "script_builder", text = scriptLabel, onClick = { navController.navigate("script_builder") })
        azRailItem(id = "templates", text = templatesLabel, onClick = { navController.navigate("templates") })
        azRailItem(id = "timeline", text = timelineLabel, onClick = { navController.navigate("timeline") })
        azRailItem(id = "data_review", text = dataReviewLabel, onClick = { navController.navigate("data_review") })

        scriptedMenuItems.forEach { item ->
            if (item.isVisible) {
                azMenuItem(id = item.id, text = item.text, onClick = item.onClick)
            }
        }

        azRailItem(id = "extras", text = extrasLabel, onClick = { navController.navigate("extras") })
        azRailItem(id = "settings", text = settingsLabel, onClick = { navController.navigate("settings") })
    }
}
