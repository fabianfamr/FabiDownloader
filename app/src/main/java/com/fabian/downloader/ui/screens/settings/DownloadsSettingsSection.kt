package com.fabian.downloader.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.KeyValueSelectionDialog
import com.fabian.downloader.ui.components.SelectionDialog
import com.fabian.downloader.ui.components.SpeedSliderDialog
import com.fabian.downloader.ui.screens.SettingsHeader
import com.fabian.downloader.ui.screens.SettingsRow
import com.fabian.downloader.ui.screens.SettingsToggleRow
import com.fabian.downloader.ui.theme.FabiColors
import com.fabian.downloader.utils.PathUtils
import com.fabian.downloader.utils.SettingsLabels

@Composable
fun DownloadsSettingsSection(
    fColors: FabiColors,
    launcher: ManagedActivityResultLauncher<Uri?, Uri?>
) {
    val ctx = LocalContext.current
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_gray2 = fColors.textMuted
    val C_bg = fColors.background

    var wifiOnly by remember { mutableStateOf(AppSettings.dataSaverEnabled) }
    var autoDownload by remember { mutableStateOf(AppSettings.clipboardAction == "auto") }
    var playlistEnabledState by remember { mutableStateOf(AppSettings.playlistEnabled) }

    var selectedQualityState by remember { mutableStateOf(AppSettings.selectedQuality) }
    var selectedVideoFormatState by remember { mutableStateOf(AppSettings.selectedVideoFormat) }
    var selectedAudioFormatState by remember { mutableStateOf(AppSettings.selectedAudioFormat) }
    var defaultAudioBitrateState by remember { mutableStateOf(AppSettings.defaultAudioBitrate) }

    var maxConcurrent by remember { mutableStateOf(AppSettings.maxConcurrentDownloads) }
    var concurrentFragmentsState by remember { mutableStateOf(AppSettings.concurrentFragments) }
    var maxSpeedState by remember { mutableStateOf(AppSettings.maxSpeed) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showVideoFormatDialog by remember { mutableStateOf(false) }
    var showAudioFormatDialog by remember { mutableStateOf(false) }
    var showAudioBitrateDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showThreadsDialog by remember { mutableStateOf(false) }
    var showEarlyStartThresholdDialog by remember { mutableStateOf(false) }

    if (showQualityDialog) {
        KeyValueSelectionDialog(
            title = stringResource(R.string.settings_quality_default),
            items = SettingsLabels.getQualityOptions(ctx, AppSettings.qualityOptions),
            selectedKey = selectedQualityState,
            onSelection = {
                AppSettings.selectedQuality = it
                selectedQualityState = it
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showVideoFormatDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_format_video),
            options = AppSettings.videoFormats,
            selectedOption = selectedVideoFormatState,
            onSelection = {
                AppSettings.selectedVideoFormat = it
                selectedVideoFormatState = it
                showVideoFormatDialog = false
            },
            onDismiss = { showVideoFormatDialog = false }
        )
    }

    if (showAudioFormatDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_select_audio_format),
            options = AppSettings.audioFormats,
            selectedOption = selectedAudioFormatState,
            onSelection = {
                AppSettings.selectedAudioFormat = it
                selectedAudioFormatState = it
                showAudioFormatDialog = false
            },
            onDismiss = { showAudioFormatDialog = false }
        )
    }

    if (showAudioBitrateDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_audio_quality_dialog),
            options = AppSettings.defaultAudioBitrateOptions,
            selectedOption = defaultAudioBitrateState,
            onSelection = {
                AppSettings.defaultAudioBitrate = it
                defaultAudioBitrateState = it
                showAudioBitrateDialog = false
            },
            onDismiss = { showAudioBitrateDialog = false }
        )
    }

    if (showSpeedDialog) {
        SpeedSliderDialog(
            initialSpeed = maxSpeedState,
            speedOptions = AppSettings.speedOptions,
            onSpeedSelected = {
                AppSettings.maxSpeed = it
                maxSpeedState = it
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showThreadsDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_threads),
            options = listOf("1", "3", "5", "8", "12", "16"),
            selectedOption = concurrentFragmentsState,
            onSelection = {
                AppSettings.concurrentFragments = it
                concurrentFragmentsState = it
                showThreadsDialog = false
            },
            onDismiss = { showThreadsDialog = false }
        )
    }

    if (showEarlyStartThresholdDialog) {
        val options = listOf(stringResource(R.string.settings_disabled), "90%", "91%", "92%", "93%", "94%", "95%", "96%", "97%")
        val currentLabel = when(val currentVal = AppSettings.earlyStartThreshold) {
            0 -> stringResource(R.string.settings_disabled)
            else -> "$currentVal%"
        }
        SelectionDialog(
            title = stringResource(R.string.settings_early_start),
            options = options,
            selectedOption = currentLabel,
            onSelection = { label ->
                val newVal = if (label.contains("%")) {
                    label.replace("%", "").toIntOrNull() ?: 0
                } else {
                    0
                }
                AppSettings.earlyStartThreshold = newVal
                showEarlyStartThresholdDialog = false
            },
            onDismiss = { showEarlyStartThresholdDialog = false }
        )
    }

    // 1. Ubicación y Almacenamiento
    SettingsHeader(stringResource(R.string.settings_header_storage), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsRow(
                icon = AppIcons.Folder,
                title = stringResource(R.string.settings_download_dir),
                trailing = PathUtils.getDisplayDownloadLocation(AppSettings.downloadLocation),
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                launcher.launch(null)
            }
        }
    }

    // 2. Conexión y Red
    SettingsHeader(stringResource(R.string.settings_header_network), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.Wifi,
                title = stringResource(R.string.settings_wifi_only),
                subtitle = stringResource(R.string.settings_wifi_only_desc),
                checked = wifiOnly,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                wifiOnly = it
                AppSettings.dataSaverEnabled = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(
                icon = AppIcons.Speed,
                title = stringResource(R.string.settings_speed_limit),
                trailing = SettingsLabels.getSpeedLabel(ctx, maxSpeedState),
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                showSpeedDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.Link,
                title = stringResource(R.string.settings_auto_download),
                subtitle = stringResource(R.string.settings_auto_download_desc),
                checked = autoDownload,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                autoDownload = it
                AppSettings.clipboardAction = if (it) "auto" else "disabled"
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.List,
                title = stringResource(R.string.settings_allow_playlists),
                subtitle = stringResource(R.string.settings_allow_playlists_desc),
                checked = playlistEnabledState,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                playlistEnabledState = it
                AppSettings.playlistEnabled = it
            }
        }
    }

    // 3. Formatos y Calidad
    SettingsHeader(stringResource(R.string.settings_header_quality), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsRow(
                icon = AppIcons.Hd,
                title = stringResource(R.string.settings_quality_default),
                trailing = SettingsLabels.getQualityLabel(ctx, selectedQualityState),
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                showQualityDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.VideoFile, stringResource(R.string.settings_format_video), selectedVideoFormatState, C_accent, C_white, C_gray1, C_card2) {
                showVideoFormatDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.AudioFile, stringResource(R.string.settings_audio_format), selectedAudioFormatState, C_accent, C_white, C_gray1, C_card2) {
                showAudioFormatDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.GraphicEq, stringResource(R.string.settings_audio_quality), defaultAudioBitrateState, C_accent, C_white, C_gray1, C_card2) {
                showAudioBitrateDialog = true
            }
        }
    }

    // 4. Rendimiento y Fragmentos
    SettingsHeader(stringResource(R.string.settings_header_performance), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            Column(modifier = Modifier.padding(14.dp, 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_simultaneous),
                            color = C_white,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.settings_simultaneous_desc),
                            color = C_gray1,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C_card2)
                                .border(1.dp, C_border, RoundedCornerShape(10.dp))
                                .clickable {
                                    maxConcurrent = maxOf(1, maxConcurrent - 1)
                                    AppSettings.maxConcurrentDownloads = maxConcurrent
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.Remove,
                                contentDescription = "-",
                                tint = C_white,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "$maxConcurrent",
                            color = C_accent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C_card2)
                                .border(1.dp, C_border, RoundedCornerShape(10.dp))
                                .clickable {
                                    maxConcurrent = minOf(12, maxConcurrent + 1)
                                    AppSettings.maxConcurrentDownloads = maxConcurrent
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = "+",
                                tint = C_white,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    (1..12).forEach { n ->
                        val isActive = n <= maxConcurrent
                        Box(modifier = Modifier.weight(1f).height(4.dp).background(if (isActive) C_accent else C_card2, RoundedCornerShape(4.dp)))
                    }
                }
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.Download, stringResource(R.string.settings_threads), concurrentFragmentsState, C_accent, C_white, C_gray1, C_card2) {
                showThreadsDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(
                icon = AppIcons.FastForward,
                title = stringResource(R.string.settings_early_start),
                trailing = if (AppSettings.earlyStartThreshold == 0) stringResource(R.string.settings_disabled) else "${AppSettings.earlyStartThreshold}%",
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                showEarlyStartThresholdDialog = true
            }
        }
    }
}
