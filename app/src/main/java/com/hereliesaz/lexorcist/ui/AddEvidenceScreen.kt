package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items // Keep this for LazyColumn
// import androidx.compose.material.icons.automirrored.filled.RotateLeft // Not used in the first composable
// import androidx.compose.material.icons.automirrored.filled.RotateRight // Not used in the first composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// import com.hereliesaz.lexorcist.viewmodel.MainViewModel // Not used in the first composable
import com.hereliesaz.lexorcist.ui.TaggedDataItem // Ensure this is present
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import com.hereliesaz.lexorcist.R // For stringResource
import com.hereliesaz.lexorcist.data.Evidence // Ensure Evidence model is imported
import java.text.SimpleDateFormat // For date formatting
import java.util.Locale // For date formatting

@Composable
fun AddEvidenceScreen(
    evidenceViewModel: EvidenceViewModel,
    ocrViewModel: OcrViewModel,
    mainViewModel: MainViewModel,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit,
    onAddTextEvidence: () -> Unit,
    onAddDocument: () -> Unit,
    onAddSpreadsheet: () -> Unit
) {
    val imageBitmapForReview by ocrViewModel.imageBitmapForReview.collectAsState()
    val isOcrInProgress by ocrViewModel.isOcrInProgress.collectAsState()
    val isUploadingFile by mainViewModel.isUploadingFile.collectAsState()
    val newlyCreatedEvidence by ocrViewModel.newlyCreatedEvidence.collectAsState() // This is StateFlow<Evidence?>
    val evidenceList by evidenceViewModel.evidenceList.collectAsState() // This is StateFlow<List<Evidence>>
    val context = LocalContext.current

    if (isOcrInProgress || isUploadingFile) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(if (isOcrInProgress) "Processing Image..." else "Uploading File...")
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.End, 
            verticalArrangement = Arrangement.Center
        ) {
            if (imageBitmapForReview == null) {
                Column(
                    horizontalAlignment = Alignment.End, 
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onSelectImage) {
                        Text("Select Image")
                    }
                    OutlinedButton(onClick = onTakePicture) {
                        Text("Take Picture")
                    }
                    OutlinedButton(onClick = onAddTextEvidence) {
                        Text("Add Text Evidence")
                    }
                    OutlinedButton(onClick = onAddDocument) {
                        Text("Add Document")
                    }
                    OutlinedButton(onClick = onAddSpreadsheet) {
                        Text("Add Spreadsheet")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(evidenceList) { evidence -> // evidence is data.Evidence
                        val displayContent = if (evidence.content.length > 100) evidence.content.substring(0, 100) + "..." else evidence.content
                        val detailsList = listOfNotNull(
                            displayContent,
                            if (evidence.category.isNotBlank()) "Category: ${evidence.category}" else null, // Corrected for non-nullable String
                            evidence.tags.takeIf { it.isNotEmpty() }?.let { "Tags: ${it.joinToString(", ")}" }, // Corrected for non-nullable List
                            "Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(evidence.documentDate)}"
                        )
                        TaggedDataItem(item = Pair(evidence.sourceDocument, detailsList))
                    }
                }

            } else {
                // Image Review UI
                Column(
                    modifier = Modifier.fillMaxSize(), 
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = imageBitmapForReview!!.asImageBitmap(),
                        contentDescription = "Image for review",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) 
                            .padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { ocrViewModel.rotateImageBeingReviewed(-90f) }) {
                            Icon(Icons.Filled.RotateLeft, contentDescription = "Rotate Left")
                        }
                        IconButton(onClick = { ocrViewModel.rotateImageBeingReviewed(90f) }) {
                            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate Right")
                        }
                        IconButton(onClick = { ocrViewModel.confirmImageReview(context) }) {
                            Icon(Icons.Filled.Done, contentDescription = "Approve Image")
                        }
                        IconButton(onClick = { ocrViewModel.cancelImageReview() }) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Cancel Review")
                        }
                    }
                }
            }
        }
    }
}
