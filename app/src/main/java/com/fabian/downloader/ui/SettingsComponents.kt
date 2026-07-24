package com.fabian.downloader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.fabian.downloader.R

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

    val threadOptions = listOf("1", "3", "5", "8", "10", "12", "16", "20")
    val simultaneousOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8")
    val clipboardOptions = listOf("banner", "auto", "disabled")

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
                SettingItem(Icons.Default.Palette, stringResource(R.string.settings_app_theme), trailing = AppSettings.themePreference) {
                    showThemeDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Language, stringResource(R.string.settings_language), trailing = AppSettings.language) {
                    showLanguageDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.Notifications, stringResource(R.string.settings_download_notifications), AppSettings.notificationsEnabled) {
                    AppSettings.notificationsEnabled = it
                }
                if (AppSettings.notificationsEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(Icons.Default.Timer, "Auto-cancelar pausa", trailing = AppSettings.selectedPausedNotificationTimeout) {
                        showPausedTimeoutDialog = true
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.DeleteSweep, stringResource(R.string.settings_confirm_on_delete), AppSettings.confirmOnDelete) {
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
                SettingItem(Icons.Default.VideoFile, stringResource(R.string.settings_video_format), trailing = selectedVideoFormat) {
                    showVideoFormatDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.AudioFile, stringResource(R.string.settings_audio_format), trailing = selectedAudioFormat) {
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
                SettingItem(Icons.Default.FolderOpen, stringResource(R.string.settings_location), trailing = downloadLocation) {
                    onPickLocation()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Speed, stringResource(R.string.settings_max_speed), trailing = maxSpeed) {
                    showSpeedDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Storage, stringResource(R.string.settings_storage_margin), trailing = AppSettings.selectedStorageMargin) {
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
                ToggleSetting(Icons.Default.Subtitles, stringResource(R.string.settings_embed_subtitles), AppSettings.embedSubtitles) {
                    AppSettings.embedSubtitles = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.Image, stringResource(R.string.settings_embed_thumbnail), AppSettings.embedThumbnail) {
                    AppSettings.embedThumbnail = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.Info, stringResource(R.string.settings_embed_metadata), AppSettings.embedMetadata) {
                    AppSettings.embedMetadata = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.settings_allow_playlists), AppSettings.playlistEnabled) {
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
                ToggleSetting(Icons.Default.Block, stringResource(R.string.settings_sponsorblock_geo), AppSettings.sponsorBlockEnabled) {
                    AppSettings.sponsorBlockEnabled = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.Shield, stringResource(R.string.settings_bypass_geo), AppSettings.bypassGeo) {
                    AppSettings.bypassGeo = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Person, stringResource(R.string.settings_custom_user_agent), trailing = AppSettings.customUserAgent.ifEmpty { stringResource(R.string.settings_value_default) }) {
                    showUserAgentDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.ContentPaste, stringResource(R.string.settings_clipboard_action_title), trailing = AppSettings.clipboardAction) {
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
                SettingItem(Icons.Default.Download, stringResource(R.string.settings_parallel_threads), trailing = AppSettings.concurrentFragments) {
                    showThreadsDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                
                // Stepper para descargas simultáneas
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
                        Icon(Icons.Default.FilterNone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (AppSettings.maxConcurrentDownloads > 1) AppSettings.maxConcurrentDownloads-- },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text("−", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = AppSettings.maxConcurrentDownloads.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.widthIn(min = 20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        IconButton(
                            onClick = { if (AppSettings.maxConcurrentDownloads < 8) AppSettings.maxConcurrentDownloads++ },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text("+", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Code, stringResource(R.string.settings_custom_args), trailing = AppSettings.customArguments.ifEmpty { stringResource(R.string.settings_value_none) }) {
                    showCustomArgsDialog = true
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showQualityDialog) {
            SelectionDialog(stringResource(R.string.settings_select_quality), qualityOptions, selectedQuality,
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
            SelectionDialog(stringResource(R.string.settings_max_speed), speedOptions, maxSpeed,
                onSelection = {
                    onSpeedChange(it)
                    showSpeedDialog = false
                },
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
            SelectionDialog(stringResource(R.string.settings_clipboard_action_title), clipboardOptions, AppSettings.clipboardAction,
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
            SelectionDialog("Auto-cancelar pausa", AppSettings.pausedNotificationTimeoutOptions, AppSettings.selectedPausedNotificationTimeout,
                onSelection = {
                    AppSettings.selectedPausedNotificationTimeout = it
                    showPausedTimeoutDialog = false
                },
                onDismiss = { showPausedTimeoutDialog = false }
            )
        }
        if (showStorageMarginDialog) {
            SelectionDialog(stringResource(R.string.settings_select_storage_margin), AppSettings.storageMarginOptions, AppSettings.selectedStorageMargin,
                onSelection = {
                    AppSettings.selectedStorageMargin = it
                    showStorageMarginDialog = false
                },
                onDismiss = { showStorageMarginDialog = false }
            )
        }
        val ctx = androidx.compose.ui.platform.LocalContext.current
        if (showLanguageDialog) {
            val languageOptions = listOf("Sistema", "Español", "English")
            SelectionDialog(stringResource(R.string.settings_select_language), languageOptions, AppSettings.language,
                onSelection = {
                    AppSettings.language = it
                    showLanguageDialog = false
                    // Apply locale
                    if (ctx is android.app.Activity) {
                        ctx.recreate()
                    }
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

@Composable
fun InputDialog(
    title: String,
    placeholder: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    singleLine: Boolean = true
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).let { 
                    if (!singleLine) it.heightIn(min = 150.dp, max = 300.dp) else it 
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }) {
                Text(stringResource(R.string.settings_btn_accept), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_btn_cancel), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun SelectionDialog(title: String, options: List<String>, selectedOption: String, onSelection: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelection(option) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelection(option) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = option, 
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, 
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_btn_cancel), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun ToggleSetting(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title, 
            color = MaterialTheme.colorScheme.onSurface, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingSectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 6.dp, top = 24.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SettingItem(icon: ImageVector, title: String, trailing: String? = null, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                Text(
                    text = trailing, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

