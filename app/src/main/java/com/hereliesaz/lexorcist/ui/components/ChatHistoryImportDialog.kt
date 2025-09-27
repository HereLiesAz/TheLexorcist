package com.hereliesaz.lexorcist.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
// Removed LocalUriHandler as Text with LinkAnnotation.Url handles it implicitly
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration // Added for underlining links
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.util.*
import androidx.compose.ui.text.LinkAnnotation // Added for LinkAnnotation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryImportDialog(
    onDismiss: () -> Unit,
    onImport: (Uri) -> Unit
) {
    // val uriHandler = LocalUriHandler.current // No longer explicitly needed here
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

                    val whatsappUrl = "https://faq.whatsapp.com/1144861179456352"
                    pushLink(LinkAnnotation.Url(whatsappUrl))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append("WhatsApp")
                    }
                    pop() // pop for whatsapp link
                    append("\n\n")

                    val messengerUrl = "https://www.facebook.com/help/messenger-app/111849642541092"
                    pushLink(LinkAnnotation.Url(messengerUrl))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append("Facebook Messenger")
                    }
                    pop() // pop for messenger link
                    append("\n\n")

                    val instagramUrl = "https://help.instagram.com/181231772500920"
                    pushLink(LinkAnnotation.Url(instagramUrl))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append("Instagram")
                    }
                    pop() // pop for instagram link
                }

                Text(text = annotatedString) // Replaced ClickableText with Text
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