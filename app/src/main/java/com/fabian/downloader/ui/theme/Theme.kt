package com.fabian.downloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Cyan/Teal Accent
    onPrimary = Color(0xFF00333A),
    secondary = Color(0xFF2979FF), // Electric Blue
    onSecondary = Color.White,
    tertiary = Color(0xFF1DE9B6),
    surface = Color(0xFF0C0C0E), // Very dark charcoal surface
    onSurface = Color.White,
    background = Color(0xFF000000), // Pure AMOLED Black background
    onBackground = Color.White,
    surfaceVariant = Color(0xFF161619), // Dark slate/charcoal variant
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF242428)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B8D4),
    onPrimary = Color.White,
    secondary = Color(0xFF2979FF),
    onSecondary = Color.White,
    tertiary = Color(0xFF00BFA5),
    surface = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    themePreference: String = "Sistema",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        "Claro" -> false
        "Oscuro" -> true
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            val window = activity?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
