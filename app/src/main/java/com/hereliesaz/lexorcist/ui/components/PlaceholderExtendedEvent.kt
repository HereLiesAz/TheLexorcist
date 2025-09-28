package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R

@Composable
fun PlaceholderExtendedEvent() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = stringResource(id = R.string.timeline_placeholder_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Body
            Text(
                text = stringResource(id = R.string.timeline_placeholder_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Example Icon
            Icon(
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = stringResource(id = R.string.timeline_placeholder_icon_desc),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Example Tags
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = "Tags",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                SuggestionChip(
                    onClick = { /* No action */ },
                    label = { Text(stringResource(id = R.string.timeline_placeholder_tag1), style = MaterialTheme.typography.labelSmall) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                SuggestionChip(
                    onClick = { /* No action */ },
                    label = { Text(stringResource(id = R.string.timeline_placeholder_tag2), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}