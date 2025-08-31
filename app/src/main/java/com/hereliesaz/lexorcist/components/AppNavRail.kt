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

    // Resolve strings for lambdas in the Composable scope
    val appNameStr = stringResource(R.string.app_name)
    val navHomeStr = stringResource(R.string.nav_home)
    val navSettingsStr = stringResource(R.string.nav_settings)
    val githubUrlStr = stringResource(R.string.github_url)
    val feedbackEmailStr = stringResource(R.string.feedback_email)
    val feedbackSubjectStr = stringResource(R.string.feedback_subject)

    val navCasesStr = stringResource(R.string.nav_cases)
    val navTimelineStr = stringResource(R.string.nav_timeline)
    val navDataReviewStr = stringResource(R.string.nav_data_review)
    val navEvidenceStr = stringResource(R.string.nav_evidence)
    val navAddEvidenceStr = stringResource(R.string.nav_add_evidence)
    val navAddonsBrowserStr = stringResource(R.string.nav_addons_browser)

    // NavItem texts (these were likely fine as they are resolved in Composable scope before being passed)
    val casesText = stringResource(R.string.cases)
    val timelineText = stringResource(R.string.timeline)
    val dataReviewText = stringResource(R.string.data_review)
    val evidenceText = stringResource(R.string.evidence)
    val addText = stringResource(R.string.add)
    val addonsBrowserText = stringResource(R.string.addons_browser)
    val settingsText = stringResource(R.string.settings)
    val aboutText = stringResource(R.string.about)
    val feedbackText = stringResource(R.string.feedback)

    AzNavRail(
        appName = appNameStr,
        useAppIconAsHeader = true,
        header = NavRailHeader { /* ... */ },
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.HOME -> onNavigate(navHomeStr)
                PredefinedAction.SETTINGS -> onNavigate(navSettingsStr)
                PredefinedAction.ABOUT -> {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(githubUrlStr)
                    )
                    context.startActivity(intent)
                }
                PredefinedAction.FEEDBACK -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse(feedbackEmailStr)
                        putExtra(Intent.EXTRA_SUBJECT, feedbackSubjectStr)
                    }
                    context.startActivity(intent)
                }
                else -> {}
            }
        },
        menuSections = listOf(
            NavRailMenuSection(
                title = "", // Assuming empty title is intentional
                items = listOf(
                    NavItem(
                        text = casesText,
                        data = NavItemData.Action(onClick = { onNavigate(navCasesStr) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = timelineText,
                        data = NavItemData.Action(onClick = { onNavigate(navTimelineStr) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = dataReviewText,
                        data = NavItemData.Action(onClick = { onNavigate(navDataReviewStr) }),
                        showOnRail = false
                    ),
                    NavItem(
                        text = evidenceText,
                        data = NavItemData.Action(onClick = { onNavigate(navEvidenceStr) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = addText,
                        data = NavItemData.Action(onClick = { onNavigate(navAddEvidenceStr) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = addonsBrowserText,
                        data = NavItemData.Action(onClick = { onNavigate(navAddonsBrowserStr) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = settingsText,
                        data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS),
                        showOnRail = false
                    )
                )
            )
        ),
        footerItems = listOf(
            NavItem(
                text = aboutText,
                data = NavItemData.Action(predefinedAction = PredefinedAction.ABOUT)
            ),
            NavItem(
                text = feedbackText,
                data = NavItemData.Action(predefinedAction = PredefinedAction.FEEDBACK)
            )
        )
    )
}