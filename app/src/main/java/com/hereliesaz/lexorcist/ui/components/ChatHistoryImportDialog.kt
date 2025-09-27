package com.hereliesaz.lexorcist.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryImportDialog(
    onDismiss: () -> Unit,
    onImport: (Uri) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { onImport(it) }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Chat History") },
        text = {
            Column {
                Text("To import your chat history, you first need to export it from the messaging app.")
                Spacer(modifier = Modifier.height(16.dp))

                val annotatedString = buildAnnotatedString {
                    append("Follow the instructions for your platform:\n\n")

                    pushStringAnnotation(tag = "whatsapp", annotation = "https://faq.whatsapp.com/1144861179456352")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("WhatsApp")
                    }
                    pop()
                    append("\n\n")

                    pushStringAnnotation(tag = "messenger", annotation = "https://www.facebook.com/help/messenger-app/111849642541092")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Facebook Messenger")
                    }
                    pop()
                    append("\n\n")

                    pushStringAnnotation(tag = "instagram", annotation = "https://help.instagram.com/181231772500920")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Instagram")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "whatsapp", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                        annotatedString.getStringAnnotations(tag = "messenger", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                        annotatedString.getStringAnnotations(tag = "instagram", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = { filePickerLauncher.launch("*/*") }) {
                Text("Import File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}