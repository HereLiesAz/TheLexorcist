package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton

@Composable
fun LocationHistoryInstructionsDialog(
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val googleTakeoutUrl = "https://takeout.google.com/"

    val annotatedString = buildAnnotatedString {
        append("1. Go to ")
        pushStringAnnotation(tag = "URL", annotation = googleTakeoutUrl)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append("Google Takeout")
        }
        pop()
        append(".\n\n")
        append("2. Click 'Deselect all' and then select only 'Location History'. Make sure the format is set to JSON.\n\n")
        append("3. Click 'Next step', then 'Create export'. Wait for the export to be created (you will receive an email).\n\n")
        append("4. Download the .zip file and extract it. Find the 'Semantic Location History' folder, which contains JSON files for each month.\n\n")
        append("5. Click the 'Import' button below and select the JSON file for the month you wish to import.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Location History") },
        text = {
            Column {
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )
            }
        },
        confirmButton = {
            AzButton(
                onClick = {
                    onImport()
                    onDismiss()
                },
                text = "Import"
            )
        },
        dismissButton = {
            AzButton(onClick = onDismiss, text = "Cancel")
        }
    )
}