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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
            @Suppress("UNCHECKED_CAST")
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

    // Entrance animation states
    var headerVisible by remember { mutableStateOf(false) }
    var searchBarVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        headerVisible = true
        kotlinx.coroutines.delay(120)
        searchBarVisible = true
        kotlinx.coroutines.delay(100)
        contentVisible = true
    }

    // --- START FIGMA EXEMPLAR DESIGN SYSTEM ---
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
    val C_gray3 = Color(0xFF32323A)
    val C_red = Color(0xFFEF5350)
    val C_redDim = Color(0x1FEF5350)
    val C_green = Color(0xFF2ECC71)
    val C_amber = Color(0xFFF59E0B)

    // Platform definition matching React App.tsx
    val platforms = remember {
        listOf(
            PlatformData("youtube", "YouTube", Color(0xFFFF0000), "youtube.com", Icons.Default.PlayCircle),
            PlatformData("tiktok", "TikTok", Color(0xFF69C9D0), "tiktok.com", Icons.Default.MusicNote),
            PlatformData("instagram", "Instagram", Color(0xFFE1306C), "instagram.com", Icons.Default.CameraAlt),
            PlatformData("twitter", "X", Color(0xFFFFFFFF), "x.com", Icons.Default.Share)
        )
    }

    val detectedPlatform = remember(query) {
        val q = query.lowercase()
        platforms.find { p -> 
            q.contains(p.domain) || 
            (p.id == "youtube" && q.contains("youtu.be")) || 
            (p.id == "twitter" && q.contains("twitter.com")) 
        }
    }

    var isAnalyzingState by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(C_bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header (exactly as React App.tsx)
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C_accentDim)
                                .border(1.dp, C_accentGlow, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = C_accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Downloader",
                                color = C_white,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 1.2.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(C_green)
                                )
                                Text(
                                    text = "Listo para descargar",
                                    color = C_gray1,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Surface(
                        color = C_accentDim,
                        border = BorderStroke(1.dp, C_accentGlow),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "v${com.fabian.downloader.BuildConfig.VERSION_NAME}",
                            color = C_accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Hero Text Section
            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn(tween(450)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(450, easing = FastOutSlowInEasing))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pega tu enlace",
                        color = C_white,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 1.3.sp
                    )
                    Text(
                        text = "Detectamos la plataforma automáticamente",
                        color = C_gray1,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                    )
                }
            }

            // Platform Badges (exactly as React App.tsx)
            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn(tween(450)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(450, easing = FastOutSlowInEasing))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        platforms.forEach { p ->
                            val active = detectedPlatform?.id == p.id
                            val animatedScale by animateFloatAsState(if (active) 1.06f else 1f)
                            
                            Box(
                                modifier = Modifier
                                    .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (active) p.color.copy(alpha = 0.1f) else C_card)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (active) p.color.copy(alpha = 0.5f) else C_border,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = p.icon,
                                        contentDescription = p.label,
                                        tint = if (active) p.color else C_gray1,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = p.label,
                                        color = if (active) C_white else C_gray1,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Unified Custom Text Field (exactly as React App.tsx)
            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn(tween(500)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(500, easing = FastOutSlowInEasing))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { 
                            query = it 
                            isAnalyzingState = false
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = C_white,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(C_card2)
                            .border(
                                width = 1.5.dp,
                                color = if (detectedPlatform != null) detectedPlatform.color.copy(alpha = 0.4f) else C_border,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("submit_link_input"),
                        singleLine = true,
                        cursorBrush = SolidColor(C_accent),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 40.dp, end = if (query.isNotEmpty()) 90.dp else 78.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Pegar enlace de video o audio...",
                                        color = C_gray1,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Left Platform/Link Icon inside Input
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 14.dp)
                    ) {
                        Icon(
                            imageVector = if (detectedPlatform != null) detectedPlatform.icon else Icons.Default.Link,
                            contentDescription = null,
                            tint = if (detectedPlatform != null) detectedPlatform.color else C_gray1,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Right Clear & Pegar buttons inside Input
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("clear_link_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = C_gray1,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Pegar Capsule Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(C_accentDim)
                                .border(1.dp, C_accentGlow, RoundedCornerShape(10.dp))
                                .clickable {
                                    try {
                                        if (clipboardManager.hasPrimaryClip()) {
                                            val clipData = clipboardManager.primaryClip
                                            if (clipData != null && clipData.itemCount > 0) {
                                                val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                                                if (clipText.isNotEmpty()) {
                                                    query = clipText
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Pegar",
                                    tint = C_accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Pegar",
                                    color = C_accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Big CTA Button ("Analizar Enlace" - exactly as React App.tsx)
            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn(tween(550)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(550, easing = FastOutSlowInEasing))
            ) {
                val isQueryValid = query.isNotEmpty() && (query.startsWith("http") || query.contains("."))
                Button(
                    onClick = {
                        if (isQueryValid && !isAnalyzingState) {
                            scope.launch {
                                isAnalyzingState = true
                                // Simulate elegant analysis delay to mirror the Figma prototype transition
                                kotlinx.coroutines.delay(800)
                                isAnalyzingState = false
                                viewModel.saveSearch(query)
                                urlToDownloadInDialog = query
                                query = ""
                            }
                        } else if (query.isNotEmpty()) {
                            android.widget.Toast.makeText(context, "Por favor introduce un enlace válido", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = query.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_link_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAnalyzingState) C_green else if (isQueryValid) C_accent else C_card2,
                        contentColor = if (isQueryValid) Color(0xFF0A0A0C) else C_gray2,
                        disabledContainerColor = C_card2,
                        disabledContentColor = C_gray2
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAnalyzingState) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF0A0A0C),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Analizando...",
                                    color = Color(0xFF0A0A0C),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isQueryValid) Color(0xFF0A0A0C) else C_gray2,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Analizar Enlace",
                                    color = if (isQueryValid) Color(0xFF0A0A0C) else C_gray2,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // Quick One-Touch Actions for Audio & Video
            AnimatedVisibility(
                visible = query.isNotEmpty() && (query.startsWith("http://") || query.startsWith("https://") || (query.contains(".") && !query.contains(" "))),
                enter = fadeIn(tween(250)) + expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(200)) + shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .testTag("quick_download_panel"),
                    shape = RoundedCornerShape(24.dp),
                    color = C_card,
                    border = BorderStroke(1.dp, C_border)
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
                                tint = C_accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Descarga con un Solo Toque",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = C_white
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
                                    .heightIn(min = 52.dp)
                                    .testTag("quick_audio_mp3_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = C_accentDim,
                                    contentColor = C_accent
                                ),
                                border = BorderStroke(1.dp, C_accentGlow),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp), tint = C_accent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solo MP3", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = C_white)
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
                                    .heightIn(min = 52.dp)
                                    .testTag("quick_audio_m4a_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x1AF59E0B),
                                    contentColor = C_amber
                                ),
                                border = BorderStroke(1.dp, C_amber.copy(alpha = 0.22f)),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp), tint = C_amber)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solo M4A", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = C_white)
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
                                    .heightIn(min = 52.dp)
                                    .testTag("quick_video_mp4_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x1A2ECC71),
                                    contentColor = C_green
                                ),
                                border = BorderStroke(1.dp, C_green.copy(alpha = 0.22f)),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = C_green)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Video MP4", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = C_white)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Recent Downloads
            AnimatedVisibility(
                visible = contentVisible && recentDownloads.isNotEmpty(),
                enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
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
                            text = "Descargas Recientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = C_white
                        )
                        Text(
                            text = "Ver todas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = C_accent,
                            modifier = Modifier
                                .clickable { onNavigateToDownloads() }
                                .padding(4.dp)
                        )
                    }
                    
                    recentDownloads.forEach { record ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { openFile(record) }
                                .testTag("recent_record_item_${record.id}"),
                            color = C_card,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (record.format == "MP4") C_green.copy(alpha = 0.22f) else C_accentGlow
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
                                            color = if (record.format == "MP4") Color(0x1A2ECC71) else C_accentDim,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (record.format == "MP4") Icons.Default.PlayArrow else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (record.format == "MP4") C_green else C_accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = C_white,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Surface(
                                            color = if (record.format == "MP4") Color(0x112ECC71) else C_accentDim,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = record.format,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (record.format == "MP4") C_green else C_accent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = if (record.isCompleted) "Completado" else "En progreso",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = C_gray1
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completado",
                                    tint = C_green,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Animated Clipboard Card
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
                    color = C_card,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, C_accentGlow)
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
                                    .background(C_accentDim, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = C_accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enlace detectado en portapapeles",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = C_white
                                )
                                Text(
                                    text = clipboardUrl ?: "",
                                    fontSize = 12.sp,
                                    color = C_gray1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { clipboardUrl = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(C_border, CircleShape)
                                    .testTag("dismiss_clipboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Descartar",
                                    tint = C_white,
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
                                    color = C_accent
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
                                    containerColor = C_accentDim,
                                    contentColor = C_accent
                                )
                            ) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(15.dp), tint = C_accent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Solo Audio", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = C_white)
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
                                    containerColor = C_accent,
                                    contentColor = Color(0xFF0A0A0C)
                                )
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
                onClose = { urlToDownloadInDialog = null },
                onNavigateToDownloads = {
                    urlToDownloadInDialog = null
                    onNavigateToDownloads()
                }
            )
        }
    }
}

data class PlatformData(
    val id: String,
    val label: String,
    val color: Color,
    val domain: String,
    val icon: ImageVector
)
