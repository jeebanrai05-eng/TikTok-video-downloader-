package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = Color.Black,
  primaryContainer = ObsidianSurfaceVariant,
  onPrimaryContainer = NeonCyan,
  secondary = NeonMagenta,
  onSecondary = Color.White,
  secondaryContainer = ObsidianSurfaceVariant,
  onSecondaryContainer = NeonMagenta,
  tertiary = HdrGold,
  onTertiary = Color.Black,
  background = ObsidianBg,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = ObsidianCardBorder,
  outlineVariant = Color(0xFF222638)
)

private val LightColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = Color.Black,
  primaryContainer = ObsidianSurfaceVariant,
  onPrimaryContainer = NeonCyan,
  secondary = NeonMagenta,
  onSecondary = Color.White,
  tertiary = HdrGold,
  background = ObsidianBg,
  surface = ObsidianSurface,
  onBackground = TextPrimary,
  onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep intentional brand neon palette
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme, // TikSave uses vibrant midnight dark theme by default
    typography = Typography,
    content = content
  )
}
