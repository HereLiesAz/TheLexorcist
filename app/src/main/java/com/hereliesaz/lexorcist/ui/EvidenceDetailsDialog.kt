package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.ui.components.AzAlertDialog
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@Composable
fun EvidenceDetailsDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onNavigateToEvidenceDetails: () -> Unit,
) {
    AzAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.evidence_details),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                if (evidence.type == "image" || evidence.type == "ocr_image_from_video") {
                    AsyncImage(
                        model = evidence.sourceDocument,
                        contentDescription = stringResource(R.string.evidence_preview),
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = evidence.content,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                AzButton(onClick = onRemove, text = stringResource(R.string.remove))
            }
        },
        confirmButton = {
            AzButton(onClick = onNavigateToEvidenceDetails, text = stringResource(R.string.view_full_details))
        },
        dismissButton = {
            AzButton(onClick = onDismiss, text = stringResource(R.string.close))
        }
    )
}
