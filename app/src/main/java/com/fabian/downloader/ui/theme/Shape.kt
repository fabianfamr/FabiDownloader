package com.fabian.downloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas expresivas basadas en las especificaciones de Material 3 Expressive:
 * Radio aumentado, curvatura orgánica y esquinas de contenedor generosas.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val PillShape = RoundedCornerShape(percent = 50)
val ExpressiveCardShape = RoundedCornerShape(24.dp)
val ExpressiveButtonShape = RoundedCornerShape(18.dp)
val ExpressiveChipShape = RoundedCornerShape(14.dp)
