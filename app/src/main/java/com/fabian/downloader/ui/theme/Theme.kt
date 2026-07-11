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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// ---------------------------------------------------------------------------
// FabiColors System
// ---------------------------------------------------------------------------

data class FabiColors(
    val background: Color,
    val card: Color,
    val cardSecondary: Color,
    val border: Color,
    val accent: Color,
    val accentDim: Color,
    val accentGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val error: Color,
    val errorDim: Color,
    val success: Color,
    val amber: Color,
    val sheet: Color,
)

private val DarkFabiColors = FabiColors(
    background = Bg,
    card = Card,
    cardSecondary = Card2,
    border = Border,
    accent = Accent,
    accentDim = AccentDim,
    accentGlow = AccentGlow,
    textPrimary = White,
    textSecondary = Gray1,
    textMuted = Gray2,
    textDisabled = Gray3,
    error = Red,
    errorDim = RedDim,
    success = Green,
    amber = Amber,
    sheet = Sheet,
)

// We don't have a light palette defined in the example, so we fallback to dark for now
// or define a basic light one if needed. The app seems to be dark-only in spirit.
private val LightFabiColors = DarkFabiColors 

val LocalFabiColors = staticCompositionLocalOf { DarkFabiColors }

val MaterialTheme.fabiColors: FabiColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFabiColors.current

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    primaryContainer = AccentDim,
    onPrimaryContainer = Accent,
    secondary = Accent,
    onSecondary = Bg,
    background = Bg,
    onBackground = White,
    surface = Card,
    onSurface = White,
    surfaceVariant = Card2,
    onSurfaceVariant = Gray1,
    outline = Border,
    error = Red,
    onError = White,
    errorContainer = RedDim,
    onErrorContainer = Red
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
    val fabiColors = if (darkTheme) DarkFabiColors else LightFabiColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalFabiColors provides fabiColors) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
