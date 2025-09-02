@file:OptIn(ExperimentalMaterial3Api::class)

package com.hereliesaz.lexorcist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import kotlin.random.Random

@Composable
fun LexorcistTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val randomColor = Color(
        red = Random.nextInt(256),
        green = Random.nextInt(256),
        blue = Random.nextInt(256)
    )
    DynamicMaterialTheme(
        seedColor = randomColor,
        typography = Typography,
        shapes = Shapes,
        isDark = useDarkTheme,
        content = content
    )
}
