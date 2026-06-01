package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BlueContainer,
    onPrimary = DeepDarkBlue,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = SurfaceWhite,
    secondary = AccentTeal,
    onSecondary = DeepDarkBlue,
    background = DeepDarkBlue,
    surface = DeepDarkBlue,
    onBackground = SurfaceWhite,
    onSurface = SurfaceWhite,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = LightGrayBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = BlueContainer,
    onPrimaryContainer = DeepDarkBlue,
    secondary = AccentTeal,
    onSecondary = SurfaceWhite,
    background = MinimalBg,
    surface = SurfaceWhite,
    onBackground = DeepDarkBlue,
    onSurface = DeepDarkBlue,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate400,
    outline = LightGrayBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  // Set default dynamicColor to false to maintain the exact branding palette requested
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
