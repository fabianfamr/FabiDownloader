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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import kotlin.math.cos
import kotlin.math.sin
import com.fabian.downloader.ui.theme.*
import kotlinx.coroutines.delay
import com.fabian.downloader.BuildConfig
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

enum class AnalyzeState { Idle, Loading, Success }

@Composable
fun MainScreen(
    database: AppDatabase, 
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    onNavigateToDownloads: () -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val application = ctx.applicationContext as Application
    
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
        ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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
            val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(ctx, record.title, record.id, record.format)
            
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    ctx,
                    "com.fabian.downloader.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (record.format == com.fabian.downloader.utils.Config.FORMAT_MP4) com.fabian.downloader.utils.Config.MIME_VIDEO else com.fabian.downloader.utils.Config.MIME_AUDIO)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(intent)
            } else {
                scope.launch {
                    snackbarHostState?.showSnackbar(ctx.getString(R.string.main_error_file_not_found))
                }
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState?.showSnackbar(ctx.getString(R.string.main_error_opening_file, e.localizedMessage ?: ""))
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
    val colors = MaterialTheme.fabiColors
    val C_bg = colors.background
    val C_card = colors.card
    val C_card2 = colors.cardSecondary
    val C_border = colors.border
    val C_accent = colors.accent
    val C_accentDim = colors.accentDim
    val C_accentGlow = colors.accentGlow
    val C_white = colors.textPrimary
    val C_gray1 = colors.textSecondary
    val C_gray2 = colors.textMuted
    val C_gray3 = colors.textDisabled
    val C_red = colors.error
    val C_redDim = colors.errorDim
    val C_green = colors.success
    val C_amber = colors.amber

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

    var analyzeState by remember { mutableStateOf(AnalyzeState.Idle) }

    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = 360f,
        animationSpec  = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label          = "orbitAngle",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulse",
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue  = -6f,
        targetValue   = 6f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "floatY",
    )

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
                                text = stringResource(R.string.main_app_title),
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
                                    text = stringResource(R.string.main_ready_to_download),
                                    color = C_gray1,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Version Pill (matching Ejemplo App.tsx)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(C_accentDim)
                            .border(1.dp, C_accentGlow, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = C_accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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
                    // Orbiting Icons Canvas
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .graphicsLayer { translationY = floatY },
                        contentAlignment = Alignment.Center
                    ) {
                        val orbitRadius = 75.dp
                        platforms.forEachIndexed { index, platform ->
                            val baseAngle = index * 90f
                            val angle = Math.toRadians((baseAngle + orbitAngle).toDouble())
                            val x = (cos(angle) * orbitRadius.value).dp
                            val y = (sin(angle) * orbitRadius.value).dp
                            Box(
                                modifier = Modifier
                                    .offset(x = x, y = y)
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(platform.color.copy(alpha = 0.25f))
                                    .border(1.dp, platform.color.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = platform.icon,
                                    contentDescription = null,
                                    tint = C_white,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Center pulsing download icon
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer { 
                                    scaleX = pulse
                                    scaleY = pulse 
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(C_accent.copy(alpha = 0.35f), Color.Transparent),
                                    )
                                )
                                .border(2.dp, C_accent.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = C_accent, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.main_paste_link_title),
                        style = TextStyle(
                            brush = Brush.horizontalGradient(listOf(C_accent, Color(0xFF7B61FF)))
                        ),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 1.3.sp
                    )
                    Text(
                        text = stringResource(R.string.main_paste_link_subtitle),
                        color = C_gray1,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Platform badges (mini inline indicators)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.main_detecting_prefix),
                            color = C_gray1,
                            fontSize = 14.sp
                        )
                        platforms.forEach { p ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(p.color.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = p.icon,
                                    contentDescription = null,
                                    tint = C_white,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.main_detecting_suffix),
                            color = C_gray1,
                            fontSize = 14.sp
                        )
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
                            analyzeState = AnalyzeState.Idle
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
                                        text = stringResource(R.string.main_input_placeholder),
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
                                    contentDescription = stringResource(R.string.main_clear_button),
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
                                    contentDescription = stringResource(R.string.main_paste_button),
                                    tint = C_accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.main_paste_button),
                                    color = C_accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            // Big CTA Button (stringResource(R.string.main_analyze_button) - exactly as React App.tsx)
            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn(tween(550)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(550, easing = FastOutSlowInEasing))
            ) {
                val isQueryValid = query.isNotEmpty() && (query.startsWith("http") || query.contains("."))
                Button(
                    onClick = {
                        if (isQueryValid && analyzeState == AnalyzeState.Idle) {
                            scope.launch {
                                analyzeState = AnalyzeState.Loading
                                // Simulate elegant analysis delay
                                delay(2000)
                                analyzeState = AnalyzeState.Success
                                delay(1200)
                                viewModel.saveSearch(query)
                                urlToDownloadInDialog = query
                                query = ""
                                analyzeState = AnalyzeState.Idle
                            }
                        } else if (query.isNotEmpty() && analyzeState == AnalyzeState.Idle) {
                            android.widget.Toast.makeText(ctx, ctx.getString(R.string.main_invalid_link), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = query.isNotEmpty() && analyzeState != AnalyzeState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_link_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when(analyzeState) {
                            AnalyzeState.Success -> C_green
                            AnalyzeState.Loading -> C_accent.copy(alpha = 0.5f)
                            else -> if (isQueryValid) C_accent else C_card2
                        },
                        contentColor = if (isQueryValid || analyzeState != AnalyzeState.Idle) Color(0xFF0A0A0C) else C_gray2,
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
                        if (analyzeState == AnalyzeState.Loading) {
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
                                    text = stringResource(R.string.main_analyzing_state),
                                    color = Color(0xFF0A0A0C),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else if (analyzeState == AnalyzeState.Success) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF0A0A0C),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.main_link_detected_state),
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
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isQueryValid) Color(0xFF0A0A0C) else C_gray2,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.main_analyze_button),
                                    color = if (isQueryValid) Color(0xFF0A0A0C) else C_gray2,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
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
                            text = stringResource(R.string.main_recent_downloads),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = C_white
                        )
                        Text(
                            text = stringResource(R.string.main_view_all),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = C_accent,
                            modifier = Modifier
                                .clickable { onNavigateToDownloads() }
                                .padding(4.dp)
                        )
                    }
                    
                    recentDownloads.forEach { record ->
                        val (platformIcon, platformColor) = getPlatformIconAndColor(record.url, record.format)
                        
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
                                color = platformColor.copy(alpha = 0.22f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(platformColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!record.thumbnailUrl.isNullOrEmpty()) {
                                        coil.compose.AsyncImage(
                                            model = record.thumbnailUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = platformIcon,
                                            contentDescription = null,
                                            tint = platformColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
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
                                            color = if (record.format == com.fabian.downloader.utils.Config.FORMAT_MP4) Color(0x112ECC71) else C_accentDim,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = record.format,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (record.format == com.fabian.downloader.utils.Config.FORMAT_MP4) C_green else C_accent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = if (record.isCompleted) stringResource(R.string.main_completed) else stringResource(R.string.main_in_progress),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = C_gray1
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.main_completed),
                                    tint = C_green,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
