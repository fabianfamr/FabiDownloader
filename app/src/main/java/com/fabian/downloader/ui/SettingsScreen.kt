package com.fabian.downloader.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.BuildConfig
import com.fabian.downloader.database.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    database: AppDatabase, 
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    onNavigateToDownloadSettings: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showCookiesDialog by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }
    var currentCookiesText by remember { mutableStateOf(AppSettings.cookiesText) }
    var isUpdatingYtdlp by remember { mutableStateOf(false) }

    var generalVisible by remember { mutableStateOf(false) }
    var downloadVisible by remember { mutableStateOf(false) }
    var engineVisible by remember { mutableStateOf(false) }
    var aboutVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        generalVisible = true
        kotlinx.coroutines.delay(70)
        downloadVisible = true
        kotlinx.coroutines.delay(70)
        engineVisible = true
        kotlinx.coroutines.delay(70)
        aboutVisible = true
    }

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
    val C_red = Color(0xFFEF5350)
    val C_redDim = Color(0x1FEF5350)
    val C_green = Color(0xFF2ECC71)
    val C_amber = Color(0xFFF59E0B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(C_bg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = C_white
        )
        Text(
            text = "Ajusta las preferencias de la aplicación",
            style = MaterialTheme.typography.bodyMedium,
            color = C_gray1,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Settings Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(
                visible = generalVisible,
                enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(350, easing = FastOutSlowInEasing))
            ) {
                Column {
                    SettingsSectionHeader("Preferencias Generales", C_accent)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = C_card,
                        border = BorderStroke(1.dp, C_border),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column {
                            SettingsSettingItem(Icons.Default.Palette, "Tema", trailing = AppSettings.themePreference, colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                val nextIndex = (AppSettings.themeOptions.indexOf(AppSettings.themePreference) + 1) % AppSettings.themeOptions.size
                                AppSettings.themePreference = AppSettings.themeOptions[nextIndex]
                            }
                            HorizontalDivider(color = C_border, modifier = Modifier.padding(horizontal = 16.dp))
                            val currentClipboardLabel = when (AppSettings.clipboardAction) {
                                "banner" -> "Mostrar banner"
                                "auto" -> "Abrir automáticamente"
                                else -> "Desactivado"
                            }
                            SettingsSettingItem(Icons.Default.ContentPaste, "Detección de portapapeles", trailing = currentClipboardLabel, colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                showClipboardDialog = true
                            }
                            HorizontalDivider(color = C_border, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsToggleSetting(Icons.Default.Notifications, "Notificaciones de sistema", AppSettings.notificationsEnabled, colorAccent = C_accent, textColor = C_white, card2Color = C_card2) {
                                AppSettings.notificationsEnabled = it
                            }
                            HorizontalDivider(color = C_border, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsToggleSetting(Icons.Default.NetworkCell, "Ahorro de datos (red móvil)", AppSettings.dataSaverEnabled, colorAccent = C_accent, textColor = C_white, card2Color = C_card2) {
                                AppSettings.dataSaverEnabled = it
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))

            AnimatedVisibility(
                visible = downloadVisible,
                enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(350, easing = FastOutSlowInEasing))
            ) {
                Column {
                    SettingsSectionHeader("Configuración de Descargas", C_accent)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = C_card,
                        border = BorderStroke(1.dp, C_border),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column {
                            SettingsSettingItem(Icons.Default.Settings, "Ajustes avanzados de descarga", trailing = null, colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                onNavigateToDownloadSettings()
                            }
                            HorizontalDivider(color = C_border, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsToggleSetting(Icons.Default.DeleteForever, "Confirmar al eliminar", AppSettings.confirmOnDelete, colorAccent = C_accent, textColor = C_white, card2Color = C_card2) {
                                AppSettings.confirmOnDelete = it
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))

            AnimatedVisibility(
                visible = engineVisible,
                enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(350, easing = FastOutSlowInEasing))
            ) {
                Column {
                    SettingsSectionHeader("Conectividad y Motores", C_accent)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = C_card,
                        border = BorderStroke(1.dp, C_border),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column {
                            SettingsSettingItem(Icons.Default.Refresh, "Actualizar motor yt-dlp", trailing = if (isUpdatingYtdlp) "Actualizando..." else "Actualizar", colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                if (!isUpdatingYtdlp) {
                                    isUpdatingYtdlp = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val result = com.yausername.youtubedl_android.YoutubeDL.getInstance().updateYoutubeDL(context)
                                            val statusMsg = when (result) {
                                                com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.DONE -> "¡Actualizado con éxito!"
                                                com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "Ya está en la versión más reciente"
                                                else -> "Estado: $result"
                                            }
                                            scope.launch {
                                                snackbarHostState?.showSnackbar(statusMsg)
                                            }
                                        } catch (e: Exception) {
                                            scope.launch {
                                                snackbarHostState?.showSnackbar("Error al actualizar: ${e.message}")
                                            }
                                        } finally {
                                            isUpdatingYtdlp = false
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = C_border, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsSettingItem(Icons.Default.Lock, "Configurar Cookies (cookies.txt)", trailing = if (AppSettings.cookiesText.isNotEmpty()) "Configurado" else "No configurado", colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                currentCookiesText = AppSettings.cookiesText
                                showCookiesDialog = true
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))

            AnimatedVisibility(
                visible = aboutVisible,
                enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(350, easing = FastOutSlowInEasing))
            ) {
                Column {
                    SettingsSectionHeader("Acerca de Fabi Downloader", C_accent)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = C_card,
                        border = BorderStroke(1.dp, C_border),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column {
                            SettingsSettingItem(Icons.Default.Info, "Versión de la Aplicación", trailing = BuildConfig.VERSION_NAME, colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                scope.launch { snackbarHostState?.showSnackbar("Fabi Downloader v${BuildConfig.VERSION_NAME}") }
                            }
                            HorizontalDivider(color = C_border, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsSettingItem(Icons.Default.Code, "Repositorio GitHub", trailing = "Abrir", colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fabianfamr/FabiDownloader"))
                                context.startActivity(intent)
                            }
                            HorizontalDivider(color = C_border, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsSettingItem(Icons.Default.Gavel, "Licencia", trailing = "MIT", colorAccent = C_accent, textColor = C_white, grayColor = C_gray1, card2Color = C_card2) {
                                scope.launch { snackbarHostState?.showSnackbar("Licencia MIT - Código Abierto") }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showCookiesDialog) {
        AlertDialog(
            onDismissRequest = { showCookiesDialog = false },
            title = { Text("Configurar Cookies (cookies.txt)", fontWeight = FontWeight.Bold, color = C_white) },
            containerColor = C_card,
            text = {
                Column {
                    Text(
                        "Pega el contenido de un archivo cookies.txt en formato Netscape para descargas con restricciones de edad, privadas o con límites (ej. Instagram privado o YouTube).",
                        fontSize = 13.sp,
                        color = C_gray1,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = currentCookiesText,
                        onValueChange = { currentCookiesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("# Netscape HTTP Cookie File...", fontSize = 12.sp, color = C_gray1) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = C_white),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = C_white,
                            unfocusedTextColor = C_white,
                            focusedBorderColor = C_accent,
                            unfocusedBorderColor = C_border,
                            unfocusedContainerColor = C_card2,
                            focusedContainerColor = C_card2
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppSettings.cookiesText = currentCookiesText
                        showCookiesDialog = false
                        scope.launch {
                            snackbarHostState?.showSnackbar("Cookies guardadas con éxito")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C_accent, contentColor = Color(0xFF0A0A0C))
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCookiesDialog = false }
                ) {
                    Text("Cancelar", color = C_white)
                }
            }
        )
    }

    if (showClipboardDialog) {
        val clipboardOptions = listOf("Mostrar banner", "Abrir automáticamente", "Desactivado")
        val currentLabel = when (AppSettings.clipboardAction) {
            "banner" -> "Mostrar banner"
            "auto" -> "Abrir automáticamente"
            else -> "Desactivado"
        }
        SettingsSelectionDialog(
            title = "Detección de portapapeles",
            options = clipboardOptions,
            selectedOption = currentLabel,
            onSelection = { selected ->
                AppSettings.clipboardAction = when (selected) {
                    "Mostrar banner" -> "banner"
                    "Abrir automáticamente" -> "auto"
                    else -> "disabled"
                }
                showClipboardDialog = false
            },
            onDismiss = { showClipboardDialog = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, colorAccent: Color) {
    Text(
        text = title,
        color = colorAccent,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 10.dp)
    )
}

@Composable
fun SettingsSettingItem(
    icon: ImageVector,
    title: String,
    trailing: String?,
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colorAccent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colorAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Surface(
                color = card2Color,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colorAccent.copy(alpha = 0.15f))
            ) {
                Text(
                    text = trailing,
                    color = colorAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        } else {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = grayColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsToggleSetting(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    colorAccent: Color,
    textColor: Color,
    card2Color: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colorAccent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colorAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0A0C),
                checkedTrackColor = colorAccent,
                uncheckedThumbColor = colorAccent.copy(alpha = 0.5f),
                uncheckedTrackColor = card2Color,
                uncheckedBorderColor = colorAccent.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
fun SettingsSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelection: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val C_card = Color(0xFF161619)
    val C_border = Color(0xFF242428)
    val C_accent = Color(0xFF00E5FF)
    val C_white = Color(0xFFFFFFFF)
    val C_gray1 = Color(0xFF8A8A96)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = C_white) },
        containerColor = C_card,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    val isSel = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelection(option) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { onSelection(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = C_accent,
                                unselectedColor = C_gray1
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option,
                            color = C_white,
                            fontSize = 14.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = C_white)
            }
        }
    )
}
