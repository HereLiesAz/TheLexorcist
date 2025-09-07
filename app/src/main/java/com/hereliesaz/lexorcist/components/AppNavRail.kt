package com.hereliesaz.lexorcist.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.lexorcist.R

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    val context = LocalContext.current

    val navHomeStr = stringResource(R.string.nav_home)
    val navSettingsStr = stringResource(R.string.nav_settings)
    val githubUrlStr = stringResource(R.string.github_url)
    val feedbackEmailStr = stringResource(R.string.feedback_email)
    val feedbackSubjectStr = stringResource(R.string.feedback_subject)

    val navCasesStr = stringResource(R.string.nav_cases)
    val navTimelineStr = stringResource(R.string.nav_timeline)
    val navReviewStr = "data_review"
    val navEvidenceStr = stringResource(R.string.nav_evidence)
    val navExtrasStr = "extras"
    val navScriptEditorStr = stringResource(R.string.nav_script_editor)

    val homeText = stringResource(id = R.string.nav_home)
    val casesText = stringResource(R.string.cases)
    val timelineText = stringResource(R.string.timeline)
    val dataReviewText = stringResource(R.string.data_review)
    val evidenceText = stringResource(R.string.evidence)
    val extrasText = "Extras"
    val scriptText = "Script"
    val settingsText = stringResource(R.string.settings)
    val aboutText = stringResource(R.string.about)
    val feedbackText = stringResource(R.string.feedback)
    val allegationsText = stringResource(id = R.string.allegations)
    val templatesText = stringResource(id = R.string.templates)

    Scaffold { padding ->
        AzNavRail {
            azSettings(
                displayAppNameInHeader = true,
                packRailButtons = false
            )

            azMenuItem(id = "home", text = homeText.replaceFirstChar { it.uppercase() }, onClick = { onNavigate(navHomeStr) })
            azMenuItem(id = "cases", text = casesText, onClick = { onNavigate(navCasesStr) })
            azMenuItem(id = "evidence", text = evidenceText, onClick = { onNavigate(navEvidenceStr) })
            azMenuItem(id = "allegations", text = allegationsText, onClick = { onNavigate("allegations") })
            azMenuItem(id = "templates", text = templatesText, onClick = { onNavigate("templates") })
            azMenuItem(id = "timeline", text = timelineText, onClick = { onNavigate(navTimelineStr) })
            azMenuItem(id = "review", text = dataReviewText, onClick = { onNavigate(navReviewStr) })
            azMenuItem(id = "extras", text = extrasText, onClick = { onNavigate(navExtrasStr) })
            azMenuItem(id = "script", text = scriptText, onClick = { onNavigate(navScriptEditorStr) })
            azMenuItem(id = "settings", text = settingsText, onClick = { onNavigate(navSettingsStr) })

            azMenuItem(id = "about", text = aboutText, onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrlStr))
                context.startActivity(intent)
            })

            azMenuItem(id = "feedback", text = feedbackText, onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(feedbackEmailStr)
                    putExtra(Intent.EXTRA_SUBJECT, feedbackSubjectStr)
                }
                context.startActivity(intent)
            })
        }
    }
}
