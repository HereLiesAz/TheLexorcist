package com.hereliesaz.lexorcist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColorScheme(
    primary = Purple200,
    secondary = Teal200,
    background = Color(0xFF121212),
    surface = Color(0xFF121212)
)

private val LightColorPalette = lightColorScheme(
    primary = Purple500,
    secondary = Teal200,
    background = Color(0xFF121212),
    surface = Color(0xFF121212)
)

@Composable
fun LexorcistTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
