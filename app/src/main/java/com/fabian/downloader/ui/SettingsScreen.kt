package com.fabian.downloader.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Ajusta las preferencias de la aplicación",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Settings Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SettingSectionHeader("Preferencias Generales")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column {
                    SettingItem(Icons.Default.Palette, "Tema", trailing = AppSettings.themePreference) {
                        val nextIndex = (AppSettings.themeOptions.indexOf(AppSettings.themePreference) + 1) % AppSettings.themeOptions.size
                        AppSettings.themePreference = AppSettings.themeOptions[nextIndex]
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    val currentClipboardLabel = when (AppSettings.clipboardAction) {
                        "banner" -> "Mostrar banner"
                        "auto" -> "Abrir automáticamente"
                        else -> "Desactivado"
                    }
                    SettingItem(Icons.Default.ContentPaste, "Detección de portapapeles", trailing = currentClipboardLabel) {
                        showClipboardDialog = true
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    ToggleSetting(Icons.Default.Notifications, "Notificaciones de sistema", AppSettings.notificationsEnabled) {
                        AppSettings.notificationsEnabled = it
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    ToggleSetting(Icons.Default.NetworkCell, "Ahorro de datos (red móvil)", AppSettings.dataSaverEnabled) {
                        AppSettings.dataSaverEnabled = it
                    }
                }
            }
            
            SettingSectionHeader("Configuración de Descargas")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column {
                    SettingItem(Icons.Default.Settings, "Ajustes avanzados de descarga", trailing = null) {
                        onNavigateToDownloadSettings()
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    ToggleSetting(Icons.Default.DeleteForever, "Confirmar al eliminar", AppSettings.confirmOnDelete) {
                        AppSettings.confirmOnDelete = it
                    }
                }
            }
            
            SettingSectionHeader("Conectividad y Motores")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column {
                    SettingItem(Icons.Default.Refresh, "Actualizar motor yt-dlp", trailing = if (isUpdatingYtdlp) "Actualizando..." else "Actualizar") {
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(Icons.Default.Lock, "Configurar Cookies (cookies.txt)", trailing = if (AppSettings.cookiesText.isNotEmpty()) "Configurado" else "No configurado") {
                        currentCookiesText = AppSettings.cookiesText
                        showCookiesDialog = true
                    }
                }
            }
            
            SettingSectionHeader("Acerca de Fabi Downloader")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column {
                    SettingItem(Icons.Default.Info, "Versión de la Aplicación", trailing = BuildConfig.VERSION_NAME) {
                        scope.launch { snackbarHostState?.showSnackbar("Fabi Downloader v${BuildConfig.VERSION_NAME}") }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(Icons.Default.Code, "Repositorio GitHub", trailing = "Abrir") {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fabianfamr/FabiDownloader"))
                        context.startActivity(intent)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(Icons.Default.Gavel, "Licencia", trailing = "MIT") {
                        scope.launch { snackbarHostState?.showSnackbar("Licencia MIT - Código Abierto") }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showCookiesDialog) {
        AlertDialog(
            onDismissRequest = { showCookiesDialog = false },
            title = { Text("Configurar Cookies (cookies.txt)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column {
                    Text(
                        "Pega el contenido de un archivo cookies.txt en formato Netscape para descargas con restricciones de edad, privadas o con límites (ej. Instagram privado o YouTube).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = currentCookiesText,
                        onValueChange = { currentCookiesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("# Netscape HTTP Cookie File...", fontSize = 12.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
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
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCookiesDialog = false }
                ) {
                    Text("Cancelar")
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
        SelectionDialog(
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
