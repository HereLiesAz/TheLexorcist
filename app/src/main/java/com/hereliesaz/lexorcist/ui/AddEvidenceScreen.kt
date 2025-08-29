package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.ui.TaggedDataItem
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import com.hereliesaz.lexorcist.TaggedDataAdapter

@Composable
fun AddEvidenceScreen(
    evidenceViewModel: EvidenceViewModel,
    ocrViewModel: OcrViewModel,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit,
    onAddTextEvidence: () -> Unit,
    onAddDocument: () -> Unit,
    onAddSpreadsheet: () -> Unit
) {
    val imageBitmapForReview by ocrViewModel.imageBitmapForReview.collectAsState()
    val isOcrInProgress by ocrViewModel.isOcrInProgress.collectAsState()
    val newlyCreatedEvidence by ocrViewModel.newlyCreatedEvidence.collectAsState()
    val uiEvidenceList by evidenceViewModel.uiEvidenceList.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(newlyCreatedEvidence) {
        newlyCreatedEvidence?.let {
            evidenceViewModel.addEvidenceToUiList(it)
        }
    }

    if (isOcrInProgress) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Processing Image...")
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
                // Buttons for adding new evidence - shown when no image is under review
                Column(
                    horizontalAlignment = Alignment.End, // Center buttons in this column
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onSelectImage) {
                        Text("Select Image")
                    }
                    Button(onClick = onTakePicture) {
                        Text("Take Picture")
                    }
                    Button(onClick = onAddTextEvidence) {
                        Text("Add Text Evidence")
                    }
                    Button(onClick = onAddDocument) {
                        Text("Add Document")
                    }
                    Button(onClick = onAddSpreadsheet) {
                        Text("Add Spreadsheet")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TODO: Re-implement this with a Composable, not RecyclerView
                // For now, just display the list of evidence content
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(uiEvidenceList) { evidence ->
                        Text(evidence.content)
                    }
                }

            // LazyColumn for tagged data - shown when no image is under review
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Allow LazyColumn to take available space
            ) {
                items(taggedData.toList()) { item ->
                    TaggedDataItem(item = item)
                }
            }

            } else {
                // Image Review UI - shown when an image is selected/taken
                Column(
                    modifier = Modifier.fillMaxSize(), // Take full screen for review
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = imageBitmapForReview!!.asImageBitmap(),
                        contentDescription = "Image for review",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Image takes most of the space
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
}
