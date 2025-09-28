package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import com.hereliesaz.aznavrail.AzButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecordAudioScreen(
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    isRecording: Boolean,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp), // Apply padding to the outer Box
    ) {
        val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

        Column(
            modifier =
                Modifier
                    .fillMaxSize() // Column fills the BoxWithConstraints
                    .verticalScroll(rememberScrollState()),
            // Make content scrollable
            horizontalAlignment = Alignment.End, // Right-align children of this Column
        ) {
            Spacer(modifier = Modifier.height(halfScreenHeight)) // Push content to start halfway down

            if (isRecording) {
                AzButton(onClick = onStopRecording, text = "Stop Recording")
            } else {
                AzButton(onClick = onStartRecording, text = "Start Recording")
            }
        }
    }
}
