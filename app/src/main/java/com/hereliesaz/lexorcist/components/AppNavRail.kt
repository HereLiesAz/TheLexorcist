package com.hereliesaz.lexorcist.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail
import com.hereliesaz.lexorcist.R

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    val context = LocalContext.current

    val appNameStr = stringResource(R.string.app_name)
    val navHomeStr = stringResource(R.string.nav_home)
    val navSettingsStr = stringResource(R.string.nav_settings)
    val githubUrlStr = stringResource(R.string.github_url)
    val feedbackEmailStr = stringResource(R.string.feedback_email)
    val feedbackSubjectStr = stringResource(R.string.feedback_subject)

    val navCasesStr = stringResource(R.string.nav_cases)
    val navTimelineStr = stringResource(R.string.nav_timeline)
    val navReviewStr = "review"
    val navEvidenceStr = stringResource(R.string.nav_evidence)
    val navExtrasStr = "extras"
    val navScriptEditorStr = stringResource(R.string.nav_script_editor) // Added for route

    val casesText = stringResource(R.string.cases)
    val timelineText = stringResource(R.string.timeline)
    val dataReviewText = stringResource(R.string.data_review)
    val evidenceText = stringResource(R.string.evidence)
    val addText = stringResource(R.string.add)
    val extrasText = "Extras"
    val scriptText = "Script"
    val settingsText = stringResource(R.string.settings)
    val aboutText = stringResource(R.string.about)
    val feedbackText = stringResource(R.string.feedback)

    AzNavRail(
        // App name in header, usually not all caps
        appName = appNameStr,
        useAppIconAsHeader = true,
        header = NavRailHeader { /* ... */ },
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.HOME -> onNavigate(navHomeStr)
                PredefinedAction.SETTINGS -> onNavigate(navSettingsStr)
                PredefinedAction.ABOUT -> {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(githubUrlStr),
                        )
                    context.startActivity(intent)
                }
                PredefinedAction.FEEDBACK -> {
                    val intent =
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(feedbackEmailStr)
                            putExtra(Intent.EXTRA_SUBJECT, feedbackSubjectStr)
                        }
                    context.startActivity(intent)
                }
                else -> {}
            }
        },
        menuSections =
            listOf(
                NavRailMenuSection(
                    title = "",
                    items =
                        listOf(
                            NavItem(
                                text = casesText,
                                data = NavItemData.Action(onClick = { onNavigate(navCasesStr) }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = evidenceText,
                                data = NavItemData.Action(onClick = { onNavigate(navEvidenceStr) }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = "Allegations",
                                data = NavItemData.Action(onClick = { onNavigate("allegations") }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = "Templates",
                                data = NavItemData.Action(onClick = { onNavigate("templates") }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = timelineText,
                                data = NavItemData.Action(onClick = { onNavigate(navTimelineStr) }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = "Review",
                                data = NavItemData.Action(onClick = { onNavigate(navReviewStr) }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = extrasText,
                                data = NavItemData.Action(onClick = { onNavigate(navExtrasStr) }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = scriptText,
                                data = NavItemData.Action(onClick = { onNavigate(navScriptEditorStr) }),
                                showOnRail = true,
                            ),
                            NavItem(
                                text = settingsText,
                                data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS),
                                showOnRail = false,
                            ),
                        ),
                ),
            ),
        footerItems =
            listOf(
                NavItem(
                    text = aboutText,
                    data = NavItemData.Action(predefinedAction = PredefinedAction.ABOUT),
                ),
                NavItem(
                    text = feedbackText,
                    data = NavItemData.Action(predefinedAction = PredefinedAction.FEEDBACK),
                ),
            ),
    )
}
