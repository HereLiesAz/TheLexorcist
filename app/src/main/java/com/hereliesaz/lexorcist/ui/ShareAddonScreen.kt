package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // Changed from BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Corrected import
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.ExtrasViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAddonScreen(
    navController: NavController,
    extrasViewModel: ExtrasViewModel = hiltViewModel()
) {
    val pendingName by extrasViewModel.pendingSharedItemName.collectAsState()
    val pendingContent by extrasViewModel.pendingSharedItemContent.collectAsState()
    val pendingType by extrasViewModel.pendingSharedItemType.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Script") } // Default to "Script"

    LaunchedEffect(pendingName, pendingContent, pendingType) {
        pendingName?.let { name = it }
        pendingContent?.let { content = it }
        pendingType?.let { type = it }
    }

    DisposableEffect(Unit) {
        onDispose {
            extrasViewModel.clearPendingSharedItem()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.share_addon_title).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            )
        }
    ) { paddingValues ->
        Box( // Changed from BoxWithConstraints
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center // Center content vertically for a better look
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.share_addon_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.share_addon_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.share_addon_content_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false), // Allow content to take space but not push buttons off
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                LexorcistOutlinedButton(
                    onClick = { type = if (type == "Script") "Template" else "Script" },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.share_addon_type_label, type)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LexorcistOutlinedButton(
                    onClick = {
                        if (name.isNotBlank() && content.isNotBlank()) {
                            extrasViewModel.shareItem(name, description, content, type, court = null)
                            navController.popBackStack()
                            // No need to clear pending item here, DisposableEffect handles it
                        } else {
                            // Optionally, show a toast or error that name and content are required
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.share_addon_share_button)
                )
            }
        }
    }
}
