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
import androidx.compose.ui.platform.LocalContext

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

private val LightFabiColors = FabiColors(
    background = Color(0xFFF8FAFC),
    card = Color(0xFFFFFFFF),
    cardSecondary = Color(0xFFF1F5F9),
    border = Color(0xFFE2E8F0),
    accent = AccentBlue,
    accentDim = AccentBlue.copy(alpha = 0.12f),
    accentGlow = AccentBlue.copy(alpha = 0.20f),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    textDisabled = Color(0xFF94A3B8),
    error = Red,
    errorDim = RedDim,
    success = Green,
    amber = Amber,
    sheet = Color(0xFFFFFFFF),
) 

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
    dynamicColor: Boolean = true,
    accentColorName: String = "Azul Eléctrico",
    amoledMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themePreference) {
        "Claro" -> false
        "Oscuro" -> true
        else -> isSystemInDarkTheme()
    }
    
    // Choose accent base
    val selectedAccent = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        null // Will use dynamic
    } else {
        when (accentColorName) {
            "Verde Esmeralda" -> AccentGreen
            "Púrpura Real" -> AccentPurple
            "Naranja Sunset" -> AccentOrange
            "Rosa Hot" -> AccentPink
            "Gris Acero" -> AccentSteel
            else -> AccentBlue
        }
    }

    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            if (selectedAccent != null) {
                DarkColorScheme.copy(
                    primary = selectedAccent,
                    secondary = selectedAccent,
                    primaryContainer = selectedAccent.copy(alpha = 0.15f)
                )
            } else DarkColorScheme
        }
        else -> {
             if (selectedAccent != null) {
                LightColorScheme.copy(
                    primary = selectedAccent,
                    secondary = selectedAccent
                )
            } else LightColorScheme
        }
    }

    if (darkTheme && amoledMode) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
    }

    val effectiveAccent = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorScheme.primary
    } else {
        selectedAccent ?: AccentBlue
    }

    val baseFabiColors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            DarkFabiColors.copy(
                background = colorScheme.background,
                card = colorScheme.surface,
                cardSecondary = colorScheme.surfaceVariant,
                border = colorScheme.outline.copy(alpha = 0.3f),
                accent = effectiveAccent,
                accentDim = effectiveAccent.copy(alpha = 0.15f),
                accentGlow = effectiveAccent.copy(alpha = 0.25f),
                textPrimary = colorScheme.onBackground,
                textSecondary = colorScheme.onSurfaceVariant,
                sheet = colorScheme.surface
            )
        } else {
            LightFabiColors.copy(
                background = colorScheme.background,
                card = colorScheme.surface,
                cardSecondary = colorScheme.surfaceVariant,
                border = colorScheme.outline.copy(alpha = 0.3f),
                accent = effectiveAccent,
                accentDim = effectiveAccent.copy(alpha = 0.15f),
                accentGlow = effectiveAccent.copy(alpha = 0.25f),
                textPrimary = colorScheme.onBackground,
                textSecondary = colorScheme.onSurfaceVariant,
                sheet = colorScheme.surface
            )
        }
    } else if (darkTheme) {
        DarkFabiColors.copy(
            accent = effectiveAccent,
            accentDim = effectiveAccent.copy(alpha = 0.12f),
            accentGlow = effectiveAccent.copy(alpha = 0.22f)
        )
    } else {
        LightFabiColors.copy(
            accent = effectiveAccent,
            accentDim = effectiveAccent.copy(alpha = 0.12f),
            accentGlow = effectiveAccent.copy(alpha = 0.20f)
        )
    }

    val fabiColors = if (darkTheme && amoledMode) {
        baseFabiColors.copy(
            background = Color.Black,
            card = Color(0xFF070708),
            cardSecondary = Color(0xFF101012),
            border = Color(0xFF1E1E24),
            sheet = Color(0xFF050506)
        )
    } else baseFabiColors

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
