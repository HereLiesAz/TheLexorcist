package com.hereliesaz.lexorcist.ui

import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement // Added import
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.hereliesaz.aznavrail.AzButton
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@VisibleForTesting
internal fun filterTemplates(templates: List<Template>, court: String): List<Template> {
    return if (court.isNotBlank()) {
        templates.filter {
            it.court?.equals(court, ignoreCase = true) == true || it.court?.equals("Generic", ignoreCase = true) == true
        }
    } else {
        templates
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    val templates by caseViewModel.templates.collectAsState()
    val court by caseViewModel.court.collectAsState()
    val jurisdictions by caseViewModel.jurisdictions.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }


    if (selectedTemplate != null) {
        TemplatePreviewDialog(
            template = selectedTemplate!!,
            onDismiss = { selectedTemplate = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.templates).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                },
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), horizontalAlignment = Alignment.End) {
            Text(
                "Here you can manage your document templates. You can create new templates, edit existing ones, and share them with the community.",
                textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                AzButton(onClick = { /* TODO */ }, text = "Create")
                Spacer(modifier = Modifier.width(8.dp))
                AzButton(onClick = { /* TODO */ }, text = "Import")
                Spacer(modifier = Modifier.width(8.dp))
                AzButton(onClick = { /* TODO */ }, text = "Request")
            }
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    readOnly = true,
                    value = court.ifEmpty { "Select a Court" },
                    onValueChange = {},
                    label = { Text("Court") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    jurisdictions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption.courtName) },
                            onClick = {
                                caseViewModel.onCourtSelected(selectionOption.courtName)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                val filteredTemplates = filterTemplates(templates, court)

                items(filteredTemplates) { template ->
                    TemplateItem(
                        template = template,
                        onPreview = { selectedTemplate = it },
                        onShare = { /* TODO */ },
                    )
                }
            }
        }
    }
}

@Composable
fun TemplateItem(
    template: Template,
    onPreview: (Template) -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = { onPreview(template) }),
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.userAgentString += " Lexorcist-Agent"
                        loadDataWithBaseURL(null, template.content, "text/html", "UTF-8", null)
                    }
                },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = template.description,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(8.dp))
                AzButton(onClick = onShare, text = "Share")
            }
        }
    }
}

@Composable
fun TemplatePreviewDialog(template: Template, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Scaffold { padding ->
            Column(modifier = Modifier.padding(padding)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.userAgentString += " Lexorcist-Agent"
                            loadDataWithBaseURL(null, template.content, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
