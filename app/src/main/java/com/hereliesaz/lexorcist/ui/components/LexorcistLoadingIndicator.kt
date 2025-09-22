package com.hereliesaz.lexorcist.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.ui.theme.ThemeMode

@Composable
fun LexorcistLoadingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "LexorcistLoadingIndicator")

    // Animation for the first arc
    val rotation1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation1"
    )
    val startAngle1 by transition.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "startAngle1"
    )
    val sweepAngle1 by transition.animateFloat(
        initialValue = 120f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sweepAngle1"
    )

    // Animation for the second arc
    val rotation2 by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation2"
    )
    val startAngle2 by transition.animateFloat(
        initialValue = 90f,
        targetValue = -270f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "startAngle2"
    )
    val sweepAngle2 by transition.animateFloat(
        initialValue = 120f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sweepAngle2"
    )

    // Animation for the third arc
    val rotation3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation3"
    )
    val startAngle3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "startAngle3"
    )
    val sweepAngle3 by transition.animateFloat(
        initialValue = 60f,
        targetValue = 160f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sweepAngle3"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.size(64.dp)) {
        val strokeWidth = size.width * 0.1f
        val diameter = size.width - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        // Draw the first arc
        drawArc(
            color = primaryColor,
            startAngle = rotation1 + startAngle1,
            sweepAngle = sweepAngle1,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth)
        )

        // Draw the second arc
        drawArc(
            color = secondaryColor,
            startAngle = rotation2 + startAngle2,
            sweepAngle = sweepAngle2,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth)
        )

        // Draw the third arc
        drawArc(
            color = tertiaryColor,
            startAngle = rotation3 + startAngle3,
            sweepAngle = sweepAngle3,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth * 0.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LexorcistLoadingIndicatorPreview() {
    LexorcistTheme(themeMode = ThemeMode.DARK) {
        LexorcistLoadingIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun LexorcistLoadingIndicatorPreviewLight() {
    LexorcistTheme(themeMode = ThemeMode.LIGHT) {
        LexorcistLoadingIndicator()
    }
}
