package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CinemaPrimaryCyan,
    onPrimary = Color(0xFF031E2B),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = CinemaSecondaryAmber,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = CinemaTertiaryRose,
    onTertiary = Color(0xFF4C0519),
    background = CinemaBackgroundDark,
    onBackground = CinemaTextPrimaryDark,
    surface = CinemaSurfaceDark,
    onSurface = CinemaTextPrimaryDark,
    surfaceVariant = CinemaSurfaceVariantDark,
    onSurfaceVariant = CinemaTextSecondaryDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CinemaPrimaryCyanLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = CinemaSecondaryAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = CinemaTertiaryRoseLight,
    onTertiary = Color.White,
    background = CinemaBackgroundLight,
    onBackground = CinemaTextPrimaryLight,
    surface = CinemaSurfaceLight,
    onSurface = CinemaTextPrimaryLight,
    surfaceVariant = CinemaSurfaceVariantLight,
    onSurfaceVariant = CinemaTextSecondaryLight
  )

@Composable
fun CineRecapTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For cinematic mood, we favor dark theme or dynamic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> DarkColorScheme // Cinema apps look premier in dark mode by default
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
