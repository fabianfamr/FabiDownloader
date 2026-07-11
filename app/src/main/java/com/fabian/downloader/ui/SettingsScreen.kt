package com.fabian.downloader.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.BuildConfig
import com.fabian.downloader.R
import com.fabian.downloader.ui.theme.*
import com.fabian.downloader.utils.UpdateManager
import com.fabian.downloader.utils.UpdateInfo
import android.content.Intent
import androidx.compose.material.icons.automirrored.filled.Label
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Necesitamos el navController para navegar a la otra pantalla
    // Pero SettingsScreen no lo recibe. Podríamos añadirlo o usar una callback.
    // Como no quiero cambiar demasiadas firmas, añadiré un botón que emule navegación si es posible,
    // o simplemente añadiré los ajustes aquí también.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ctx.contentResolver.takePersistableUriPermission(uri, takeFlags)
            AppSettings.downloadLocation = uri.toString()
        }
    }

    val fColors = MaterialTheme.fabiColors
    val C_bg = fColors.background
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_accentDim = fColors.accentDim
    val C_accentGlow = fColors.accentGlow
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_gray2 = fColors.textMuted
    val C_green = fColors.success
    val C_amber = fColors.amber
    val C_red = fColors.error

    var cacheState by remember { mutableStateOf(0) } // 0: Idle, 1: Clearing, 2: Done

    // State bindings to AppSettings
    var maxConcurrent by remember { mutableStateOf(AppSettings.maxConcurrentDownloads) }
    var autoDownload by remember { mutableStateOf(AppSettings.clipboardAction == "auto") }
    var wifiOnly by remember { mutableStateOf(AppSettings.dataSaverEnabled) }
    var embedSubtitles by remember { mutableStateOf(AppSettings.embedSubtitles) }
    var embedThumbnail by remember { mutableStateOf(AppSettings.embedThumbnail) }
    var embedMetadata by remember { mutableStateOf(AppSettings.embedMetadata) }
    var confirmOnDelete by remember { mutableStateOf(AppSettings.confirmOnDelete) }
    var sponsorBlock by remember { mutableStateOf(AppSettings.sponsorBlockEnabled) }
    var bypassGeo by remember { mutableStateOf(AppSettings.bypassGeo) }
    
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    var showProxyDialog by remember { mutableStateOf(false) }
    var showCustomArgsDialog by remember { mutableStateOf(false) }

    var showClipboardDialog by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(AppSettings.notificationsEnabled) }
    var showSpeedInNotif by remember { mutableStateOf(AppSettings.showDownloadSpeedInNotification) }
    var vibrateOnComplete by remember { mutableStateOf(AppSettings.vibrateOnComplete) }
    var soundOnComplete by remember { mutableStateOf(AppSettings.soundOnComplete) }

    var useExternalPlayer by remember { mutableStateOf(AppSettings.useExternalPlayer) }
    var keepHistory by remember { mutableStateOf(AppSettings.keepHistory) }
    var autoRetry by remember { mutableStateOf(AppSettings.autoRetry) }

    var dynamicColor by remember { mutableStateOf(AppSettings.dynamicColor) }
    var showAccentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(maxConcurrent) { AppSettings.maxConcurrentDownloads = maxConcurrent }
    LaunchedEffect(autoDownload) { AppSettings.clipboardAction = if (autoDownload) "auto" else "disabled" }
    LaunchedEffect(wifiOnly) { AppSettings.dataSaverEnabled = wifiOnly }
    LaunchedEffect(notificationsEnabled) { AppSettings.notificationsEnabled = notificationsEnabled }
    LaunchedEffect(showSpeedInNotif) { AppSettings.showDownloadSpeedInNotification = showSpeedInNotif }
    LaunchedEffect(vibrateOnComplete) { AppSettings.vibrateOnComplete = vibrateOnComplete }
    LaunchedEffect(soundOnComplete) { AppSettings.soundOnComplete = soundOnComplete }
    LaunchedEffect(useExternalPlayer) { AppSettings.useExternalPlayer = useExternalPlayer }
    LaunchedEffect(keepHistory) { AppSettings.keepHistory = keepHistory }
    LaunchedEffect(autoRetry) { AppSettings.autoRetry = autoRetry }
    LaunchedEffect(dynamicColor) { AppSettings.dynamicColor = dynamicColor }
    LaunchedEffect(embedSubtitles) { AppSettings.embedSubtitles = embedSubtitles }
    LaunchedEffect(embedThumbnail) { AppSettings.embedThumbnail = embedThumbnail }
    LaunchedEffect(embedMetadata) { AppSettings.embedMetadata = embedMetadata }
    LaunchedEffect(confirmOnDelete) { AppSettings.confirmOnDelete = confirmOnDelete }
    LaunchedEffect(sponsorBlock) { AppSettings.sponsorBlockEnabled = sponsorBlock }
    LaunchedEffect(bypassGeo) { AppSettings.bypassGeo = bypassGeo }

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        contentVisible = true
    }

    var showVideoFormatDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    var showThreadsDialog by remember { mutableStateOf(false) }
    
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateFound by remember { mutableStateOf<UpdateInfo?>(null) }

    if (updateFound != null) {
        AlertDialog(
            onDismissRequest = { updateFound = null },
            containerColor = Color(0xFF161619),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = C_accent)
                    Text(stringResource(R.string.settings_update_available_title), color = C_white)
                }
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.settings_update_available_msg, updateFound!!.latestVersion),
                        color = C_white
                    )
                    if (updateFound!!.releaseNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cambios en esta versión:",
                            style = MaterialTheme.typography.labelMedium,
                            color = C_accent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .background(C_card2, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = updateFound!!.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = C_gray1
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateFound!!.downloadUrl))
                        ctx.startActivity(intent)
                        updateFound = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C_accent, contentColor = Color.Black)
                ) {
                    Text("Actualizar ahora")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateFound = null }) {
                    Text("Más tarde", color = C_gray1)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showQualityDialog) {
        SelectionDialog(
            title = "Calidad de video",
            options = AppSettings.qualityOptions,
            selectedOption = AppSettings.selectedQuality,
            onSelection = {
                AppSettings.selectedQuality = it
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showVideoFormatDialog) {
        SelectionDialog(
            title = "Formato de video",
            options = AppSettings.videoFormats,
            selectedOption = AppSettings.selectedVideoFormat,
            onSelection = {
                AppSettings.selectedVideoFormat = it
                showVideoFormatDialog = false
            },
            onDismiss = { showVideoFormatDialog = false }
        )
    }

    if (showThreadsDialog) {
        SelectionDialog(
            title = "Hilos de descarga paralelos",
            options = listOf("1", "3", "5", "8", "12", "16"),
            selectedOption = AppSettings.concurrentFragments,
            onSelection = {
                AppSettings.concurrentFragments = it
                showThreadsDialog = false
            },
            onDismiss = { showThreadsDialog = false }
        )
    }

    if (showSpeedDialog) {
        SelectionDialog(
            title = "Límite de velocidad",
            options = AppSettings.speedOptions,
            selectedOption = AppSettings.maxSpeed,
            onSelection = {
                AppSettings.maxSpeed = it
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = "Seleccionar Tema",
            options = AppSettings.themeOptions,
            selectedOption = AppSettings.themePreference,
            onSelection = {
                AppSettings.themePreference = it
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAccentDialog) {
        SelectionDialog(
            title = "Color de Acento",
            options = AppSettings.accentColorOptions,
            selectedOption = AppSettings.accentColorName,
            onSelection = {
                AppSettings.accentColorName = it
                showAccentDialog = false
            },
            onDismiss = { showAccentDialog = false }
        )
    }

    if (showProxyDialog) {
        InputDialog(
            title = "Configurar Proxy",
            placeholder = "http://127.0.0.1:8080",
            initialValue = AppSettings.proxyUrl,
            onConfirm = { AppSettings.proxyUrl = it },
            onDismiss = { showProxyDialog = false }
        )
    }

    if (showCustomArgsDialog) {
        InputDialog(
            title = "Argumentos yt-dlp",
            placeholder = "--restrict-filenames",
            initialValue = AppSettings.customArguments,
            onConfirm = { AppSettings.customArguments = it },
            onDismiss = { showCustomArgsDialog = false }
        )
    }

    if (showClipboardDialog) {
        SelectionDialog(
            title = "Acción al copiar enlace",
            options = listOf("banner", "auto", "disabled"),
            selectedOption = AppSettings.clipboardAction,
            onSelection = {
                AppSettings.clipboardAction = it
                showClipboardDialog = false
            },
            onDismiss = { showClipboardDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(C_bg)
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = C_white,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 1. Apariencia y Estética
                SettingsHeader("Apariencia y Estética", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsRow(Icons.Default.DarkMode, "Tema visual", AppSettings.themePreference, C_accent, C_white, C_gray1, C_card2) {
                            showThemeDialog = true
                        }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Palette, "Color dinámico", "Usar colores del sistema (Android 12+)", dynamicColor, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { dynamicColor = it }
                        if (!dynamicColor) {
                            HorizontalDivider(color = C_border, thickness = 1.dp)
                            SettingsRow(Icons.Default.ColorLens, "Color de acento", AppSettings.accentColorName, C_accent, C_white, C_gray1, C_card2) {
                                showAccentDialog = true
                            }
                        }
                    }
                }

                // 2. Notificaciones y Alertas
                SettingsHeader("Notificaciones y Alertas", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsToggleRow(Icons.Default.Notifications, "Notificaciones globales", "Activar/Desactivar avisos", notificationsEnabled, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { notificationsEnabled = it }
                        if (notificationsEnabled) {
                            HorizontalDivider(color = C_border, thickness = 1.dp)
                            SettingsToggleRow(Icons.Default.Speed, "Mostrar velocidad", "En la barra de notificaciones", showSpeedInNotif, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { showSpeedInNotif = it }
                            HorizontalDivider(color = C_border, thickness = 1.dp)
                            SettingsToggleRow(Icons.Default.Vibration, "Vibrar al finalizar", "Feedback háptico", vibrateOnComplete, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { vibrateOnComplete = it }
                            HorizontalDivider(color = C_border, thickness = 1.dp)
                            SettingsToggleRow(Icons.Default.VolumeUp, "Sonido al finalizar", "Alerta sonora", soundOnComplete, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { soundOnComplete = it }
                        }
                    }
                }

                // 3. Comportamiento y Reproducción
                SettingsHeader("Comportamiento y Reproducción", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsToggleRow(Icons.Default.OpenInNew, "Reproductor externo", "Abrir videos con app externa", useExternalPlayer, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { useExternalPlayer = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.History, "Mantener historial", "Guardar lista de descargas", keepHistory, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { keepHistory = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Replay, "Reintento automático", "Reintentar si falla la red", autoRetry, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { autoRetry = it }
                    }
                }

                // 4. Red y Descargas
                SettingsHeader("Red y Descargas", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsToggleRow(Icons.Default.Wifi, "Solo descargar con Wi-Fi", "Ahorra datos móviles", wifiOnly, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { wifiOnly = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Link, "Descarga automática", "Al copiar un enlace compatible", autoDownload, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { autoDownload = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        
                        SettingsRow(Icons.Default.Speed, "Límite de velocidad", AppSettings.maxSpeed, C_accent, C_white, C_gray1, C_card2) {
                            showSpeedDialog = true
                        }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.Hd, "Calidad predeterminada", AppSettings.selectedQuality, C_accent, C_white, C_gray1, C_card2) {
                            showQualityDialog = true
                        }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.VideoFile, "Formato de video", AppSettings.selectedVideoFormat, C_accent, C_white, C_gray1, C_card2) {
                            showVideoFormatDialog = true
                        }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.Download, "Hilos de descarga", AppSettings.concurrentFragments, C_accent, C_white, C_gray1, C_card2) {
                            showThreadsDialog = true
                        }
                        HorizontalDivider(color = C_border, thickness = 1.dp)

                        // Concurrent Downloads Stepper
                        Column(modifier = Modifier.padding(14.dp, 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Descargas simultáneas", color = C_white, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Máximo de transferencias activas", color = C_gray1, fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(8.dp)).border(1.dp, C_border, RoundedCornerShape(8.dp)).clickable { maxConcurrent = maxOf(1, maxConcurrent - 1) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("-", color = C_white, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
                                    Text("$maxConcurrent", color = C_accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    Box(
                                        modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(8.dp)).border(1.dp, C_border, RoundedCornerShape(8.dp)).clickable { maxConcurrent = minOf(5, maxConcurrent + 1) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("+", color = C_white, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                (1..5).forEach { n ->
                                    val isActive = n <= maxConcurrent
                                    Box(modifier = Modifier.weight(1f).height(4.dp).background(if (isActive) C_accent else C_card2, RoundedCornerShape(4.dp)))
                                }
                            }
                        }
                    }
                }

                // 5. Biblioteca y Metadatos
                SettingsHeader("Biblioteca y Metadatos", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsToggleRow(Icons.Default.Image, "Incrustar miniaturas", "Agrega portadas a los archivos", embedThumbnail, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { embedThumbnail = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.AutoMirrored.Filled.Label, "Incrustar metadatos", "Título, artista, álbum", embedMetadata, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { embedMetadata = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Subtitles, "Descargar subtítulos", "Incrustar subtítulos si existen", embedSubtitles, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { embedSubtitles = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.DeleteForever, "Confirmar eliminación", "Preguntar antes de borrar", confirmOnDelete, C_red, C_white, C_gray1, C_card2, C_border, C_bg) { confirmOnDelete = it }
                    }
                }

                // 6. Avanzado y Privacidad
                SettingsHeader("Avanzado y Privacidad", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsToggleRow(Icons.Default.Block, "Bloqueo de sponsors", "Omitir partes patrocinadas", sponsorBlock, C_amber, C_white, C_gray1, C_card2, C_border, C_bg) { sponsorBlock = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Public, "Saltar bloqueos geográficos", "Usar nodos alternativos", bypassGeo, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { bypassGeo = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.Dns, "Configurar Proxy", AppSettings.proxyUrl.ifEmpty { "Desactivado" }, C_accent, C_white, C_gray1, C_card2) { showProxyDialog = true }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.Code, "Argumentos yt-dlp", AppSettings.customArguments.ifEmpty { "Por defecto" }, C_accent, C_white, C_gray1, C_card2) { showCustomArgsDialog = true }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.ContentPaste, "Acción de portapapeles", AppSettings.clipboardAction, C_accent, C_white, C_gray1, C_card2) { showClipboardDialog = true }
                    }
                }

                // 7. Almacenamiento
                SettingsHeader("Almacenamiento", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column {
                        SettingsRow(Icons.Default.Folder, "Directorio de descarga", AppSettings.downloadLocation.substringAfterLast("/"), C_accent, C_white, C_gray1, C_card2) {
                            launcher.launch(null)
                        }
                    }
                }

                // Clear Cache
                val cacheBg = when(cacheState) {
                    2 -> Color(0xFF0E1F0E)
                    1 -> Color(0xFF1A1A1E)
                    else -> Color(0xFF18110A)
                }
                val cacheBorder = when(cacheState) {
                    2 -> C_green.copy(alpha = 0.33f)
                    1 -> C_accent.copy(alpha = 0.2f)
                    else -> C_amber.copy(alpha = 0.26f)
                }
                val cacheIconBg = when(cacheState) {
                    2 -> C_green.copy(alpha = 0.13f)
                    1 -> C_accent.copy(alpha = 0.13f)
                    else -> C_amber.copy(alpha = 0.13f)
                }
                val cacheAccent = when(cacheState) {
                    2 -> C_green
                    1 -> C_accent
                    else -> C_amber
                }

                Surface(
                    color = cacheBg, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, cacheBorder),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clickable {
                        if (cacheState == 0) {
                            scope.launch {
                                cacheState = 1
                                delay(2200)
                                cacheState = 2
                                delay(2000)
                                cacheState = 0
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(cacheIconBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            if (cacheState == 1) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = C_accent)
                            } else {
                                Icon(imageVector = if (cacheState == 2) Icons.Default.Check else Icons.Default.Delete, contentDescription = null, tint = cacheAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when(cacheState) {
                                    1 -> "Limpiando..."
                                    2 -> "Caché eliminado"
                                    else -> "Limpiar caché"
                                }, 
                                color = cacheAccent, 
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Libera espacio de archivos temporales", color = C_gray1, fontSize = 11.sp)
                        }
                    }
                }
                
                // 8. General
                SettingsHeader("General", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(14.dp, 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = C_accent, modifier = Modifier.size(16.dp))
                                }
                                Text("Versión de la app", color = C_white, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(modifier = Modifier.background(C_card2, RoundedCornerShape(20.dp)).border(1.dp, C_border, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("v${BuildConfig.VERSION_NAME}", color = C_gray1, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.Update, "Verificar actualizaciones", if (isCheckingUpdates) "Buscando..." else "Ver ahora", C_accent, C_white, C_gray1, C_card2) {
                            if (!isCheckingUpdates) {
                                scope.launch {
                                    isCheckingUpdates = true
                                    val result = UpdateManager.checkForUpdates()
                                    isCheckingUpdates = false
                                    result.onSuccess { info ->
                                        if (info != null && UpdateManager.isNewerVersion(info.latestVersion, BuildConfig.VERSION_NAME)) {
                                            updateFound = info
                                        } else {
                                            Toast.makeText(ctx, ctx.getString(R.string.settings_update_not_available), Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure { e ->
                                        Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsRow(Icons.Default.Code, "Repositorio GitHub", "Ver código", C_accent, C_white, C_gray1, C_card2) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fabianfamr/FabiDownloader"))
                            ctx.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    colorAccent: Color,
    textColor: Color,
    grayColor: Color,
    card2Color: Color,
    borderColor: Color,
    bgColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(14.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(32.dp).background(if (checked) colorAccent.copy(alpha = 0.15f) else card2Color, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (checked) colorAccent else grayColor, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = grayColor, fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = bgColor,
                checkedTrackColor = colorAccent,
                uncheckedThumbColor = grayColor,
                uncheckedTrackColor = card2Color,
                uncheckedBorderColor = borderColor
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailing: String,
    colorAccent: Color,
    textColor: Color,
    grayColor: Color,
    card2Color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(card2Color, RoundedCornerShape(10.dp)), 
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colorAccent, modifier = Modifier.size(16.dp))
            }
            Text(title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(trailing, color = grayColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, null, tint = grayColor, modifier = Modifier.size(16.dp))
        }
    }
}
