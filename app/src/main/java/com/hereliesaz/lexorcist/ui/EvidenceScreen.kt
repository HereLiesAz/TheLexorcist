package com.hereliesaz.lexorcist.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import java.util.Locale

@Composable
fun EvidenceScreen(
    evidenceViewModel: EvidenceViewModel = viewModel(),
    navController: NavController,
) {
    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { evidenceViewModel::processImageEvidence }
        }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { evidenceViewModel::processAudioEvidence }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Top,
    ) {
        Button(onClick = { navController.navigate("add_text_evidence") }) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault()))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault()))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Icon(Icons.Default.Image, contentDescription = stringResource(R.string.add_image_evidence).uppercase(Locale.getDefault()))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_image_evidence).uppercase(Locale.getDefault()))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { audioPickerLauncher.launch("audio/*") }) {
            Icon(Icons.Default.Audiotrack, contentDescription = stringResource(R.string.add_audio_evidence).uppercase(Locale.getDefault()))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_audio_evidence).uppercase(Locale.getDefault()))
        }
    }
}
