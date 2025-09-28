package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.data.Evidence
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExtendedEvent(evidence: Evidence) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with date and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val date = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()).format(Date(evidence.documentDate))
                Text(text = date, style = MaterialTheme.typography.labelSmall)
                Icon(
                    imageVector = getIconForType(evidence.type),
                    contentDescription = evidence.type,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            Text(
                text = evidence.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            evidence.formattedContent?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            if (evidence.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = "Tags",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    evidence.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = { /* No action */ },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getIconForType(type: String): ImageVector {
    return when (type.lowercase()) {
        "text" -> Icons.AutoMirrored.Filled.Message
        "image" -> Icons.Default.Image // Assuming Icons.Filled.Image is not deprecated or has AutoMirrored version if needed
        "video" -> Icons.Default.Videocam // Assuming Icons.Filled.Videocam is not deprecated or has AutoMirrored version if needed
        "audio" -> Icons.Default.Audiotrack // Assuming Icons.Filled.Audiotrack is not deprecated or has AutoMirrored version if needed
        else -> Icons.AutoMirrored.Filled.Article
    }
}