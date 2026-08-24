package com.hereliesaz.lexorcist.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.PhotoGroupViewModel
import java.io.File
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGroupScreen(
    navController: NavController,
    caseViewModel: CaseViewModel = hiltViewModel(),
    photoGroupViewModel: PhotoGroupViewModel = hiltViewModel(),
    mainViewModel: MainViewModel,
) {
    val context = LocalContext.current
    val photoUris by photoGroupViewModel.photoUris.collectAsState()
    val description by photoGroupViewModel.description.collectAsState()
    var captureLost by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            // A successful capture with no pending URI means the saved state
            // was lost as well as the view model. Say so rather than dropping
            // the photograph without a word, which is what this did before.
            if (success && !photoGroupViewModel.onCaptureSucceeded()) {
                captureLost = true
            }
        },
    )

    val selectPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            photoGroupViewModel.addPhotos(uris)
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Photo Group",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { photoGroupViewModel.setDescription(it) },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(textAlign = TextAlign.End),
            )
            if (captureLost) {
                Text(
                    text = stringResource(R.string.photo_capture_interrupted),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                items(photoUris) { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                AzButton(
                    onClick = {
                        // filesDir, not cacheDir: the camera app runs while
                        // this process may be killed, and the system is free to
                        // clear the cache directory in the meantime.
                        val captureDir = File(context.filesDir, "pending_captures").apply { mkdirs() }
                        val tmpFile = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")
                        val tmpUri = FileProvider.getUriForFile(context, "com.hereliesaz.lexorcist.fileprovider", tmpFile)
                        photoGroupViewModel.setLatestTmpUri(tmpUri)
                        takePictureLauncher.launch(tmpUri)
                    },
                    text = stringResource(R.string.take_picture),
                )
                AzButton(
                    onClick = { selectPictureLauncher.launch("image/*") },
                    text = stringResource(R.string.select_photos),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AzButton(
                onClick = {
                    caseViewModel.addPhotoGroupEvidence(photoUris, description, "")
                    photoGroupViewModel.clear()
                    navController.popBackStack()
                },
                text = stringResource(R.string.save),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
