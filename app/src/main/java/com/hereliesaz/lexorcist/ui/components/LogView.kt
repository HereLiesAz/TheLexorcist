package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hereliesaz.lexorcist.model.LogEntry

@Composable
fun LogView(logs: List<LogEntry>) {
    LazyColumn {
        items(logs) { log ->
            Text(text = "[${log.level}] ${log.message}")
        }
    }
}
