package com.hereliesaz.lexorcist.ui.components

import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NewLexorcistLoadingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "NewLexorcistLoadingAnimation")

    val animationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 0
                1f at 2500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.size(96.dp)) {
        val strokeWidth = size.width * 0.07f
        val cap = StrokeCap.Round

        // Animation phases based on progress
        val moveInProgress = (animationProgress / 0.4f).coerceAtMost(1f)
        val clashProgress = ((animationProgress - 0.4f) / 0.3f).coerceIn(0f, 1f)
        val fadeProgress = ((animationProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)
        val overallAlpha = (1f - fadeProgress)

        // Gavel animation
        withTransform({
            val easedProgress = EaseInOutCubic.transform(moveInProgress)
            translate(left = -size.width / 2 * (1 - easedProgress))
            rotate(degrees = -45f * easedProgress, pivot = center)
        }) {
            drawGavel(primaryColor.copy(alpha = overallAlpha), strokeWidth, cap)
        }

        // Pitchfork animation
        withTransform({
            val easedProgress = EaseInOutCubic.transform(moveInProgress)
            translate(left = size.width / 2 * (1 - easedProgress))
            rotate(degrees = 45f * easedProgress, pivot = center)
        }) {
            drawPitchfork(secondaryColor.copy(alpha = overallAlpha), strokeWidth, cap)
        }

        // Particle clash animation
        if (clashProgress > 0 && clashProgress < 1f) {
            val particleAlpha = (1f - clashProgress) * overallAlpha
            val particleCount = 8
            for (i in 0 until particleCount) {
                val angle = (2 * Math.PI / particleCount * i).toFloat()
                val radius = size.width * 0.25f * clashProgress
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                drawCircle(
                    color = if (i % 2 == 0) primaryColor else secondaryColor,
                    radius = size.width * 0.04f * (1f - clashProgress),
                    center = Offset(x, y),
                    alpha = particleAlpha
                )
            }
        }
    }
}

private fun DrawScope.drawGavel(color: Color, strokeWidth: Float, cap: StrokeCap) {
    // Gavel (adjusted for centered drawing)
    val handleStart = Offset(size.width * 0.85f, size.height * 0.15f)
    val handleEnd = Offset(size.width * 0.15f, size.height * 0.85f)
    val headStart = Offset(size.width * 0.1f, size.height * 0.3f)
    val headEnd = Offset(size.width * 0.3f, size.height * 0.1f)
    drawLine(color, handleStart, handleEnd, strokeWidth, cap)
    drawLine(color, headStart, headEnd, strokeWidth, cap)
}

private fun DrawScope.drawPitchfork(color: Color, strokeWidth: Float, cap: StrokeCap) {
    // Pitchfork (adjusted for centered drawing)
    val handleStart = Offset(size.width * 0.15f, size.height * 0.15f)
    val handleEnd = Offset(size.width * 0.85f, size.height * 0.85f)
    val top = Offset(size.width * 0.15f, size.height * 0.15f)
    val tineLeftEnd = Offset(top.x - size.width * 0.08f, top.y + size.height * 0.1f)
    val tineRightEnd = Offset(top.x + size.width * 0.08f, top.y + size.height * 0.1f)
    val tineMiddleEnd = Offset(top.x, top.y + size.height * 0.15f)
    drawLine(color, handleStart, handleEnd, strokeWidth, cap)
    drawLine(color, top, tineLeftEnd, strokeWidth, cap)
    drawLine(color, top, tineRightEnd, strokeWidth, cap)
    drawLine(color, top, tineMiddleEnd, strokeWidth, cap)
}

@Preview(showBackground = true)
@Composable
fun NewLexorcistLoadingIndicatorPreview() {
    LexorcistTheme(themeMode = ThemeMode.DARK) {
        NewLexorcistLoadingIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun NewLexorcistLoadingIndicatorPreviewLight() {
    LexorcistTheme(themeMode = ThemeMode.LIGHT) {
        NewLexorcistLoadingIndicator()
    }
}
