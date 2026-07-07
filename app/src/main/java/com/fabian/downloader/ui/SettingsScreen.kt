package com.fabian.downloader.ui

import android.widget.Toast
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val C_bg = Color(0xFF0A0A0C)
    val C_card = Color(0xFF161619)
    val C_card2 = Color(0xFF1E1E22)
    val C_border = Color(0xFF242428)
    val C_accent = Color(0xFF00E5FF)
    val C_accentDim = Color(0x1A00E5FF)
    val C_accentGlow = Color(0x3800E5FF)
    val C_white = Color(0xFFFFFFFF)
    val C_gray1 = Color(0xFF8A8A96)
    val C_gray2 = Color(0xFF4A4A56)
    val C_green = Color(0xFF2ECC71)
    val C_amber = Color(0xFFF59E0B)
    val C_red = Color(0xFFEF5350)

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
    
    var cacheCleared by remember { mutableStateOf(false) }

    LaunchedEffect(maxConcurrent) { AppSettings.maxConcurrentDownloads = maxConcurrent }
    LaunchedEffect(autoDownload) { AppSettings.clipboardAction = if (autoDownload) "auto" else "disabled" }
    LaunchedEffect(wifiOnly) { AppSettings.dataSaverEnabled = wifiOnly }
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

                // 1. Personalización
                SettingsHeader("Personalización", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp, 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = C_accent, modifier = Modifier.size(16.dp))
                                }
                                Text("Tema visual", color = C_white, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(C_card2, RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val themes = listOf("Claro", "Oscuro", "Sistema")
                                themes.forEach { t ->
                                    val isSelected = AppSettings.themePreference == t
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) C_accent else Color.Transparent)
                                            .clickable { AppSettings.themePreference = t }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = t, color = if (isSelected) C_bg else C_gray1, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Red y Descargas
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

                // 3. Biblioteca y Metadatos
                SettingsHeader("Biblioteca y Metadatos", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsToggleRow(Icons.Default.Image, "Incrustar miniaturas", "Agrega portadas a los archivos", embedThumbnail, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { embedThumbnail = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Label, "Incrustar metadatos", "Título, artista, álbum", embedMetadata, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { embedMetadata = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Subtitles, "Descargar subtítulos", "Incrustar subtítulos si existen", embedSubtitles, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { embedSubtitles = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.DeleteForever, "Confirmar eliminación", "Preguntar antes de borrar", confirmOnDelete, C_red, C_white, C_gray1, C_card2, C_border, C_bg) { confirmOnDelete = it }
                    }
                }

                // 4. Avanzado y Privacidad
                SettingsHeader("Avanzado y Privacidad", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column {
                        SettingsToggleRow(Icons.Default.Block, "Bloqueo de sponsors", "Omitir partes patrocinadas", sponsorBlock, C_amber, C_white, C_gray1, C_card2, C_border, C_bg) { sponsorBlock = it }
                        HorizontalDivider(color = C_border, thickness = 1.dp)
                        SettingsToggleRow(Icons.Default.Public, "Saltar bloqueos geográficos", "Usar nodos alternativos", bypassGeo, C_accent, C_white, C_gray1, C_card2, C_border, C_bg) { bypassGeo = it }
                    }
                }

                // 5. Almacenamiento
                SettingsHeader("Almacenamiento", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).background(C_card2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = C_accent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Directorio base", color = C_white, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "/storage/emulated/0/Downloader/", color = C_gray1, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = C_accent, modifier = Modifier.size(20.dp))
                    }
                }

                // Clear Cache
                val cacheBg = if (cacheCleared) Color(0xFF0E1F0E) else Color(0xFF18110A)
                val cacheBorder = if (cacheCleared) C_green.copy(alpha = 0.33f) else C_amber.copy(alpha = 0.26f)
                val cacheIconBg = if (cacheCleared) C_green.copy(alpha = 0.13f) else C_amber.copy(alpha = 0.13f)
                val cacheAccent = if (cacheCleared) C_green else C_amber

                Surface(
                    color = cacheBg, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, cacheBorder),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clickable {
                        if (!cacheCleared) {
                            cacheCleared = true
                            scope.launch {
                                delay(2200)
                                cacheCleared = false
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(cacheIconBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = if (cacheCleared) Icons.Default.Check else Icons.Default.Delete, contentDescription = null, tint = cacheAccent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(if (cacheCleared) "Caché eliminado" else "Limpiar caché", color = cacheAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Libera espacio de archivos temporales", color = C_gray1, fontSize = 11.sp)
                        }
                    }
                }
                
                // 6. General
                SettingsHeader("General", C_gray2)
                Surface(
                    color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
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
