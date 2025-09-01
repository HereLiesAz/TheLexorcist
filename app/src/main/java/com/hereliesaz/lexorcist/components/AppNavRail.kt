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
import java.util.Locale

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
    val navDataReviewStr = stringResource(R.string.nav_data_review)
    val navEvidenceStr = stringResource(R.string.nav_evidence) 
    val navAddEvidenceStr = stringResource(R.string.nav_add_evidence)
    val navAddonsBrowserStr = stringResource(R.string.nav_addons_browser)
    val navScriptEditorStr = stringResource(R.string.nav_script_editor) // Added for route

    val casesText = stringResource(R.string.cases).uppercase(Locale.getDefault())
    val timelineText = stringResource(R.string.timeline).uppercase(Locale.getDefault())
    val dataReviewText = stringResource(R.string.data_review).uppercase(Locale.getDefault())
    val evidenceText = stringResource(R.string.evidence).uppercase(Locale.getDefault())
    val addText = stringResource(R.string.add).uppercase(Locale.getDefault())
    val extrasText = "EXTRAS" // Already uppercase, kept for clarity
    val scriptText = "SCRIPT".uppercase(Locale.getDefault())
    val settingsText = stringResource(R.string.settings).uppercase(Locale.getDefault())
    val aboutText = stringResource(R.string.about).uppercase(Locale.getDefault())
    val feedbackText = stringResource(R.string.feedback).uppercase(Locale.getDefault())

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
                title = "", 
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
                        text = evidenceText,
                        data = NavItemData.Action(onClick = { onNavigate(navDataReviewStr) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = addText,
                        data = NavItemData.Action(onClick = { onNavigate(navAddEvidenceStr) }),
                        showOnRail = true
                    ),
                    NavItem(
                        text = extrasText,
                        data = NavItemData.Action(onClick = { onNavigate(navAddonsBrowserStr) }), 
                        showOnRail = true
                    ),
                    NavItem(
                        text = scriptText,
                        data = NavItemData.Action(onClick = { onNavigate(navScriptEditorStr) }),
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
