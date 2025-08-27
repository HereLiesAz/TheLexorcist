package com.hereliesaz.lexorcist.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    var selectedItem by remember { mutableStateOf("home") }
    val items = listOf("home", "cases", "add_evidence", "timeline", "settings")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.List,
        Icons.Default.Add,
        Icons.Default.Schedule,
        Icons.Default.Settings
    )
    val labels = listOf("Home", "Cases", "Add Evidence", "Timeline", "Settings")

    NavigationRail {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = { Icon(icons[index], contentDescription = labels[index]) },
                label = { Text(labels[index]) },
                selected = selectedItem == item,
                onClick = {
                    selectedItem = item
                    onNavigate(item)
                }
            )
        }
    }
}
