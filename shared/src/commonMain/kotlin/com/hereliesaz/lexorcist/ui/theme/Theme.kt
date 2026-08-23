package com.hereliesaz.lexorcist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode { Light, Dark, System }

/**
 * The app theme, shared across Android, desktop and iOS.
 *
 * The Android-only predecessor built its colour scheme from
 * `Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))`
 * evaluated directly in the composable body with no `remember`, so the entire
 * palette was re-rolled on every recomposition of the theme. The scheme here
 * is fixed and legible, which is what a document-handling tool needs.
 */
@Composable
fun LexorcistTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (dark) LexorcistDarkColors else LexorcistLightColors,
        typography = LexorcistTypography,
        shapes = LexorcistShapes,
        content = content,
    )
}

private val Ink = Color(0xFF11131A)
private val Parchment = Color(0xFFFAF8F4)
private val Oxblood = Color(0xFF8C2F30)
private val OxbloodLight = Color(0xFFE79495)
private val Brass = Color(0xFF9A7B3F)
private val BrassLight = Color(0xFFE0C289)
private val Slate = Color(0xFF4A5266)
private val SlateLight = Color(0xFFB9C1D6)

internal val LexorcistLightColors = lightColorScheme(
    primary = Oxblood,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF40000A),
    secondary = Slate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Brass,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA8),
    onTertiaryContainer = Color(0xFF2A1800),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7E1DA),
    onSurfaceVariant = Color(0xFF4A453E),
    outline = Color(0xFF7C766E),
    outlineVariant = Color(0xFFCDC7BF),
)

internal val LexorcistDarkColors = darkColorScheme(
    primary = OxbloodLight,
    onPrimary = Color(0xFF561D1F),
    primaryContainer = Color(0xFF723033),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = SlateLight,
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4658),
    onSecondaryContainer = Color(0xFFDCE2F9),
    tertiary = BrassLight,
    onTertiary = Color(0xFF453000),
    tertiaryContainer = Color(0xFF624600),
    onTertiaryContainer = Color(0xFFFFDEA8),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Ink,
    onBackground = Color(0xFFE6E1E5),
    surface = Ink,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF4A453E),
    onSurfaceVariant = Color(0xFFCDC7BF),
    outline = Color(0xFF968F87),
    outlineVariant = Color(0xFF4A453E),
)

internal val LexorcistShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
