package com.fabian.downloader.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.MediaThumbnail
import com.fabian.downloader.ui.components.getPlatformIconAndColor
import com.fabian.downloader.ui.theme.fabiColors
import com.fabian.downloader.utils.YtdlpParser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobileDownloadingItem(
    record: DownloadRecord, 
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onShowErrorDetails: (String) -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val fColors = MaterialTheme.fabiColors
    val C_card = fColors.card
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_accentDim = fColors.accentDim
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_red = fColors.error
    val C_amber = fColors.amber

    val failedPrefix = stringResource(R.string.downloads_failed_prefix)
    val isFailed = record.speed == "FAILED" || 
            record.title.startsWith("Fallo: ") || 
            record.title.startsWith("Failed: ") || 
            record.title.startsWith(failedPrefix)
    val cleanTitle = remember(record.title) {
        var t = record.title
        while (t.startsWith("Fallo: ") || t.startsWith("Failed: ") || t.startsWith(failedPrefix)) {
            t = if (t.startsWith("Fallo: ")) {
                t.substringAfter("Fallo: ")
            } else if (t.startsWith("Failed: ")) {
                t.substringAfter("Failed: ")
            } else {
                t.substringAfter(failedPrefix)
            }
        }
        t
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            isFailed -> C_red
            record.isPaused -> C_amber
            else -> C_accent
        },
        label = "statusColor"
    )

    val targetProgress = remember(record.progress) {
        if (record.progress < 0) 0f else (record.progress / 100f).coerceIn(0f, 1f)
    }

    Surface(
        color = if (isSelected) C_accentDim else C_card,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp, 
            if (isSelected) C_accent else (if (isFailed) statusColor.copy(alpha = 0.35f) else C_border)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { 
                    if (isSelectionMode) {
                        onLongPress()
                    } else {
                        if (isFailed) onResume() 
                        else if (record.isPaused) onResume() 
                        else onPause() 
                    }
                },
                onLongClick = { onLongPress() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                    end = if (isSelectionMode) 14.dp else 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (platformIcon, platformColor) = remember(record.url, record.format) {
                getPlatformIconAndColor(record.url, record.format)
            }

            MediaThumbnail(
                record = record,
                isFailed = isFailed,
                size = 56.dp,
                fallbackIcon = if (isFailed) AppIcons.Error else platformIcon,
                fallbackColor = if (isFailed) C_red else platformColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cleanTitle, 
                    style = MaterialTheme.typography.titleSmall,
                    color = C_white, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 13.sp,
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { targetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape),
                    color = statusColor,
                    trackColor = C_white.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speedText = when {
                        isFailed -> stringResource(R.string.downloads_error_retry)
                        record.isPaused -> stringResource(R.string.downloads_status_paused)
                        record.progress < 0 -> stringResource(R.string.downloads_status_waiting)
                        else -> YtdlpParser.getLocalizedStatus(ctx, record.speed)
                    }

                    Text(
                        text = speedText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (record.isPaused) C_amber else if (isFailed) C_red else C_accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val percentText = if (record.isPaused) {
                        "${if (record.progress < 0) 0 else record.progress}%"
                    } else if (record.progress < 0) {
                        "0%"
                    } else {
                        "${record.progress}%"
                    }

                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = C_gray1,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) C_accent else Color.Transparent)
                        .border(1.5.dp, if (isSelected) C_accent else C_border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = AppIcons.Check,
                            contentDescription = null,
                            tint = Color(0xFF0A0A0C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.MoreVert,
                        contentDescription = stringResource(R.string.downloads_action_options),
                        modifier = Modifier.size(18.dp),
                        tint = C_white
                    )
                }
            }
        }
    }
}
