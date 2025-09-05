package com.hereliesaz.lexorcist.ui

import android.webkit.WebView
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: AddonsBrowserViewModel = hiltViewModel()) {
    var showEditor by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }
    val templates =
        remember {
            mutableStateOf(
                listOf(
                    Template("1", "Template 1", "Description 1", "<h1>Template 1</h1><p>This is a test template.</p>", "Author 1"),
                    Template("2", "Template 2", "Description 2", "<h1>Template 2</h1><p>This is another test template.</p>", "Author 2"),
                ),
            )
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.templates).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "Here you can manage your document templates. You can create new templates, edit existing ones, and share them with the community.",
            )
            Button(onClick = {
                selectedTemplate = null
                showEditor = true
            }) {
                Text("Create New Template")
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
                        }
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
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        loadDataWithBaseURL(null, template.content, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = template.description,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onShare) {
                    Text("Share")
                }
            }
        }
    }
}
