package com.fabian.downloader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.LocaleHelper
import com.fabian.downloader.utils.PathUtils
import com.fabian.downloader.utils.SettingsLabels

@Composable
fun DownloadSettingsContent(
    qualityOptions: List<String>,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    videoFormats: List<String>,
    selectedVideoFormat: String,
    onVideoFormatChange: (String) -> Unit,
    audioFormats: List<String>,
    selectedAudioFormat: String,
    onAudioFormatChange: (String) -> Unit,
    downloadLocation: String,
    onPickLocation: () -> Unit,
    onLocationChange: (String) -> Unit,
    maxSpeed: String,
    onSpeedChange: (String) -> Unit,
    speedOptions: List<String>
) {
    val context = LocalContext.current
    var showQualityDialog by remember { mutableStateOf(false) }
    var showVideoFormatDialog by remember { mutableStateOf(false) }
    var showAudioFormatDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showThreadsDialog by remember { mutableStateOf(false) }
    var showSimultaneousDialog by remember { mutableStateOf(false) }
    var showCustomArgsDialog by remember { mutableStateOf(false) }
    var showUserAgentDialog by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStorageMarginDialog by remember { mutableStateOf(false) }
    var showPausedTimeoutDialog by remember { mutableStateOf(false) }
    var showBatteryLowThresholdDialog by remember { mutableStateOf(false) }
    var showBatteryLowActionDialog by remember { mutableStateOf(false) }

    val threadOptions = listOf("1", "3", "5", "8", "10", "12", "16", "20")
    val simultaneousOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SettingSectionHeader(stringResource(R.string.settings_section_customization))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                SettingItem(AppIcons.Palette, stringResource(R.string.settings_app_theme), trailing = AppSettings.themePreference) {
                    showThemeDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.Language, stringResource(R.string.settings_language), trailing = LocaleHelper.getDisplayName(AppSettings.language)) {
                    showLanguageDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(AppIcons.Notifications, stringResource(R.string.settings_download_notifications), AppSettings.notificationsEnabled) {
                    AppSettings.notificationsEnabled = it
                }
                if (AppSettings.notificationsEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(AppIcons.Timer, stringResource(R.string.settings_auto_cancel_pause), trailing = AppSettings.selectedPausedNotificationTimeout) {
                        showPausedTimeoutDialog = true
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(AppIcons.DeleteSweep, stringResource(R.string.settings_confirm_on_delete), AppSettings.confirmOnDelete) {
                    AppSettings.confirmOnDelete = it
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader(stringResource(R.string.settings_section_quality_format))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                SettingItem(AppIcons.Hd, stringResource(R.string.settings_quality_default), trailing = SettingsLabels.getQualityLabel(context, selectedQuality)) {
                    showQualityDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.VideoFile, stringResource(R.string.settings_video_format), trailing = selectedVideoFormat) {
                    showVideoFormatDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.AudioFile, stringResource(R.string.settings_audio_format), trailing = selectedAudioFormat) {
                    showAudioFormatDialog = true
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader(stringResource(R.string.settings_section_storage_speed))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                SettingItem(AppIcons.FolderOpen, stringResource(R.string.settings_location), trailing = PathUtils.getDisplayDownloadLocation(downloadLocation)) {
                    onPickLocation()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.Speed, stringResource(R.string.settings_max_speed), trailing = SettingsLabels.getSpeedLabel(context, maxSpeed)) {
                    showSpeedDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.Storage, stringResource(R.string.settings_storage_margin), trailing = SettingsLabels.getStorageMarginLabel(context, AppSettings.selectedStorageMargin)) {
                    showStorageMarginDialog = true
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader(stringResource(R.string.settings_section_postprocessing))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                ToggleSetting(AppIcons.Subtitles, stringResource(R.string.settings_embed_subtitles), AppSettings.embedSubtitles) {
                    AppSettings.embedSubtitles = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(AppIcons.Image, stringResource(R.string.settings_embed_thumbnail), AppSettings.embedThumbnail) {
                    AppSettings.embedThumbnail = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(AppIcons.Info, stringResource(R.string.settings_embed_metadata), AppSettings.embedMetadata) {
                    AppSettings.embedMetadata = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(AppIcons.PlaylistPlay, stringResource(R.string.settings_allow_playlists), AppSettings.playlistEnabled) {
                    AppSettings.playlistEnabled = it
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader(stringResource(R.string.settings_section_integration_network))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                ToggleSetting(AppIcons.Block, stringResource(R.string.settings_sponsorblock_geo), AppSettings.sponsorBlockEnabled) {
                    AppSettings.sponsorBlockEnabled = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(AppIcons.Shield, stringResource(R.string.settings_bypass_geo), AppSettings.bypassGeo) {
                    AppSettings.bypassGeo = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.Person, stringResource(R.string.settings_custom_user_agent), trailing = AppSettings.customUserAgent.ifEmpty { stringResource(R.string.settings_value_default) }) {
                    showUserAgentDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.ContentPaste, stringResource(R.string.settings_clipboard_action_title), trailing = SettingsLabels.getClipboardActionLabel(context, AppSettings.clipboardAction)) {
                    showClipboardDialog = true
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader(stringResource(R.string.settings_section_ytdlp_engine))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                SettingItem(AppIcons.Download, stringResource(R.string.settings_parallel_threads), trailing = AppSettings.concurrentFragments) {
                    showThreadsDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.FilterNone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_simultaneous_downloads),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_max_transfers),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (AppSettings.maxConcurrentDownloads > 1) AppSettings.maxConcurrentDownloads-- },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = AppIcons.Remove,
                                contentDescription = "-",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Text(
                            text = AppSettings.maxConcurrentDownloads.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        IconButton(
                            onClick = { if (AppSettings.maxConcurrentDownloads < 12) AppSettings.maxConcurrentDownloads++ },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = "+",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(AppIcons.Code, stringResource(R.string.settings_custom_args), trailing = AppSettings.customArguments.ifEmpty { stringResource(R.string.settings_value_none) }) {
                    showCustomArgsDialog = true
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader(stringResource(R.string.settings_battery_optimization_title))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                ToggleSetting(AppIcons.BatteryChargingFull, stringResource(R.string.settings_battery_optimization), AppSettings.batteryOptimizationEnabled) {
                    AppSettings.batteryOptimizationEnabled = it
                }
                if (AppSettings.batteryOptimizationEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(AppIcons.BatteryAlert, stringResource(R.string.settings_battery_threshold), trailing = AppSettings.selectedBatteryLowThreshold) {
                        showBatteryLowThresholdDialog = true
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(AppIcons.SettingsApplications, stringResource(R.string.settings_battery_action), trailing = SettingsLabels.getBatteryActionLabel(context, AppSettings.selectedBatteryLowAction)) {
                        showBatteryLowActionDialog = true
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showQualityDialog) {
            KeyValueSelectionDialog(
                title = stringResource(R.string.settings_select_quality),
                items = SettingsLabels.getQualityOptions(context, qualityOptions),
                selectedKey = selectedQuality,
                onSelection = {
                    onQualityChange(it)
                    showQualityDialog = false
                },
                onDismiss = { showQualityDialog = false }
            )
        }
        if (showVideoFormatDialog) {
            SelectionDialog(stringResource(R.string.settings_select_video_format), videoFormats, selectedVideoFormat,
                onSelection = {
                    onVideoFormatChange(it)
                    showVideoFormatDialog = false
                },
                onDismiss = { showVideoFormatDialog = false }
            )
        }
        if (showAudioFormatDialog) {
            SelectionDialog(stringResource(R.string.settings_select_audio_format), audioFormats, selectedAudioFormat,
                onSelection = {
                    onAudioFormatChange(it)
                    showAudioFormatDialog = false
                },
                onDismiss = { showAudioFormatDialog = false }
            )
        }
        if (showSpeedDialog) {
            SpeedSliderDialog(
                initialSpeed = maxSpeed,
                speedOptions = speedOptions,
                onSpeedSelected = { onSpeedChange(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }
        if (showThreadsDialog) {
            SelectionDialog(stringResource(R.string.settings_parallel_threads_short), threadOptions, AppSettings.concurrentFragments,
                onSelection = {
                    AppSettings.concurrentFragments = it
                    showThreadsDialog = false
                },
                onDismiss = { showThreadsDialog = false }
            )
        }
        if (showSimultaneousDialog) {
            SelectionDialog(stringResource(R.string.settings_max_simultaneous_short), simultaneousOptions, AppSettings.maxConcurrentDownloads.toString(),
                onSelection = {
                    AppSettings.maxConcurrentDownloads = it.toIntOrNull() ?: 2
                    showSimultaneousDialog = false
                },
                onDismiss = { showSimultaneousDialog = false }
            )
        }
        if (showCustomArgsDialog) {
            InputDialog(
                title = stringResource(R.string.settings_args_dialog_title),
                placeholder = stringResource(R.string.settings_args_placeholder_example),
                initialValue = AppSettings.customArguments,
                onConfirm = { AppSettings.customArguments = it },
                onDismiss = { showCustomArgsDialog = false }
            )
        }
        if (showUserAgentDialog) {
            InputDialog(
                title = stringResource(R.string.settings_custom_user_agent),
                placeholder = stringResource(R.string.settings_user_agent_placeholder),
                initialValue = AppSettings.customUserAgent,
                onConfirm = { AppSettings.customUserAgent = it },
                onDismiss = { showUserAgentDialog = false }
            )
        }
        if (showClipboardDialog) {
            KeyValueSelectionDialog(
                title = stringResource(R.string.settings_clipboard_action_title),
                items = SettingsLabels.getClipboardActionOptions(context),
                selectedKey = AppSettings.clipboardAction,
                onSelection = {
                    AppSettings.clipboardAction = it
                    showClipboardDialog = false
                },
                onDismiss = { showClipboardDialog = false }
            )
        }
        if (showThemeDialog) {
            SelectionDialog(stringResource(R.string.settings_select_theme), AppSettings.themeOptions, AppSettings.themePreference,
                onSelection = {
                    AppSettings.themePreference = it
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }
        if (showPausedTimeoutDialog) {
            SelectionDialog(stringResource(R.string.settings_auto_cancel_pause), AppSettings.pausedNotificationTimeoutOptions, AppSettings.selectedPausedNotificationTimeout,
                onSelection = {
                    AppSettings.selectedPausedNotificationTimeout = it
                    showPausedTimeoutDialog = false
                },
                onDismiss = { showPausedTimeoutDialog = false }
            )
        }
        if (showBatteryLowThresholdDialog) {
            SelectionDialog(stringResource(R.string.settings_battery_threshold), AppSettings.batteryLowThresholdOptions, AppSettings.selectedBatteryLowThreshold,
                onSelection = {
                    AppSettings.selectedBatteryLowThreshold = it
                    showBatteryLowThresholdDialog = false
                },
                onDismiss = { showBatteryLowThresholdDialog = false }
            )
        }
        if (showBatteryLowActionDialog) {
            KeyValueSelectionDialog(
                title = stringResource(R.string.settings_battery_action),
                items = SettingsLabels.getBatteryActionOptions(context, AppSettings.batteryLowActionOptions),
                selectedKey = AppSettings.selectedBatteryLowAction,
                onSelection = {
                    AppSettings.selectedBatteryLowAction = it
                    showBatteryLowActionDialog = false
                },
                onDismiss = { showBatteryLowActionDialog = false }
            )
        }
        if (showStorageMarginDialog) {
            KeyValueSelectionDialog(
                title = stringResource(R.string.settings_select_storage_margin),
                items = SettingsLabels.getStorageMarginOptions(context, AppSettings.storageMarginOptions),
                selectedKey = AppSettings.selectedStorageMargin,
                onSelection = {
                    AppSettings.selectedStorageMargin = it
                    showStorageMarginDialog = false
                },
                onDismiss = { showStorageMarginDialog = false }
            )
        }
        if (showLanguageDialog) {
            val languageOptions = LocaleHelper.SUPPORTED_LANGUAGES
            val selectedOption = LocaleHelper.getDisplayName(AppSettings.language)
            SelectionDialog(
                title = stringResource(R.string.settings_select_language),
                options = languageOptions,
                selectedOption = selectedOption,
                onSelection = {
                    AppSettings.language = it
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}
