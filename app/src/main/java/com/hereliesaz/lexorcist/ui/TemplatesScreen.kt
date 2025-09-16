package com.hereliesaz.lexorcist.ui

import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: AddonsBrowserViewModel = hiltViewModel()) {
    var showEditor by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }
    val context = LocalContext.current
    val templates = remember { mutableStateOf<List<Template>>(emptyList()) }

    LaunchedEffect(Unit) {
        val templateResources =
            listOf(
                R.raw.template_cover_sheet,
                R.raw.template_custody_log,
                R.raw.template_declaration,
                R.raw.template_metadata,
                R.raw.template_table_of_exhibits,
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
                Template(
                    id = resId.toString(),
                    name = name,
                    description = "A standard template for $name.",
                    content = content,
                    author = "Lexorcist",
                )
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
                items(templates.value) { template ->
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
            onSave = {
                // TODO: Implement save logic
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
            Row {
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
