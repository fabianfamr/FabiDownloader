package com.fabian.downloader.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.MediaThumbnail
import com.fabian.downloader.ui.components.getPlatformIconAndColor
import com.fabian.downloader.ui.components.isAudioFormat
import com.fabian.downloader.ui.theme.fabiColors
import com.fabian.downloader.utils.YtdlpParser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobileDownloadedItem(
    record: DownloadRecord, 
    onPlay: () -> Unit, 
    onDelete: () -> Unit, 
    onShare: () -> Unit, 
    isSelected: Boolean, 
    isSelectionMode: Boolean = false, 
    onLongPress: () -> Unit
) {
    val ctx = LocalContext.current
    val fColors = MaterialTheme.fabiColors
    val C_card = fColors.card
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_accentDim = fColors.accentDim
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary

    val cleanTitle = remember(record.title) {
        var t = record.title
        while (t.startsWith("Fallo: ") || t.startsWith("Failed: ") || t.startsWith(Config.STATUS_FAILED_PREFIX)) {
            t = if (t.startsWith("Fallo: ")) {
                t.substringAfter("Fallo: ")
            } else if (t.startsWith("Failed: ")) {
                t.substringAfter("Failed: ")
            } else {
                t.substringAfter(Config.STATUS_FAILED_PREFIX)
            }
        }
        t
    }
    val (platformIcon, platformColor) = remember(record.url, record.format) {
        getPlatformIconAndColor(record.url, record.format)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) C_accentDim else C_card,
        border = if (isSelected) BorderStroke(2.dp, C_accent) else BorderStroke(1.dp, C_border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { onPlay() },
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
            val isAudio = remember(record.format) { isAudioFormat(record.format) }

            MediaThumbnail(
                record = record,
                size = 52.dp,
                fallbackIcon = platformIcon,
                fallbackColor = platformColor
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanTitle, 
                    style = MaterialTheme.typography.titleSmall,
                    color = C_white, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isAudio) AppIcons.MusicNote else AppIcons.PlayArrow,
                        contentDescription = null,
                        tint = if (isAudio) C_accent else platformColor,
                        modifier = Modifier.size(13.dp)
                    )

                    if (AppSettings.showQualityBadge) {
                        Surface(
                            color = platformColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${record.quality} • ${record.format}", 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = platformColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Box(modifier = Modifier.size(3.dp).background(C_gray1, CircleShape))
                    }
                    Text(
                        text = YtdlpParser.getLocalizedSize(ctx, record.size), 
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = C_gray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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
                        tint = C_white,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
