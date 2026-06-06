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


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable default scheme for custom branding
  content: @Composable () -> Unit,
) {
  isAppInDarkMode.value = darkTheme

  val colorScheme = if (darkTheme) {
    darkColorScheme(
      primary = BrandPrimary,
      secondary = BrandSecondary,
      tertiary = BrandIndigo,
      background = BrandBackground,
      surface = BrandSurface,
      onPrimary = BrandCard,
      onSecondary = BrandCard,
      onBackground = BrandText,
      onSurface = BrandText
    )
  } else {
    lightColorScheme(
      primary = BrandPrimary,
      secondary = BrandSecondary,
      tertiary = BrandIndigo,
      background = BrandBackground,
      surface = BrandSurface,
      onPrimary = BrandCard,
      onSecondary = BrandCard,
      onBackground = BrandText,
      onSurface = BrandText,
    )
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

