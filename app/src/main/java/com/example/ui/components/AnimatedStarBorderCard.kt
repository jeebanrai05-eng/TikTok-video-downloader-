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
import com.example.ui.theme.HdrGold
import com.example.ui.theme.HdrGoldLight
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.UltraPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Futuristic card frame featuring an orbiting neon energy light beam and a twinkling
 * multi-point Star traveling smoothly around the rounded border perimeter.
 */
@Composable
fun AnimatedStarBorderCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "star_border_orbit")

    val orbitProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_progress"
    )

    val starSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_spin"
    )

    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    val borderPath = remember { Path() }
    val primaryTailPath = remember { Path() }
    val secondaryTailPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }
    val starPath = remember { Path() }

    // Outer padding gives space for the glowing star & halo so it never clips at card edges
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp)
    ) {
        // Card body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ObsidianSurface,
                            Color(0xFF0A0E1B)
                        )
                    )
                ),
            content = content
        )

        // Perimeter Neon Beam + Traveling Star overlay
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

            // 1. Base ambient border
            drawPath(
                path = borderPath,
                color = ObsidianCardBorder.copy(alpha = 0.65f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 2. Counter-phase secondary subtle aura beam (opposite side of perimeter)
            val secondaryEnd = ((orbitProgress + 0.5f) % 1f) * totalLen
            val secondaryLen = totalLen * 0.14f
            extractWrappedSegment(pathMeasure, totalLen, secondaryEnd - secondaryLen, secondaryEnd, secondaryTailPath)
            val secHead = pathMeasure.getPosition(secondaryEnd)
            val secTail = pathMeasure.getPosition(wrapDistance(secondaryEnd - secondaryLen, totalLen))
            if (secHead != Offset.Unspecified && secTail != Offset.Unspecified) {
                drawPath(
                    path = secondaryTailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            UltraPurple.copy(alpha = 0.55f),
                            NeonMagenta.copy(alpha = 0.85f)
                        ),
                        start = secTail,
                        end = secHead
                    ),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Primary high-energy light comet beam behind the star
            val headDist = orbitProgress * totalLen
            val tailLen = totalLen * 0.24f
            extractWrappedSegment(pathMeasure, totalLen, headDist - tailLen, headDist, primaryTailPath)

            val headPos = pathMeasure.getPosition(headDist)
            val tailPos = pathMeasure.getPosition(wrapDistance(headDist - tailLen, totalLen))

            if (headPos != Offset.Unspecified && tailPos != Offset.Unspecified) {
                // Soft outer neon bloom
                drawPath(
                    path = primaryTailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            UltraPurple.copy(alpha = 0.16f),
                            NeonMagenta.copy(alpha = 0.24f),
                            NeonCyan.copy(alpha = 0.34f),
                            HdrGold.copy(alpha = 0.45f)
                        ),
                        start = tailPos,
                        end = headPos
                    ),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )

                // Mid laser beam
                drawPath(
                    path = primaryTailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            UltraPurple.copy(alpha = 0.65f),
                            NeonMagenta.copy(alpha = 0.85f),
                            NeonCyan,
                            HdrGoldLight
                        ),
                        start = tailPos,
                        end = headPos
                    ),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Trailing sparkle micro-stars along the light comet tail
                val sparkleOffsets = floatArrayOf(0.035f, 0.075f, 0.125f)
                sparkleOffsets.forEachIndexed { idx, frac ->
                    val spDist = wrapDistance(headDist - totalLen * frac, totalLen)
                    val spPos = pathMeasure.getPosition(spDist)
                    if (spPos != Offset.Unspecified) {
                        val spAlpha = (0.9f - idx * 0.24f).coerceIn(0.2f, 1f)
                        val spRadius = (3.4f - idx * 0.7f).dp.toPx() * (if (idx % 2 == 0) starTwinkle else 2f - starTwinkle)
                        drawFourPointSparkle(
                            center = spPos,
                            outerRadius = spRadius,
                            color = if (idx == 0) HdrGoldLight.copy(alpha = spAlpha) else NeonCyan.copy(alpha = spAlpha),
                            path = starPath
                        )
                    }
                }

                // 4. Main glowing aura + rotating 5-point Star at lead position
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HdrGoldLight.copy(alpha = 0.72f),
                            NeonCyan.copy(alpha = 0.38f),
                            NeonMagenta.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = headPos,
                        radius = 16.dp.toPx() * starTwinkle
                    ),
                    radius = 16.dp.toPx() * starTwinkle,
                    center = headPos
                )

                val starRadius = 7.2.dp.toPx() * starTwinkle
                rotate(degrees = starSpin, pivot = headPos) {
                    buildFivePointStarPath(
                        center = headPos,
                        outerRadius = starRadius,
                        innerRadius = starRadius * 0.44f,
                        outPath = starPath
                    )
                    drawPath(
                        path = starPath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, HdrGoldLight, HdrGold),
                            center = headPos,
                            radius = starRadius
                        )
                    )
                    drawPath(
                        path = starPath,
                        color = Color.White.copy(alpha = 0.9f),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }

                // Crisp bright center glint
                drawCircle(
                    color = Color.White,
                    radius = 1.9.dp.toPx(),
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
