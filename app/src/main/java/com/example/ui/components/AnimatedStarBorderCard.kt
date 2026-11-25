package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HdrGold
import com.example.ui.theme.HdrGoldLight
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanLight
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance AMOLED card frame featuring a continuous 5-second slow-motion 360°
 * electric blue/cyan running light beam with a layered inner backdrop mask so text and inputs
 * remain crisp without distortion even on low-end Android devices.
 */
@Composable
fun AnimatedStarBorderCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    highlightGoldMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "slow_beam_orbit")

    // Smooth 5-second continuous 360-degree loop
    val orbitProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_progress"
    )

    val starSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_spin"
    )

    val twinklePulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle_pulse"
    )

    val borderPath = remember { Path() }
    val primaryTailPath = remember { Path() }
    val secondaryTailPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }
    val starPath = remember { Path() }

    val primaryLeadColor = if (highlightGoldMode) HdrGoldLight else NeonCyan
    val primaryMidColor = if (highlightGoldMode) NeonMagenta else NeonCyanLight
    val primaryTailColor = if (highlightGoldMode) NeonCyan else ElectricBlue
    val counterColor = if (highlightGoldMode) HdrGold else ElectricBlue

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp)
    ) {
        // Layered dark AMOLED backdrop mask (keeps text sharp at high contrast)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ObsidianSurface,
                            Color(0xFF040710)
                        )
                    )
                ),
            content = content
        )

        // Lightweight hardware-accelerated Canvas perimeter beam overlay
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeInset = 1.5.dp.toPx()
            val radiusPx = cornerRadius.toPx()
            val w = size.width
            val h = size.height
            if (w <= strokeInset * 2 || h <= strokeInset * 2) return@Canvas

            borderPath.reset()
            borderPath.addRoundRect(
                RoundRect(
                    rect = Rect(strokeInset, strokeInset, w - strokeInset, h - strokeInset),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            )
            pathMeasure.setPath(borderPath, true)
            val totalLen = pathMeasure.length
            if (totalLen <= 0f) return@Canvas

            // 1. Crisp ambient border hairline
            drawPath(
                path = borderPath,
                color = if (highlightGoldMode) HdrGold.copy(alpha = 0.42f) else ObsidianCardBorder.copy(alpha = 0.85f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 2. Opposite-phase secondary cyan/blue aura beam (5-second synchronized)
            val secondaryEnd = ((orbitProgress + 0.5f) % 1f) * totalLen
            val secondaryLen = totalLen * 0.20f
            extractWrappedSegment(pathMeasure, totalLen, secondaryEnd - secondaryLen, secondaryEnd, secondaryTailPath)
            val secHead = pathMeasure.getPosition(secondaryEnd)
            val secTail = pathMeasure.getPosition(wrapDistance(secondaryEnd - secondaryLen, totalLen))
            if (secHead != Offset.Unspecified && secTail != Offset.Unspecified) {
                drawPath(
                    path = secondaryTailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            counterColor.copy(alpha = 0.45f),
                            NeonCyan.copy(alpha = 0.80f)
                        ),
                        start = secTail,
                        end = secHead
                    ),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Primary slow-motion 360-degree traveling cyan/blue light beam
            val headDist = orbitProgress * totalLen
            val tailLen = totalLen * 0.30f
            extractWrappedSegment(pathMeasure, totalLen, headDist - tailLen, headDist, primaryTailPath)

            val headPos = pathMeasure.getPosition(headDist)
            val tailPos = pathMeasure.getPosition(wrapDistance(headDist - tailLen, totalLen))

            if (headPos != Offset.Unspecified && tailPos != Offset.Unspecified) {
                // Soft low-cost outer neon bloom
                drawPath(
                    path = primaryTailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryTailColor.copy(alpha = 0.16f),
                            primaryMidColor.copy(alpha = 0.28f),
                            primaryLeadColor.copy(alpha = 0.42f)
                        ),
                        start = tailPos,
                        end = headPos
                    ),
                    style = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Sharp core beam
                drawPath(
                    path = primaryTailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryTailColor.copy(alpha = 0.65f),
                            primaryMidColor.copy(alpha = 0.90f),
                            primaryLeadColor,
                            Color.White
                        ),
                        start = tailPos,
                        end = headPos
                    ),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Subtle trailing sparkle dots along the beam
                val sparkleOffsets = floatArrayOf(0.04f, 0.09f)
                sparkleOffsets.forEachIndexed { idx, frac ->
                    val spDist = wrapDistance(headDist - totalLen * frac, totalLen)
                    val spPos = pathMeasure.getPosition(spDist)
                    if (spPos != Offset.Unspecified) {
                        val spAlpha = (0.85f - idx * 0.25f).coerceIn(0.2f, 1f)
                        val spRadius = (2.8f - idx * 0.7f).dp.toPx() * twinklePulse
                        drawFourPointSparkle(
                            center = spPos,
                            outerRadius = spRadius,
                            color = primaryLeadColor.copy(alpha = spAlpha),
                            path = starPath
                        )
                    }
                }

                // Lead star halo & rotating star glint
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.72f),
                            primaryLeadColor.copy(alpha = 0.42f),
                            primaryTailColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = headPos,
                        radius = 13.dp.toPx() * twinklePulse
                    ),
                    radius = 13.dp.toPx() * twinklePulse,
                    center = headPos
                )

                val starRadius = 6.0.dp.toPx() * twinklePulse
                rotate(degrees = starSpin, pivot = headPos) {
                    buildFivePointStarPath(
                        center = headPos,
                        outerRadius = starRadius,
                        innerRadius = starRadius * 0.45f,
                        outPath = starPath
                    )
                    drawPath(
                        path = starPath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, primaryLeadColor, primaryTailColor),
                            center = headPos,
                            radius = starRadius
                        )
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = 1.6.dp.toPx(),
                    center = headPos
                )
            }
        }
    }
}

private fun wrapDistance(distance: Float, totalLength: Float): Float {
    val mod = distance % totalLength
    return if (mod < 0f) mod + totalLength else mod
}

private fun extractWrappedSegment(
    pathMeasure: PathMeasure,
    totalLen: Float,
    startDist: Float,
    endDist: Float,
    destPath: Path
) {
    destPath.reset()
    val wrappedStart = wrapDistance(startDist, totalLen)
    if (startDist >= 0f && wrappedStart < endDist) {
        pathMeasure.getSegment(wrappedStart, endDist, destPath, true)
    } else {
        pathMeasure.getSegment(wrappedStart, totalLen, destPath, true)
        pathMeasure.getSegment(0f, endDist, destPath, true)
    }
}

private fun buildFivePointStarPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    outPath: Path
) {
    outPath.reset()
    val points = 5
    val step = PI / points
    var angle = -PI / 2.0
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) outPath.moveTo(x, y) else outPath.lineTo(x, y)
        angle += step
    }
    outPath.close()
}

private fun DrawScope.drawFourPointSparkle(
    center: Offset,
    outerRadius: Float,
    color: Color,
    path: Path
) {
    path.reset()
    val inner = outerRadius * 0.28f
    val step = PI / 4.0
    var angle = -PI / 2.0
    for (i in 0 until 8) {
        val r = if (i % 2 == 0) outerRadius else inner
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += step
    }
    path.close()
    drawPath(path = path, color = color)
}
