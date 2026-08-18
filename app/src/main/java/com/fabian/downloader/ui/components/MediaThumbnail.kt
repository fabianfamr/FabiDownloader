package com.fabian.downloader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.ui.theme.fabiColors

/**
 * Determina si un formato corresponde a música/audio.
 */
fun isAudioFormat(format: String?): Boolean {
    if (format.isNullOrBlank()) return false
    val f = format.uppercase().trim()
    return f == Config.FORMAT_MP3 ||
            f == Config.FORMAT_M4A ||
            f == Config.FORMAT_OGG ||
            f == Config.FORMAT_WAV ||
            f == "AAC" ||
            f == "FLAC" ||
            f == "OPUS" ||
            f.contains("MP3") ||
            f.contains("AUDIO")
}

/**
 * Determina si un formato corresponde a imagen.
 */
fun isImageFormat(format: String?): Boolean {
    if (format.isNullOrBlank()) return false
    val f = format.uppercase().trim()
    return f == Config.FORMAT_JPG ||
            f == Config.FORMAT_PNG ||
            f == Config.FORMAT_WEBP ||
            f == "JPEG"
}

/**
 * Componente principal para renderizar la miniatura adecuada según el tipo de medio.
 * Para audio/música muestra un disco de vinilo estilizado con la portada centrada (estilo Snaptube).
 * Para video/imagen muestra la carátula rectangular con esquinas redondeadas.
 */
@Composable
fun MediaThumbnail(
    record: DownloadRecord,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    isFailed: Boolean = false,
    fallbackIcon: ImageVector? = null,
    fallbackColor: Color? = null
) {
    val ctx = LocalContext.current
    val isAudio = remember(record.format) { isAudioFormat(record.format) }
    val fColors = MaterialTheme.fabiColors
    val (defaultIcon, defaultColor) = remember(record.url, record.format) {
        getPlatformIconAndColor(record.url, record.format)
    }
    val iconToUse = fallbackIcon ?: defaultIcon
    val colorToUse = fallbackColor ?: defaultColor

    val localFileModel = remember(record.id, record.isCompleted, record.format) {
        if (record.thumbnailUrl.isNullOrEmpty() && record.isCompleted) {
            val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(
                ctx,
                record.title,
                record.id,
                record.format
            )
            if (file.exists()) file else null
        } else {
            null
        }
    }

    val imageModel = record.thumbnailUrl.takeIf { !it.isNullOrEmpty() } ?: localFileModel

    if (isAudio) {
        VinylRecordThumbnail(
            imageModel = imageModel,
            isFailed = isFailed,
            fallbackIcon = iconToUse,
            fallbackColor = colorToUse,
            size = size,
            modifier = modifier
        )
    } else {
        VideoMediaThumbnail(
            imageModel = imageModel,
            isFailed = isFailed,
            fallbackIcon = iconToUse,
            fallbackColor = colorToUse,
            size = size,
            modifier = modifier
        )
    }
}

/**
 * Renderiza una miniatura de disco de vinilo con surcos, reflejos y la portada en el centro.
 */
@Composable
fun VinylRecordThumbnail(
    imageModel: Any?,
    isFailed: Boolean,
    fallbackIcon: ImageVector,
    fallbackColor: Color,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val fColors = MaterialTheme.fabiColors

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Fondo base y surcos del disco de vinilo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = canvasWidth / 2f

            // 1. Base del disco (negro carbón / vinilo)
            drawCircle(
                color = Color(0xFF141518),
                radius = radius,
                center = center
            )

            // 2. Surcos concéntricos del disco LP
            val grooveRatios = floatArrayOf(0.92f, 0.84f, 0.76f, 0.68f)
            for (r in grooveRatios) {
                drawCircle(
                    color = Color(0xFF282A30).copy(alpha = 0.6f),
                    radius = radius * r,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // 3. Reflejos cónicos / brillo de luz sobre el vinilo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Borde exterior suave del vinilo
            drawCircle(
                color = Color(0xFF383B44),
                radius = radius - 0.5f,
                center = center,
                style = Stroke(width = 1f)
            )
        }

        // Portada central del disco (Label central circular)
        val centerRatio = 0.58f
        val centerSize = size * centerRatio

        Box(
            modifier = Modifier
                .size(centerSize)
                .clip(CircleShape)
                .background(
                    if (isFailed) fColors.error.copy(alpha = 0.25f)
                    else fallbackColor.copy(alpha = 0.2f)
                )
                .border(1.dp, Color(0xFF1E2024), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isFailed) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = fColors.error,
                    modifier = Modifier.size(centerSize * 0.55f)
                )
            } else if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = stringResource(R.string.downloads_thumbnail),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = fallbackColor,
                    modifier = Modifier.size(centerSize * 0.55f)
                )
            }

            // Pequeño orificio central del disco de vinilo
            Box(
                modifier = Modifier
                    .size(size * 0.12f)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1012))
                    .border(0.75.dp, Color(0xFF4A4D57), CircleShape)
            )
        }
    }
}

/**
 * Renderiza una miniatura rectangular estándar con esquinas redondeadas para videos o imágenes.
 */
@Composable
fun VideoMediaThumbnail(
    imageModel: Any?,
    isFailed: Boolean,
    fallbackIcon: ImageVector,
    fallbackColor: Color,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val fColors = MaterialTheme.fabiColors

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isFailed) fColors.error.copy(alpha = 0.15f)
                else fallbackColor.copy(alpha = 0.12f)
            )
            .border(1.dp, fColors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isFailed) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = fColors.error,
                modifier = Modifier.size(size * 0.45f)
            )
        } else if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = stringResource(R.string.downloads_thumbnail),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = fallbackColor,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}
