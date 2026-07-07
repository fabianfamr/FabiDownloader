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
    var showProxyDialog by remember { mutableStateOf(false) }
    var showUserAgentDialog by remember { mutableStateOf(false) }

    val threadOptions = listOf("1", "3", "5", "8", "12", "16")
    val simultaneousOptions = listOf("1", "2", "3", "4", "5")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SettingSectionHeader("Calidad")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            SettingItem(Icons.Default.Hd, "Calidad de video", trailing = selectedQuality) {
                showQualityDialog = true
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader("Formato")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                SettingItem(Icons.Default.VideoFile, "Formato de video", trailing = selectedVideoFormat) {
                    showVideoFormatDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.AudioFile, "Formato de audio", trailing = selectedAudioFormat) {
                    showAudioFormatDialog = true
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader("Almacenamiento y Velocidad")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                SettingItem(Icons.Default.FolderOpen, "Ubicación", trailing = downloadLocation) {
                    onPickLocation()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Speed, "Velocidad máxima", trailing = maxSpeed) {
                    showSpeedDialog = true
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader("Post-Procesamiento y Metadatos")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                ToggleSetting(Icons.Default.Subtitles, "Incrustar subtítulos", AppSettings.embedSubtitles) {
                    AppSettings.embedSubtitles = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.Image, "Incrustar carátula/miniatura", AppSettings.embedThumbnail) {
                    AppSettings.embedThumbnail = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.Info, "Incrustar metadatos y etiquetas", AppSettings.embedMetadata) {
                    AppSettings.embedMetadata = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.AutoMirrored.Filled.PlaylistPlay, "Permitir listas de reproducción", AppSettings.playlistEnabled) {
                    AppSettings.playlistEnabled = it
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader("Integración y Red")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                ToggleSetting(Icons.Default.Block, "SponsorBlock (omitir patrocinios)", AppSettings.sponsorBlockEnabled) {
                    AppSettings.sponsorBlockEnabled = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSetting(Icons.Default.Shield, "Evitar bloqueos regionales (Bypass Geo)", AppSettings.bypassGeo) {
                    AppSettings.bypassGeo = it
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Dns, "Configurar Proxy", trailing = AppSettings.proxyUrl.ifEmpty { "No configurado" }) {
                    showProxyDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Person, "User-Agent Personalizado", trailing = AppSettings.customUserAgent.ifEmpty { "Por defecto" }) {
                    showUserAgentDialog = true
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SettingSectionHeader("Motor de Consola yt-dlp")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column {
                SettingItem(Icons.Default.Download, "Hilos de descarga paralelos", trailing = AppSettings.concurrentFragments) {
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
                            text = "Descargas simultáneas",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Máximo de transferencias activas",
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
                            onClick = { if (AppSettings.maxConcurrentDownloads < 5) AppSettings.maxConcurrentDownloads++ },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text("+", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingItem(Icons.Default.Code, "Argumentos personalizados yt-dlp", trailing = AppSettings.customArguments.ifEmpty { "Ninguno" }) {
                    showCustomArgsDialog = true
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showQualityDialog) {
            SelectionDialog("Selecciona Calidad", qualityOptions, selectedQuality,
                onSelection = {
                    onQualityChange(it)
                    showQualityDialog = false
                },
                onDismiss = { showQualityDialog = false }
            )
        }
        if (showVideoFormatDialog) {
            SelectionDialog("Selecciona Formato Video", videoFormats, selectedVideoFormat,
                onSelection = {
                    onVideoFormatChange(it)
                    showVideoFormatDialog = false
                },
                onDismiss = { showVideoFormatDialog = false }
            )
        }
        if (showAudioFormatDialog) {
            SelectionDialog("Selecciona Formato Audio", audioFormats, selectedAudioFormat,
                onSelection = {
                    onAudioFormatChange(it)
                    showAudioFormatDialog = false
                },
                onDismiss = { showAudioFormatDialog = false }
            )
        }
        if (showSpeedDialog) {
            SelectionDialog("Velocidad máxima", speedOptions, maxSpeed,
                onSelection = {
                    onSpeedChange(it)
                    showSpeedDialog = false
                },
                onDismiss = { showSpeedDialog = false }
            )
        }
        if (showThreadsDialog) {
            SelectionDialog("Hilos paralelos", threadOptions, AppSettings.concurrentFragments,
                onSelection = {
                    AppSettings.concurrentFragments = it
                    showThreadsDialog = false
                },
                onDismiss = { showThreadsDialog = false }
            )
        }
        if (showSimultaneousDialog) {
            SelectionDialog("Descargas simultáneas máximas", simultaneousOptions, AppSettings.maxConcurrentDownloads.toString(),
                onSelection = {
                    AppSettings.maxConcurrentDownloads = it.toIntOrNull() ?: 2
                    showSimultaneousDialog = false
                },
                onDismiss = { showSimultaneousDialog = false }
            )
        }
        if (showCustomArgsDialog) {
            InputDialog(
                title = "Argumentos yt-dlp",
                placeholder = "Ej: --restrict-filenames --no-mtime",
                initialValue = AppSettings.customArguments,
                onConfirm = { AppSettings.customArguments = it },
                onDismiss = { showCustomArgsDialog = false }
            )
        }
        if (showProxyDialog) {
            InputDialog(
                title = "Configurar Proxy",
                placeholder = "Ej: http://127.0.0.1:8080 o socks5://user:pass@host:port",
                initialValue = AppSettings.proxyUrl,
                onConfirm = { AppSettings.proxyUrl = it },
                onDismiss = { showProxyDialog = false }
            )
        }
        if (showUserAgentDialog) {
            InputDialog(
                title = "User-Agent Personalizado",
                placeholder = "Escribe tu cabecera User-Agent",
                initialValue = AppSettings.customUserAgent,
                onConfirm = { AppSettings.customUserAgent = it },
                onDismiss = { showUserAgentDialog = false }
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
    onDismiss: () -> Unit
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }) {
                Text("ACEPTAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                Text("CANCELAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

