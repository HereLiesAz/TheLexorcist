package com.hereliesaz.lexorcist.ui

import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.gson.Gson
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel
import java.io.File
import java.util.Locale
import java.util.UUID

@VisibleForTesting
internal fun filterTemplates(templates: List<Template>, court: String): List<Template> {
    return if (court.isNotBlank()) {
        templates.filter {
            it.court.equals(court, ignoreCase = true) || it.court == "Generic"
        }
    } else {
        templates
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: AddonsBrowserViewModel = hiltViewModel(),
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    var showEditor by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }
    val context = LocalContext.current
    val templates = remember { mutableStateOf<List<Template>>(emptyList()) }
    val templatesFile = File(context.filesDir, "templates.json")
    val court by caseViewModel.court.collectAsState()

    fun saveTemplates(updatedTemplates: List<Template>) {
        val json = Gson().toJson(updatedTemplates)
        templatesFile.writeText(json)
        templates.value = updatedTemplates
    }

    LaunchedEffect(Unit) {
        if (templatesFile.exists()) {
            val json = templatesFile.readText()
            templates.value = Gson().fromJson(json, Array<Template>::class.java).toList()
        } else {
            val templateResources =
                listOf(
                    R.raw.template_cover_sheet,
                    R.raw.template_custody_log,
                    R.raw.template_declaration,
                    R.raw.template_metadata,
                    R.raw.template_table_of_exhibits,
                    R.raw.template_california_complaint,
                    R.raw.template_federal_complaint,
                    R.raw.template_louisiana_complaint,
                    R.raw.template_new_york_complaint,
                    R.raw.template_texas_answer,
                    R.raw.template_california_answer,
                    R.raw.template_federal_answer,
                    R.raw.template_federal_motion_to_dismiss,
                    R.raw.template_california_motion_to_dismiss,
                    R.raw.template_texas_motion_to_dismiss,
                    R.raw.template_florida_complaint,
                    R.raw.template_illinois_complaint,
                )

            templates.value =
                templateResources.map { resId ->
                    val content =
                        context.resources
                            .openRawResource(resId)
                            .bufferedReader()
                            .use { it.readText() }
                    val name =
                        context.resources
                            .getResourceEntryName(
                                resId,
                            ).replace("template_", "")
                            .replace("_", " ")
                            .replaceFirstChar { it.titlecase() }
                    val description = when (resId) {
                        R.raw.template_california_complaint -> "A template that adheres to the California Rules of Court for pleading papers."
                        R.raw.template_federal_complaint -> "A template for U.S. Federal Courts, based on the FRCP and N.D. Cal. local rules."
                        R.raw.template_louisiana_complaint -> "A template for Louisiana State Courts, focusing on content structure."
                        R.raw.template_new_york_complaint -> "A template for New York Supreme Court complaints."
                        R.raw.template_texas_answer -> "A template for an Original Answer in Texas state court."
                        R.raw.template_california_answer -> "A template for an Answer in California state court."
                        R.raw.template_federal_answer -> "A template for an Answer in U.S. Federal Court."
                        R.raw.template_federal_motion_to_dismiss -> "A template for a Motion to Dismiss in U.S. Federal Court."
                        R.raw.template_california_motion_to_dismiss -> "A template for a Demurrer (Motion to Dismiss) in California state court."
                        R.raw.template_texas_motion_to_dismiss -> "A template for a Special Exception and Motion to Dismiss in Texas state court."
                        R.raw.template_florida_complaint -> "A template for a Complaint in Florida state court."
                        R.raw.template_illinois_complaint -> "A template for a Complaint in Illinois state court."
                        else -> "A standard template for $name."
                    }
                    val court = when (resId) {
                        R.raw.template_california_complaint -> "California"
                        R.raw.template_federal_complaint -> "Federal"
                        R.raw.template_louisiana_complaint -> "Louisiana"
                        R.raw.template_new_york_complaint -> "New York"
                        R.raw.template_texas_answer -> "Texas"
                        R.raw.template_california_answer -> "California"
                        R.raw.template_federal_answer -> "Federal"
                        R.raw.template_federal_motion_to_dismiss -> "Federal"
                        R.raw.template_california_motion_to_dismiss -> "California"
                        R.raw.template_texas_motion_to_dismiss -> "Texas"
                        R.raw.template_florida_complaint -> "Florida"
                        R.raw.template_illinois_complaint -> "Illinois"
                        else -> "Generic"
                    }
                    Template(
                        id = resId.toString(),
                        name = name,
                        description = description,
                        content = content,
                        author = "Lexorcist",
                        court = court,
                    )
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.templates).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
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
            )
            val context = LocalContext.current
            val launcher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent(),
                ) { uri ->
                    uri?.let {
                        try {
                            context.contentResolver.openInputStream(it)?.use { inputStream ->
                                val text =
                                    inputStream.bufferedReader().use { reader ->
                                        reader.readText()
                                    }
                                val template = Gson().fromJson(text, Template::class.java)
                                viewModel.shareAddon(
                                    name = template.name,
                                    description = template.description,
                                    content = template.content,
                                    type = "Template",
                                )
                            }
                        } catch (e: Exception) {
                            // Handle exception
                        }
                    }
                }

            Row {
                LexorcistOutlinedButton(onClick = {
                    selectedTemplate = null
                    showEditor = true
                }, text = "Create New Template")
                Spacer(modifier = Modifier.width(8.dp))
                LexorcistOutlinedButton(onClick = { launcher.launch("application/json") }, text = "Import Template")
            }
            LazyColumn {
                val filteredTemplates = filterTemplates(templates.value, court)

                items(filteredTemplates) { template ->
                    TemplateItem(
                        template = template,
                        onEdit = {
                            selectedTemplate = template
                            showEditor = true
                        },
                        onShare = {
                            viewModel.shareAddon(
                                name = template.name,
                                description = template.description,
                                content = template.content,
                                type = "Template",
                            )
                        },
                    )
                }
            }
        }
    }

    if (showEditor) {
        TemplateEditor(
            template = selectedTemplate,
            onSave = { updatedTemplate ->
                val currentTemplates = templates.value.toMutableList()
                if (selectedTemplate == null) {
                    currentTemplates.add(updatedTemplate.copy(id = UUID.randomUUID().toString()))
                } else {
                    val index = currentTemplates.indexOfFirst { it.id == updatedTemplate.id }
                    if (index != -1) {
                        currentTemplates[index] = updatedTemplate
                    }
                }
                saveTemplates(currentTemplates)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
fun TemplateItem(
    template: Template,
    onEdit: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onEdit),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.userAgentString = settings.userAgentString + " Lexorcist-Agent"
                        loadDataWithBaseURL(null, template.content, "text/html", "UTF-8", null)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.description,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                LexorcistOutlinedButton(onClick = onShare, text = "Share")
            }
        }
    }
}
