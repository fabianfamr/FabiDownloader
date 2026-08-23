package com.fabian.downloader.ui.screens

import com.fabian.downloader.ui.components.AppIcons

import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.viewmodels.MainViewModel
import com.fabian.downloader.ui.components.getPlatformIconAndColor
import androidx.compose.ui.res.stringResource

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.fabian.downloader.R
import androidx.compose.ui.res.painterResource
import com.fabian.downloader.services.ExtractionService
import com.fabian.downloader.configs.Config
import com.fabian.downloader.ui.theme.*
import kotlinx.coroutines.*

data class DownloadOption(
    val id: String,
    val title: String,
    val format: String,
    val quality: String,
    val category: String,
    val sizeStr: String = ""
)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun extractUrl(text: String): String {
    val regex = Regex("""https?://[^\s]+""")
    val match = regex.find(text)
    return match?.value ?: text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePopupScreen(
    url: String,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onNavigateToDownloads: (() -> Unit)? = null
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fColors = LocalFabiColors.current
    val cleanUrl = remember(url) { extractUrl(url) }
    
    // Extraction states
    var title by remember { mutableStateOf<String?>(null) }
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    var formatSizes by remember { mutableStateOf<Map<String, Double>?>(null) }
    var platformInfoState by remember { mutableStateOf<Triple<String, String, String>?>(null) } // platformId, platformName, brandColorHex
    var extractedPlaylist by remember { mutableStateOf<com.fabian.downloader.services.ExtractionService.ExtractedPlaylist?>(null) }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDownloadStartedDialog by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }
    
    // Trigger metadata extraction when dialog opens
    LaunchedEffect(cleanUrl, retryTrigger) {
        if (cleanUrl.isEmpty()) {
            errorMsg = ctx.getString(R.string.share_error_empty)
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMsg = null
        
        val service = com.fabian.downloader.services.sites.SiteServiceProvider.getServiceForUrl(cleanUrl)
        platformInfoState = Triple(service.siteId, service.displayName, service.brandColorHex)
        
        val isLikelyPlaylist = cleanUrl.contains("playlist", ignoreCase = true) || 
                               cleanUrl.contains("list=", ignoreCase = true) ||
                               cleanUrl.contains("album", ignoreCase = true) ||
                               cleanUrl.contains("series", ignoreCase = true) ||
                               (cleanUrl.contains("tiktok.com/@") && !cleanUrl.contains("/video/"))

        // Try playlist extraction if URL looks like playlist or multi-post
        val jobPlaylist = launch {
            if (isLikelyPlaylist) {
                try {
                    val playlist = viewModel.extractPlaylist(cleanUrl)
                    if (playlist != null && playlist.items.isNotEmpty()) {
                        extractedPlaylist = playlist
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error extracting playlist", e)
                }
            }
        }

        // Parallel extraction with Kotlin Coroutines: Title, Thumbnail, Playlist, and Format Sizes
        // Fast Title & Thumbnail extraction in parallel
        val jobTitleAndIcon = launch(Dispatchers.IO) {
            try {
                val titleDeferred = async(Dispatchers.IO) { viewModel.extractTitle(cleanUrl) }
                val thumbDeferred = async(Dispatchers.IO) { viewModel.extractThumbnail(cleanUrl) }
                
                val extractedTitle = titleDeferred.await()
                val extractedThumb = thumbDeferred.await()
                
                withContext(Dispatchers.Main) {
                    title = extractedTitle
                    thumbnailUrl = extractedThumb
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error extracting title/icon in parallel", e)
            }
        }
        
        // Asynchronous non-blocking format sizes fetching
        val jobSizes = launch(Dispatchers.IO) {
            try {
                val extractedSizes = kotlinx.coroutines.withTimeoutOrNull(15000) {
                    viewModel.extractFormatSizes(cleanUrl)
                }
                withContext(Dispatchers.Main) {
                    formatSizes = extractedSizes ?: emptyMap()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error extracting sizes in background", e)
                withContext(Dispatchers.Main) {
                    formatSizes = emptyMap()
                }
            }
        }
        
        try {
            if (AppSettings.quickShareMode) {
                // Modo rápido: muestra los botones de descarga de inmediato sin bloquear la UI
                title = if (service.siteId == "generic") ctx.getString(R.string.share_direct_link) else ctx.getString(R.string.share_video_of, service.displayName)
                isLoading = false
            } else {
                // Espera no bloqueante breve para título/icono inicial, tamaños siguen en segundo plano
                kotlinx.coroutines.withTimeoutOrNull(800) {
                    if (isLikelyPlaylist) jobPlaylist.join()
                    jobTitleAndIcon.join()
                }
                if (title == null && extractedPlaylist == null) {
                    title = if (service.siteId == "generic") ctx.getString(R.string.share_direct_link) else ctx.getString(R.string.share_video_of, service.displayName)
                }
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error in extraction coroutines", e)
            errorMsg = ctx.getString(R.string.share_error_analyze)
            isLoading = false
        }
    }
    
    val currentPlatform = platformInfoState ?: Triple("generic", ctx.getString(R.string.share_direct_link), "#607D8B")
    val sizeText = remember(formatSizes) {
        if (formatSizes != null && formatSizes!!.isNotEmpty()) {
            val maxMb = formatSizes!!.values.maxOrNull() ?: 0.0
            if (maxMb > 0.0) ctx.getString(R.string.share_size_mb, maxMb) else ctx.getString(R.string.share_size_auto)
        } else {
            ctx.getString(R.string.share_size_auto)
        }
    }
    
    // Synthesize ExtractedVideo for backward compatibility with downstream UI components
    val extractedVideo = remember(title, thumbnailUrl, formatSizes, currentPlatform, sizeText) {
        val effectiveTitle = title ?: if (currentPlatform.first == "generic") ctx.getString(R.string.share_direct_link) else ctx.getString(R.string.share_video_of, currentPlatform.second)
        com.fabian.downloader.services.ExtractionService.ExtractedVideo(
            title = effectiveTitle,
            availableFormats = listOf(Config.FORMAT_MP4, Config.FORMAT_MP3, Config.FORMAT_M4A),
            size = sizeText,
            thumbnailUrl = thumbnailUrl,
            formatSizes = formatSizes ?: emptyMap(),
            platformId = currentPlatform.first,
            platformName = currentPlatform.second,
            brandColorHex = currentPlatform.third
        )
    }
    
    val musicOptions = remember(formatSizes) {
        listOf(
            DownloadOption("music_320", ctx.getString(R.string.share_quality_classic_mp3), Config.FORMAT_MP3, "320", ctx.getString(R.string.share_category_music)),
            DownloadOption("music_192", ctx.getString(R.string.share_quality_mp3_192), Config.FORMAT_MP3, "192", ctx.getString(R.string.share_category_music)),
            DownloadOption("music_128", ctx.getString(R.string.share_quality_mp3_128), Config.FORMAT_MP3, "128", ctx.getString(R.string.share_category_music)),
            DownloadOption("music_64", ctx.getString(R.string.share_quality_fast_m4a), Config.FORMAT_M4A, "64", ctx.getString(R.string.share_category_music))
        ).map { option ->
            option.copy(sizeStr = getOptionSize(ctx, option, formatSizes))
        }
    }
    
    val videoOptions = remember(formatSizes) {
        listOf(
            DownloadOption("video_1080", ctx.getString(R.string.share_quality_fhd_1080), Config.FORMAT_MP4, "1080p", ctx.getString(R.string.share_category_video)),
            DownloadOption("video_720", ctx.getString(R.string.share_quality_hq_720), Config.FORMAT_MP4, "720p", ctx.getString(R.string.share_category_video)),
            DownloadOption("video_480", ctx.getString(R.string.share_quality_std_480), Config.FORMAT_MP4, "480p", ctx.getString(R.string.share_category_video)),
            DownloadOption("video_360", ctx.getString(R.string.share_quality_fast_360), Config.FORMAT_MP4, "360p", ctx.getString(R.string.share_category_video))
        ).map { option ->
            option.copy(sizeStr = getOptionSize(ctx, option, formatSizes))
        }
    }
    
    var selectedOptionId by remember {
        mutableStateOf(
            if (cleanUrl.contains("music.youtube.com") || cleanUrl.contains("spotify") || cleanUrl.contains("soundcloud") || AppSettings.selectedQuality.contains("Solo Audio")) {
                when (AppSettings.defaultAudioBitrate) {
                    "320 kbps (Máxima)", "320 kbps" -> "music_320"
                    "256 kbps", "192 kbps" -> "music_192"
                    "128 kbps" -> "music_128"
                    else -> "music_320"
                }
            } else {
                when {
                    AppSettings.selectedQuality.contains("1080p") -> "video_1080"
                    AppSettings.selectedQuality.contains("720p") -> "video_720"
                    AppSettings.selectedQuality.contains("480p") -> "video_480"
                    AppSettings.selectedQuality.contains("360p") -> "video_360"
                    AppSettings.selectedQuality.contains("2160p") || AppSettings.selectedQuality.contains("4K") -> "video_1080"
                    else -> AppSettings.lastDownloadedOptionId.ifEmpty { "video_720" }
                }
            }
        )
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Platform-specific brand color and icon
    val platformInfo = remember(cleanUrl) {
        getPlatformIconAndColor(cleanUrl, Config.FORMAT_MP4)
    }
    val platformColor = platformInfo.second
    val platformIcon = platformInfo.first
    
    val handleClose: () -> Unit = remember(onClose) {
        {
            com.fabian.downloader.services.sites.BaseSiteService.cancelAllExtractions()
            onClose()
        }
    }

    ModalBottomSheet(
        onDismissRequest = handleClose,
        sheetState = sheetState,
        containerColor = fColors.sheet,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = fColors.border,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            
            // --- MAIN HEADER AREA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ctx.getString(R.string.share_options_title),
                    color = fColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                
                IconButton(
                    onClick = handleClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(fColors.cardSecondary, CircleShape)
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = ctx.getString(R.string.share_close),
                        tint = fColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- CONTENT SWITCHER (LOADING, ERROR, SUCCESS, PLAYLIST) ---
            Crossfade(
                targetState = Quadruple(isLoading, errorMsg, extractedPlaylist, extractedVideo),
                label = "PopupContentState"
            ) { state ->
                val (loading, error, playlist, video) = state
                
                when {
                    loading -> {
                        LoadingStateView(platformColor, cleanUrl)
                    }
                    error != null -> {
                        ErrorStateView(
                            errorMsg = error,
                            onRetry = {
                                isLoading = true
                                errorMsg = null
                                retryTrigger++
                            },
                            onQuickDownload = {
                                val allOptions = musicOptions + videoOptions
                                val selected = allOptions.find { it.id == selectedOptionId } ?: musicOptions.first()
                                AppSettings.lastDownloadedOptionId = selected.id
                                viewModel.downloadVideo(
                                    url = cleanUrl,
                                    quality = selected.quality,
                                    format = selected.format,
                                    title = title,
                                    thumbnailUrl = null
                                )
                                onClose()
                            }
                        )
                    }
                    playlist != null -> {
                        com.fabian.downloader.ui.components.PlaylistBatchView(
                            playlist = playlist,
                            onStartBatchDownload = { selectedItems, quality, format ->
                                viewModel.downloadBatch(selectedItems, quality, format)
                                showDownloadStartedDialog = true
                            }
                        )
                    }
                    else -> {
                        extractedVideo?.let { video ->
                            Column {
                            // Video Metadata Header Card
                            VideoMetadataHeader(video, platformIcon, platformColor, cleanUrl)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // --- MUSIC SECTION ---
                            Text(
                                text = ctx.getString(R.string.share_music_section),
                                color = Color(0xFF888888),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                musicOptions.forEach { option ->
                                    OptionListItem(
                                        option = option,
                                        icon = AppIcons.MusicNote,
                                        isSelected = selectedOptionId == option.id,
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        onClick = { selectedOptionId = option.id }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // --- VIDEO SECTION ---
                            Text(
                                text = ctx.getString(R.string.share_video_section),
                                color = Color(0xFF888888),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                videoOptions.forEach { option ->
                                    OptionListItem(
                                        option = option,
                                        icon = AppIcons.OndemandVideo,
                                        isSelected = selectedOptionId == option.id,
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        onClick = { selectedOptionId = option.id }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Download button
                            val allOptions = musicOptions + videoOptions
                            val isDownloadEnabled = allOptions.any { it.id == selectedOptionId }
                            Button(
                                onClick = {
                                    val selected = allOptions.find { it.id == selectedOptionId }
                                    if (selected != null) {
                                        AppSettings.lastDownloadedOptionId = selected.id
                                        viewModel.downloadVideo(
                                            url = cleanUrl,
                                            quality = selected.quality,
                                            format = selected.format,
                                            title = video.title,
                                            thumbnailUrl = video.thumbnailUrl
                                        )
                                        showDownloadStartedDialog = true
                                    }
                                },
                                enabled = isDownloadEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFF222226),
                                    disabledContentColor = Color.Gray
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = if (isDownloadEnabled) ctx.getString(R.string.share_download_button) else ctx.getString(R.string.share_select_option),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // Download started confirmation dialog
    if (showDownloadStartedDialog) {
        DownloadStartedDialog(
            onDismiss = {
                showDownloadStartedDialog = false
                onClose()
            },
            onViewDownloads = {
                showDownloadStartedDialog = false
                onClose()
                onNavigateToDownloads?.invoke()
            }
        )
    }
}

@Composable
fun SectionDivider(
    label: String,
    icon: ImageVector
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF8A8A96),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = Color(0xFF8A8A96),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF242428),
            thickness = 1.dp
        )
    }
}

@Composable
fun VideoMetadataHeader(
    video: ExtractionService.ExtractedVideo,
    platformIcon: ImageVector,
    platformColor: Color,
    cleanUrl: String = ""
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val domainName = remember(cleanUrl, video.platformName) {
        try {
            val host = java.net.URI(cleanUrl).host
            if (!host.isNullOrEmpty()) {
                host.removePrefix("www.").lowercase()
            } else {
                video.platformName.lowercase()
            }
        } catch (e: Exception) {
            video.platformName.lowercase()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail image
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E1E22)),
            contentAlignment = Alignment.Center
        ) {
            if (!video.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = ctx.getString(R.string.share_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Title & Domain
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = domainName,
                color = Color(0xFF888888),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OptionListItem(
    option: DownloadOption,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) accentColor else Color(0xFFA0A0A0),
            modifier = Modifier.size(22.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = option.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (option.sizeStr.isNotEmpty()) {
            Text(
                text = option.sizeStr,
                color = Color(0xFF888888),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        
        // Custom Radio Circle
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = if (isSelected) accentColor else Color.Transparent,
                    shape = CircleShape
                )
                .then(
                    if (!isSelected) Modifier.border(1.5.dp, Color(0xFF555555), CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingStateView(platformColor: Color, url: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "PulseEffect")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = platformColor,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = ctx.getString(R.string.share_analyzing),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer(alpha = pulseAlpha)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = url,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun ErrorStateView(
    errorMsg: String,
    onRetry: () -> Unit,
    onQuickDownload: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = AppIcons.ErrorOutline,
            contentDescription = ctx.getString(R.string.share_error),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = errorMsg,
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFF242428)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(ctx.getString(R.string.share_retry))
            }
            
            Button(
                onClick = onQuickDownload,
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text(ctx.getString(R.string.share_quick_download), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FormatRow(
    option: DownloadOption,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "rowBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "rowBorder"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "rowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBgColor)
            .border(1.dp, animatedBorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(22.dp)
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = option.title,
                color = if (isSelected) Color.White else Color.LightGray,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            
            if (option.sizeStr.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = ctx.getString(R.string.share_size),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    if (option.sizeStr == "X") {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(R.string.share_not_available),
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(11.dp)
                        )
                    } else {
                        Text(
                            text = option.sizeStr,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
                .then(
                    if (!isSelected) Modifier.border(1.5.dp, Color.Gray, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

fun getOptionSize(ctx: android.content.Context, option: DownloadOption, formatSizes: Map<String, Double>?): String {
    // Si aún está buscando o no hay tamaños, no mostrar nada (estilo Snaptube)
    if (formatSizes == null || formatSizes.isEmpty()) return ""
    
    val qKey = option.quality.lowercase() 
    val fKey = option.format.lowercase() 
    
    val sizeInMb = formatSizes[option.id] 
        ?: formatSizes[qKey]
        ?: formatSizes["${qKey}p"]
        ?: formatSizes["video_$qKey"]
        ?: formatSizes["video_${qKey}p"]
        ?: formatSizes["audio_$fKey"]
        ?: formatSizes.entries.find { it.key.contains(qKey, ignoreCase = true) }?.value
        ?: formatSizes.entries.find { it.key.contains(fKey, ignoreCase = true) }?.value
    
    if (sizeInMb != null && sizeInMb > 0.0) {
        return if (sizeInMb >= 1024.0) {
            String.format(java.util.Locale.US, "%.1f GB", sizeInMb / 1024.0)
        } else {
            ctx.getString(R.string.share_size_mb, sizeInMb)
        }
    }
    
    return ""
}

@Composable
fun DownloadStartedDialog(
    onDismiss: () -> Unit,
    onViewDownloads: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { alpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing)) }
            launch { scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Dim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha.value * 0.7f))
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Dialog card
            Surface(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { /* consume click */ },
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1A1A1E),
                border = BorderStroke(1.dp, Color(0xFF2A2A30))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing icon circle
                    val infiniteTransition = rememberInfiniteTransition(label = "glow")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.5f,
                        animationSpec = infiniteRepeatable(
                            tween(1200, easing = FastOutSlowInEasing),
                            RepeatMode.Reverse
                        ),
                        label = "glowAlpha"
                    )

                    Box(contentAlignment = Alignment.Center) {
                        // Outer glow ring
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(
                                    androidx.compose.ui.graphics.Color(0xFF00E5FF).copy(alpha = glowAlpha),
                                    CircleShape
                                )
                        )
                        // Inner icon circle
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.CloudDownload,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = ctx.getString(R.string.share_started_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = ctx.getString(R.string.share_started_subtitle),
                        color = Color(0xFF8A8A92),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ctx.getString(R.string.share_not_now) button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFF3A3A42)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFAAAAAA)
                            )
                        ) {
                            Text(
                                text = ctx.getString(R.string.share_not_now),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // "Ver descargas" button
                        Button(
                            onClick = onViewDownloads,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = ctx.getString(R.string.share_view),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}




