package com.hereliesaz.lexorcist.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@Composable
fun AppNavRail(onNavigate: (String) -> Unit) {
    var selectedItem by remember { mutableStateOf("home") }
    val items =
        listOf(
            "home",
            "cases",
            "evidence",
            "allegations",
            "templates",
            "timeline",
            "data_review",
            "extras",
            "script_editor",
            "settings",
        )

    val icons =
        listOf(
            Icons.Default.Home,
            Icons.Default.AccountBox,
            Icons.Default.List,
            Icons.Default.ThumbUp,
            Icons.Default.Info,
            Icons.Default.DateRange,
            Icons.Default.Check,
            Icons.Default.Build,
            Icons.Default.Edit,
            Icons.Default.Settings,
        )

    val strings =
        listOf(
            R.string.nav_home,
            R.string.nav_cases,
            R.string.nav_evidence,
            R.string.allegations,
            R.string.templates,
            R.string.nav_timeline,
            R.string.data_review,
            R.string.extras,
            R.string.script_editor,
            R.string.nav_settings,
        )

    NavigationRail {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = { Icon(icons[index], contentDescription = stringResource(strings[index])) },
                label = { Text(stringResource(strings[index])) },
                selected = selectedItem == item,
                onClick = {
                    selectedItem = item
                    onNavigate(item)
                },
            )
        }
    }
}
