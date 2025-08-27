package com.hereliesaz.lexorcist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color // Compose Color
import java.util.Random
// Import from the material-color-utilities, which is a dependency of androidx.compose.material3
import com.google.android.material.color.utilities.Scheme

// The old static schemes can be removed or commented out if no longer needed as fallbacks.
// private val DarkColorScheme = darkColorScheme(
// primary = Purple80,
// secondary = PurpleGrey80,
// tertiary = Pink80
// )
//
// private val LightColorScheme = lightColorScheme(
// primary = Purple40,
// secondary = PurpleGrey40,
// tertiary = Pink40
// )

// This function does not need to be @Composable
fun generateRandomColorScheme(isDark: Boolean): ColorScheme {
    val random = Random()
    // Generate a random ARGB color. Alpha is FF for opaque seed colors.
    val seedColorInt = (0xFF shl 24) or random.nextInt(0x00FFFFFF + 1) // Opaque random color

    val mcuScheme = if (isDark) { // MCU (Material Color Utilities) scheme
        Scheme.dark(seedColorInt)
    } else {
        Scheme.light(seedColorInt)
    }

    // Map the MCU Scheme's ARGB Int colors to Compose Colors and create Material3 ColorScheme
    return if (isDark) {
        darkColorScheme(
            primary = Color(mcuScheme.primary),
            onPrimary = Color(mcuScheme.onPrimary),
            primaryContainer = Color(mcuScheme.primaryContainer),
            onPrimaryContainer = Color(mcuScheme.onPrimaryContainer),
            inversePrimary = Color(mcuScheme.inversePrimary),
            secondary = Color(mcuScheme.secondary),
            onSecondary = Color(mcuScheme.onSecondary),
            secondaryContainer = Color(mcuScheme.secondaryContainer),
            onSecondaryContainer = Color(mcuScheme.onSecondaryContainer),
            tertiary = Color(mcuScheme.tertiary),
            onTertiary = Color(mcuScheme.onTertiary),
            tertiaryContainer = Color(mcuScheme.tertiaryContainer),
            onTertiaryContainer = Color(mcuScheme.onTertiaryContainer),
            background = Color(mcuScheme.background),
            onBackground = Color(mcuScheme.onBackground),
            surface = Color(mcuScheme.surface),
            onSurface = Color(mcuScheme.onSurface),
            surfaceVariant = Color(mcuScheme.surfaceVariant),
            onSurfaceVariant = Color(mcuScheme.onSurfaceVariant),
            surfaceTint = Color(mcuScheme.primary), // surfaceTint is often the same as primary in M3
            inverseSurface = Color(mcuScheme.inverseSurface),
            inverseOnSurface = Color(mcuScheme.inverseOnSurface),
            error = Color(mcuScheme.error),
            onError = Color(mcuScheme.onError),
            errorContainer = Color(mcuScheme.errorContainer),
            onErrorContainer = Color(mcuScheme.onErrorContainer),
            outline = Color(mcuScheme.outline),
            outlineVariant = Color(mcuScheme.outlineVariant),
            scrim = Color(mcuScheme.scrim)
        )
    } else {
        lightColorScheme(
            primary = Color(mcuScheme.primary),
            onPrimary = Color(mcuScheme.onPrimary),
            primaryContainer = Color(mcuScheme.primaryContainer),
            onPrimaryContainer = Color(mcuScheme.onPrimaryContainer),
            inversePrimary = Color(mcuScheme.inversePrimary),
            secondary = Color(mcuScheme.secondary),
            onSecondary = Color(mcuScheme.onSecondary),
            secondaryContainer = Color(mcuScheme.secondaryContainer),
            onSecondaryContainer = Color(mcuScheme.onSecondaryContainer),
            tertiary = Color(mcuScheme.tertiary),
            onTertiary = Color(mcuScheme.onTertiary),
            tertiaryContainer = Color(mcuScheme.tertiaryContainer),
            onTertiaryContainer = Color(mcuScheme.onTertiaryContainer),
            background = Color(mcuScheme.background),
            onBackground = Color(mcuScheme.onBackground),
            surface = Color(mcuScheme.surface),
            onSurface = Color(mcuScheme.onSurface),
            surfaceVariant = Color(mcuScheme.surfaceVariant),
            onSurfaceVariant = Color(mcuScheme.onSurfaceVariant),
            surfaceTint = Color(mcuScheme.primary), // surfaceTint is often the same as primary in M3
            inverseSurface = Color(mcuScheme.inverseSurface),
            inverseOnSurface = Color(mcuScheme.inverseOnSurface),
            error = Color(mcuScheme.error),
            onError = Color(mcuScheme.onError),
            errorContainer = Color(mcuScheme.errorContainer),
            onErrorContainer = Color(mcuScheme.onErrorContainer),
            outline = Color(mcuScheme.outline),
            outlineVariant = Color(mcuScheme.outlineVariant),
            scrim = Color(mcuScheme.scrim)
        )
    }
}

@Composable
fun LexorcistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Generate the color scheme once and remember it across recompositions,
    // re-generating only if darkTheme boolean changes.
    val colorScheme = remember(darkTheme) {
        generateRandomColorScheme(darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming Typography is defined (e.g., in Typography.kt)
        content = content
    )
}
