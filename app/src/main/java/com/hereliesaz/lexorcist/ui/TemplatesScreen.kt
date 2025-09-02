package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel

@Composable
fun TemplatesScreen(
    viewModel: AddonsBrowserViewModel = hiltViewModel()
) {
    var showEditor by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }
    val templates = remember {
        mutableStateOf(
            listOf(
                Template("1", "Template 1", "Description 1", "Content 1", "Author 1"),
                Template("2", "Template 2", "Description 2", "Content 2", "Author 2"),
            )
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Here you can manage your document templates. You can create new templates, edit existing ones, and share them with the community.")
        Button(onClick = {
            selectedTemplate = null
            showEditor = true
        }) {
            Text("Create New Template")
        }
        LazyColumn {
            items(templates.value) { template ->
                Row(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = template.name,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTemplate = template
                                showEditor = true
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        viewModel.shareAddon(
                            name = template.name,
                            description = template.description,
                            content = template.content,
                            type = "Template"
                        )
                    }) {
                        Text("Share")
                    }
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
            onDismiss = { showEditor = false }
        )
    }
}
