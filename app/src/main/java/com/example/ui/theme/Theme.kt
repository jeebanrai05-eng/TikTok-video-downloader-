package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AmoledVibrantProColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF021114),
    primaryContainer = ObsidianSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Color.White,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = NeonMagenta,
    tertiary = HdrGold,
    onTertiary = Color(0xFF1A1200),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    outlineVariant = Color(0xFF16223A),
    error = ErrorRed,
    onError = Color.White
)

private val TikSaveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AmoledVibrantProColorScheme,
        typography = Typography,
        shapes = TikSaveShapes,
        content = content
    )
}
