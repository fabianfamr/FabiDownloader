package com.fabian.downloader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window Width Size Classes conformes con Material 3 adaptativo:
 * - Compact: Teléfonos en vertical (< 600dp)
 * - Medium: Teléfonos en horizontal, dispositivos plegables y tablets pequeñas (600dp - 839dp)
 * - Expanded: Tablets en horizontal, pantallas grandes y Chromebooks (>= 840dp)
 */
enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded;

    val isWide: Boolean
        get() = this != Compact

    val isExpanded: Boolean
        get() = this == Expanded
}

val LocalWindowWidthClass = staticCompositionLocalOf { WindowWidthClass.Compact }

@Composable
fun rememberWindowWidthClass(maxWidth: Dp): WindowWidthClass {
    return when {
        maxWidth < 600.dp -> WindowWidthClass.Compact
        maxWidth < 840.dp -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }
}
