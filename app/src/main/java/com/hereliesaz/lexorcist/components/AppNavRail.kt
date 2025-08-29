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
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    AzNavRail(
        appName = stringResource(R.string.app_name),
        useAppIconAsHeader = true,
        header = NavRailHeader {        },
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.HOME -> onNavigate(stringResource(R.string.nav_home))
                PredefinedAction.SETTINGS -> onNavigate(stringResource(R.string.nav_settings))
                PredefinedAction.ABOUT -> {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(stringResource(R.string.github_url))
                    )
                    context.startActivity(intent)
                }
                PredefinedAction.FEEDBACK -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse(stringResource(R.string.feedback_email))
                        putExtra(Intent.EXTRA_SUBJECT, stringResource(R.string.feedback_subject))
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
                        text = stringResource(R.string.cases),
                        data = NavItemData.Action(onClick = { onNavigate(stringResource(R.string.nav_cases)) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = stringResource(R.string.timeline),
                        data = NavItemData.Action(onClick = { onNavigate(stringResource(R.string.nav_timeline)) }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = stringResource(R.string.data_review),
                        data = NavItemData.Action(onClick = { onNavigate(stringResource(R.string.nav_data_review)) }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = stringResource(R.string.visualization),
                        data = NavItemData.Action(onClick = { onNavigate(stringResource(R.string.nav_visualization)) }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = stringResource(R.string.evidence),
                        data = NavItemData.Action(onClick = { onNavigate(stringResource(R.string.nav_evidence)) }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = stringResource(R.string.add),
                        data = NavItemData.Action(onClick = { onNavigate(stringResource(R.string.nav_add_evidence)) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = stringResource(R.string.settings),
                        data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS),
                        showOnRail = false
                    )
                )
            )
        ),
        footerItems = listOf(
            NavItem(
                text = stringResource(R.string.about),
                data = NavItemData.Action(predefinedAction = PredefinedAction.ABOUT)
                ),
            NavItem(
                text = stringResource(R.string.feedback),
                data = NavItemData.Action(predefinedAction = PredefinedAction.FEEDBACK)
        )
    )
    )
}