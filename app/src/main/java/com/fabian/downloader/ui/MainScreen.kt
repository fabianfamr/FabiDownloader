package com.fabian.downloader.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import com.fabian.downloader.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.*
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MainScreen(
    database: AppDatabase, 
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    onNavigateToDownloads: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val viewModel: MainViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(application, database) as T
            }
        }
    )

    // Observe downloads flow in real-time
    val downloadsList by database.downloadDao().getAllDownloads().collectAsStateWithLifecycle(initialValue = emptyList())
    // Get last 3 completed downloads
    val recentDownloads by remember(downloadsList) {
        derivedStateOf { 
            downloadsList
                .filter { it.isCompleted }
                .sortedByDescending { it.timestamp }
                .take(3) 
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    var urlToDownloadInDialog by remember { mutableStateOf<String?>(null) }
    var lastProcessedClipboardUrl by remember { mutableStateOf("") }
    val clipboardManager = remember {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    val action = AppSettings.clipboardAction
                    if (action != "disabled" && clipboardManager.hasPrimaryClip()) {
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                            if ((clipText.startsWith("http://") || clipText.startsWith("https://")) && clipText != lastProcessedClipboardUrl) {
                                lastProcessedClipboardUrl = clipText
                                if (action == "auto") {
                                    urlToDownloadInDialog = clipText
                                } else {
                                    clipboardUrl = clipText
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore background clipboard access restrictions on some Android versions
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val openFile: (DownloadRecord) -> Unit = { record ->
        try {
            val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(context, record.title, record.id, record.format)
            
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.fabian.downloader.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (record.format == "MP4") "video/*" else "audio/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } else {
                scope.launch {
                    snackbarHostState?.showSnackbar("El archivo no existe o fue eliminado")
                }
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState?.showSnackbar("Error al abrir: ${e.localizedMessage}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Premium Styled App Brand Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = Color.Transparent
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Fabi Downloader",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            letterSpacing = (-0.75).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Descarga videos y audio al instante",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Modern Floating glass-capsule Link Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                tonalElevation = 2.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(42.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).testTag("link_text_input"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    "Pega un enlace de video aquí...", 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        )
                    )
                    
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.testTag("clear_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear, 
                                contentDescription = "Limpiar", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { 
                            if (query.isNotEmpty()) {
                                if (query.startsWith("http") || query.contains(".")) {
                                    viewModel.saveSearch(query)
                                    urlToDownloadInDialog = query
                                    query = ""
                                } else {
                                    android.widget.Toast.makeText(context, "Por favor introduce un enlace válido", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(26.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .padding(end = 2.dp)
                            .testTag("submit_link_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("Ir", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // Quick One-Touch Actions for Audio & Video
            AnimatedVisibility(
                visible = query.isNotEmpty() && (query.startsWith("http://") || query.startsWith("https://") || (query.contains(".") && !query.contains(" "))),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .testTag("quick_download_panel"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Descarga con un Solo Toque",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Audio MP3 button
                            Button(
                                onClick = {
                                    val urlToDownload = query.trim()
                                    viewModel.downloadVideo(
                                        url = urlToDownload,
                                        quality = "160",
                                        format = "MP3",
                                        title = "Procesando enlace..."
                                    )
                                    query = ""
                                    scope.launch {
                                        snackbarHostState?.showSnackbar("🎵 Descargando audio MP3 en segundo plano...")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("quick_audio_mp3_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solo MP3", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Audio M4A button
                            Button(
                                onClick = {
                                    val urlToDownload = query.trim()
                                    viewModel.downloadVideo(
                                        url = urlToDownload,
                                        quality = "128",
                                        format = "M4A",
                                        title = "Procesando enlace..."
                                    )
                                    query = ""
                                    scope.launch {
                                        snackbarHostState?.showSnackbar("🎵 Descargando audio M4A en segundo plano...")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("quick_audio_m4a_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solo M4A", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Video MP4 button
                            Button(
                                onClick = {
                                    val urlToDownload = query.trim()
                                    viewModel.downloadVideo(
                                        url = urlToDownload,
                                        quality = "720p",
                                        format = "MP4",
                                        title = "Procesando enlace..."
                                    )
                                    query = ""
                                    scope.launch {
                                        snackbarHostState?.showSnackbar("📹 Descargando video MP4 (720p) en segundo plano...")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("quick_video_mp4_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Video MP4", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Recent Downloads
            if (recentDownloads.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Descargas Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Ver todas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToDownloads() }
                            .padding(4.dp)
                    )
                }
                
                recentDownloads.forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { openFile(record) }
                            .testTag("recent_record_item_${record.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (record.format == "MP4") 
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) 
                            else 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        color = if (record.format == "MP4") 
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) 
                                        else 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (record.format == "MP4") Icons.Default.PlayArrow else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (record.format == "MP4") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Surface(
                                        color = if (record.format == "MP4") 
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) 
                                        else 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = record.format,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (record.format == "MP4") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = if (record.isCompleted) "Completado" else "En progreso",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completado",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Animated clipboard card
            AnimatedVisibility(
                visible = clipboardUrl != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("clipboard_notification_card"),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enlace detectado en portapapeles",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = clipboardUrl ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { clipboardUrl = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), CircleShape)
                                    .testTag("dismiss_clipboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Descartar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    query = clipboardUrl ?: ""
                                    clipboardUrl = null
                                },
                                modifier = Modifier.testTag("paste_clipboard_button")
                            ) {
                                Text(
                                    "Pegar", 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            FilledTonalButton(
                                onClick = {
                                    val urlToDownload = clipboardUrl
                                    if (!urlToDownload.isNullOrEmpty()) {
                                        viewModel.downloadVideo(
                                            url = urlToDownload,
                                            quality = "160",
                                            format = "MP3",
                                            title = "Procesando enlace..."
                                        )
                                        clipboardUrl = null
                                        scope.launch {
                                            snackbarHostState?.showSnackbar("🎵 Descargando audio MP3 en segundo plano...")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("clipboard_quick_audio_button"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Solo Audio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            
                            Button(
                                onClick = {
                                    val urlToDownload = clipboardUrl
                                    if (!urlToDownload.isNullOrEmpty()) {
                                        urlToDownloadInDialog = urlToDownload
                                        clipboardUrl = null
                                    }
                                },
                                modifier = Modifier.testTag("clipboard_analyze_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analizar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (urlToDownloadInDialog != null) {
            SharePopupScreen(
                url = urlToDownloadInDialog!!,
                viewModel = viewModel,
                onClose = { urlToDownloadInDialog = null }
            )
        }
    }
}

