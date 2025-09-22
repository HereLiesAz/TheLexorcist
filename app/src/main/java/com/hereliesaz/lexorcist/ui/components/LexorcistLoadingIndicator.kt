package com.hereliesaz.lexorcist.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.ui.theme.ThemeMode

@Composable
fun LexorcistLoadingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "LexorcistLogoAnimation")

    val animationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                0f at 0
                0.5f at 1000 with LinearEasing // Draw-in duration
                0.5f at 1200 // Hold duration
                1f at 2200 with LinearEasing // Fade-out duration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val drawProgress = (animationProgress * 2).coerceAtMost(1f)
    val alpha = if (animationProgress > 0.5f) 1f - (animationProgress - 0.5f) * 2 else 1f

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.size(64.dp)) {
        val strokeWidth = size.width * 0.08f
        val cap = StrokeCap.Round

        // Gavel
        val gavelHandleStart = Offset(size.width * 0.85f, size.height * 0.15f)
        val gavelHandleEnd = Offset(size.width * 0.15f, size.height * 0.85f)
        val gavelHeadStart = Offset(size.width * 0.1f, size.height * 0.3f)
        val gavelHeadEnd = Offset(size.width * 0.3f, size.height * 0.1f)

        // Pitchfork
        val pitchforkHandleStart = Offset(size.width * 0.15f, size.height * 0.15f)
        val pitchforkHandleEnd = Offset(size.width * 0.85f, size.height * 0.85f)
        val pitchforkTop = Offset(size.width * 0.15f, size.height * 0.15f)
        val tineLeftEnd = Offset(pitchforkTop.x - size.width * 0.08f, pitchforkTop.y + size.height * 0.1f)
        val tineRightEnd = Offset(pitchforkTop.x + size.width * 0.08f, pitchforkTop.y + size.height * 0.1f)
        val tineMiddleEnd = Offset(pitchforkTop.x, pitchforkTop.y + size.height * 0.15f)

        // Draw Gavel
        drawLine(primaryColor, gavelHandleStart, lerp(gavelHandleStart, gavelHandleEnd, drawProgress), strokeWidth, cap, alpha = alpha)
        drawLine(primaryColor, gavelHeadStart, lerp(gavelHeadStart, gavelHeadEnd, drawProgress), strokeWidth, cap, alpha = alpha)

        // Draw Pitchfork
        drawLine(secondaryColor, pitchforkHandleStart, lerp(pitchforkHandleStart, pitchforkHandleEnd, drawProgress), strokeWidth, cap, alpha = alpha)
        drawLine(secondaryColor, pitchforkTop, lerp(pitchforkTop, tineLeftEnd, drawProgress), strokeWidth, cap, alpha = alpha)
        drawLine(secondaryColor, pitchforkTop, lerp(pitchforkTop, tineRightEnd, drawProgress), strokeWidth, cap, alpha = alpha)
        drawLine(secondaryColor, pitchforkTop, lerp(pitchforkTop, tineMiddleEnd, drawProgress), strokeWidth, cap, alpha = alpha)
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
