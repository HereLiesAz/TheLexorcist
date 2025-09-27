package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.hereliesaz.aznavrail.AzButton
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Updated import
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Script
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsScreen(caseViewModel: CaseViewModel = hiltViewModel()) {
    val scripts by caseViewModel.scripts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.scripts).uppercase(Locale.getDefault()),
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
                "Here you can manage your scripts. You can create new scripts, edit existing ones, and share them with the community.",
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
            LazyColumn {
                items(scripts) { script ->
                    ScriptItem(
                        script = script,
                        onEdit = { /* TODO */ },
                        onShare = { /* TODO */ },
                    )
                }
            }
        }
    }
}

@Composable
fun ScriptItem(
    script: Script,
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
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
            Text(
                text = script.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = script.description,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                AzButton(onClick = onShare, text = "Share")
            }
        }
    }
}
