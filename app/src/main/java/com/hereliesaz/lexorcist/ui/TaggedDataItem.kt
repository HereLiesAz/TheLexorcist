package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TaggedDataItem(item: Pair<String, List<String>>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth() // Ensure the Column takes full width for alignment reference
                .padding(8.dp),
        horizontalAlignment = Alignment.End, // Right-align children (Text blocks)
    ) {
        Text(
            text = item.first,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            // textAlign will remain default (start)
        )
        Text(
            text = item.second.joinToString("\n"),
            modifier = Modifier.padding(top = 4.dp),
            // textAlign will remain default (start)
        )
    }
}
