package com.fabian.downloader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.fabian.downloader.ui.components.isAudioFormat
import com.fabian.downloader.ui.theme.FabiColorScheme

@Composable
fun MainRecentDownloadsSection(
    recentDownloads: List<DownloadRecord>,
    contentVisible: Boolean,
    colors: FabiColorScheme,
    onNavigateToDownloads: () -> Unit,
    onOpenFile: (DownloadRecord) -> Unit
) {
    AnimatedVisibility(
        visible = contentVisible && recentDownloads.isNotEmpty(),
        enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(
            initialOffsetY = { 20 },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.main_recent_downloads),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.main_view_all),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    modifier = Modifier
                        .clickable { onNavigateToDownloads() }
                        .padding(4.dp)
                )
            }

            recentDownloads.forEach { record ->
                val (platformIcon, platformColor) = getPlatformIconAndColor(record.url, record.format)
                val isAudio = isAudioFormat(record.format)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenFile(record) }
                        .testTag("recent_record_item_${record.id}"),
                    color = colors.card,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = platformColor.copy(alpha = 0.22f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaThumbnail(
                            record = record,
                            size = 48.dp,
                            fallbackIcon = platformIcon,
                            fallbackColor = platformColor
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAudio) AppIcons.MusicNote else AppIcons.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isAudio) colors.accent else platformColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Surface(
                                    color = if (isAudio) colors.accentDim else Color(0x112ECC71),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = record.format,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAudio) colors.accent else colors.success,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = if (record.isCompleted) stringResource(R.string.main_completed) else stringResource(R.string.main_in_progress),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            contentDescription = stringResource(R.string.main_completed),
                            tint = colors.success,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
