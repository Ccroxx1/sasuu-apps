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

import androidx.compose.ui.graphics.Color

private val SophisticatedPrimary = Color(0xFFD0BCFF)
private val SophisticatedOnPrimary = Color(0xFF21005D)
private val SophisticatedPrimaryContainer = Color(0xFF381E72)
private val SophisticatedOnPrimaryContainer = Color(0xFFEADDFF)
private val SophisticatedSecondary = Color(0xFFCCC2DC)
private val SophisticatedOnSecondary = Color(0xFF332D41)
private val SophisticatedSecondaryContainer = Color(0xFF4A4458)
private val SophisticatedOnSecondaryContainer = Color(0xFFE8DEF8)
private val SophisticatedBackground = Color(0xFF1C1B1F)
private val SophisticatedOnBackground = Color(0xFFE6E1E5)
private val SophisticatedSurface = Color(0xFF2B2930)
private val SophisticatedOnSurface = Color(0xFFE6E1E5)
private val SophisticatedSurfaceVariant = Color(0xFF49454F)
private val SophisticatedOnSurfaceVariant = Color(0xFFCAC4D0)
private val SophisticatedOutline = Color(0xFF49454F)

private val DarkColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    onSecondaryContainer = SophisticatedOnSecondaryContainer,
    background = SophisticatedBackground,
    onBackground = SophisticatedOnBackground,
    surface = SophisticatedSurface,
    onSurface = SophisticatedOnSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline
)

private val LightColorScheme = DarkColorScheme // Force sophisticated dark mode for consistent premium experience

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to dark theme for "Sophisticated Dark"
  dynamicColor: Boolean = false, // Always preserve our custom theme
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
