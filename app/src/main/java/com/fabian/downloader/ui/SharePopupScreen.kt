package com.fabian.downloader.ui
import androidx.compose.ui.res.stringResource

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlinx.coroutines.launch

data class DownloadOption(
    val id: String,
    val title: String,
    val format: String,
    val quality: String,
    val category: String,
    val sizeStr: String = ""
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
    val cleanUrl = remember(url) { extractUrl(url) }
    
    // Extraction states
    var title by remember { mutableStateOf<String?>(null) }
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    var formatSizes by remember { mutableStateOf<Map<String, Double>?>(null) }
    var platformInfoState by remember { mutableStateOf<Triple<String, String, String>?>(null) } // platformId, platformName, brandColorHex
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDownloadStartedDialog by remember { mutableStateOf(false) }
    
    // Trigger metadata extraction when dialog opens
    LaunchedEffect(cleanUrl) {
        if (cleanUrl.isEmpty()) {
            errorMsg = ctx.getString(R.string.share_error_empty)
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMsg = null
        
        val service = com.fabian.downloader.services.sites.SiteServiceProvider.getServiceForUrl(cleanUrl)
        platformInfoState = Triple(service.siteId, service.displayName, service.brandColorHex)
        
        // Parallel extraction: Title/Icon (FAST) and Sizes (SLOW)
        val jobTitleAndIcon = launch {
            try {
                val extractedTitle = viewModel.extractTitle(cleanUrl)
                val extractedThumb = viewModel.extractThumbnail(cleanUrl)
                title = extractedTitle
                thumbnailUrl = extractedThumb
            } catch (e: Exception) {
                Log.e("SharePopupScreen", "Error extracting title/icon", e)
            }
        }
        
        val jobSizes = launch {
            try {
                val extractedSizes = kotlinx.coroutines.withTimeoutOrNull(15000) {
                    viewModel.extractFormatSizes(cleanUrl)
                }
                formatSizes = extractedSizes ?: emptyMap()
            } catch (e: Exception) {
                Log.e("SharePopupScreen", "Error extracting sizes", e)
                formatSizes = emptyMap()
            }
        }
        
        try {
            // Wait for Title and Icon to load so we can transition out of loading state quickly
            jobTitleAndIcon.join()
            if (title == null) {
                title = if (service.siteId == "generic") ctx.getString(R.string.share_direct_link) else ctx.getString(R.string.share_video_of, service.displayName)
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("SharePopupScreen", "Error in extraction coroutines", e)
            errorMsg = ctx.getString(R.string.share_error_analyze)
            isLoading = false
        }
    }
    
    val currentPlatform = platformInfoState ?: Triple("generic", ctx.getString(R.string.share_direct_link), "#607D8B")
    val sizeText = remember(formatSizes) {
        if (formatSizes != null && formatSizes!!.isNotEmpty()) {
            val maxMb = formatSizes!!.values.maxOrNull() ?: 0.0
            if (maxMb > 0.0) String.format(java.util.Locale.US, "%.1f MB", maxMb) else "Auto"
        } else {
            "Auto"
        }
    }
    
    // Synthesize ExtractedVideo for backward compatibility with downstream UI components
    val extractedVideo = remember(title, thumbnailUrl, formatSizes, currentPlatform, sizeText) {
        if (title != null) {
            com.fabian.downloader.services.ExtractionService.ExtractedVideo(
                title = title ?: "",
                availableFormats = listOf("MP4", "MP3", "M4A"),
                size = sizeText,
                thumbnailUrl = thumbnailUrl,
                formatSizes = formatSizes ?: emptyMap(),
                platformId = currentPlatform.first,
                platformName = currentPlatform.second,
                brandColorHex = currentPlatform.third
            )
        } else null
    }
    
    val musicOptions = remember(formatSizes) {
        listOf(
            DownloadOption("music_320", "320 kbps", "MP3", "320", "Music"),
            DownloadOption("music_192", "192 kbps", "MP3", "192", "Music"),
            DownloadOption("music_128", "128 kbps", "MP3", "128", "Music"),
            DownloadOption("music_64", "64 kbps", "M4A", "64", "Music")
        ).map { option ->
            option.copy(sizeStr = getOptionSize(option, formatSizes))
        }
    }
    
    val videoOptions = remember(formatSizes) {
        listOf(
            DownloadOption("video_1080", "1080p FHD", "MP4", "1080p", "Video"),
            DownloadOption("video_720", "720p HD", "MP4", "720p", "Video"),
            DownloadOption("video_480", "480p", "MP4", "480p", "Video"),
            DownloadOption("video_360", "360p", "MP4", "360p", "Video")
        ).map { option ->
            option.copy(sizeStr = getOptionSize(option, formatSizes))
        }
    }
    
    var selectedOptionId by remember { mutableStateOf(AppSettings.lastDownloadedOptionId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Platform-specific brand color and icon
    val platformInfo = remember(cleanUrl) {
        getPlatformIconAndColor(cleanUrl, "MP4")
    }
    val platformColor = platformInfo.second
    val platformIcon = platformInfo.first
    
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF0C0C0E), // Match app's pitch black/very dark charcoal surface
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF242428),
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
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF161619), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = ctx.getString(R.string.share_close),
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- CONTENT SWITCHER (LOADING, ERROR, SUCCESS) ---
            Crossfade(
                targetState = Triple(isLoading, errorMsg, extractedVideo),
                label = "PopupContentState"
            ) { state ->
                val (loading, error, video) = state
                
                when {
                    loading -> {
                        LoadingStateView(platformColor, cleanUrl)
                    }
                    error != null -> {
                        ErrorStateView(
                            errorMsg = error,
                            onRetry = {
                                // Trigger retry by reloading
                                isLoading = true
                                errorMsg = null
                                kotlin.concurrent.thread {
                                    try {
                                        // Simple run block inside Thread to trigger launched effect re-run
                                        errorMsg = null
                                    } catch(e: Exception) {}
                                }
                            },
                            onQuickDownload = {
                                // Fallback to immediate download using general placeholders
                                viewModel.downloadVideo(
                                    url = cleanUrl,
                                    quality = if (selectedOptionId.startsWith("music")) {
                                        if (selectedOptionId == "music_classic") "320" else "128"
                                    } else {
                                        if (selectedOptionId == "video_hq") "720p" else "360p"
                                    },
                                    format = if (selectedOptionId.startsWith("music")) {
                                        if (selectedOptionId == "music_classic") "MP3" else "M4A"
                                    } else "MP4",
                                    title = ctx.getString(R.string.share_download_prefix, (System.currentTimeMillis() % 100000).toString()),
                                    thumbnailUrl = null
                                )
                                onClose()
                            }
                        )
                    }
                    video != null -> {
                        Column {
                            // Video Metadata Header Card
                            VideoMetadataHeader(video, platformIcon, platformColor)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // --- MUSIC SECTION ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ctx.getString(R.string.share_music_section),
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFF242428),
                                    thickness = 1.dp
                                )
                            }
                            
                            // Grid of Music Options (2 columns)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                musicOptions.chunked(2).forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        chunk.forEach { option ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                SnaptubeFormatItem(
                                                    option = option,
                                                    isSelected = selectedOptionId == option.id,
                                                    accentColor = MaterialTheme.colorScheme.primary,
                                                    onClick = {
                                                        selectedOptionId = option.id
                                                    }
                                                )
                                            }
                                        }
                                        if (chunk.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            // --- VIDEO SECTION ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ctx.getString(R.string.share_video_section),
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFF242428),
                                    thickness = 1.dp
                                )
                            }
                            
                            // Grid of Video Options (2 columns)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                videoOptions.chunked(2).forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        chunk.forEach { option ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                SnaptubeFormatItem(
                                                    option = option,
                                                    isSelected = selectedOptionId == option.id,
                                                    accentColor = MaterialTheme.colorScheme.primary,
                                                    onClick = {
                                                        selectedOptionId = option.id
                                                    }
                                                )
                                            }
                                        }
                                        if (chunk.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Download button
                            val isDownloadEnabled = (musicOptions + videoOptions).any { it.id == selectedOptionId }
                            Button(
                                onClick = {
                                    val allOptions = musicOptions + videoOptions
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
                                    .heightIn(min = 54.dp),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFF161619),
                                    disabledContentColor = Color.Gray
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (isDownloadEnabled) Color.Black else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDownloadEnabled) ctx.getString(R.string.share_download_button) else ctx.getString(R.string.share_select_option),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
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
    platformColor: Color
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161619))
            .border(1.5.dp, Color(0xFF242428), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail image or placeholder
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E22)),
            contentAlignment = Alignment.Center
        ) {
            if (!video.thumbnailUrl.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = ctx.getString(R.string.share_thumbnail),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Decorative semi-transparent overlay with a small play icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
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
        
        // Title & Source Info
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
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = platformIcon,
                    contentDescription = null,
                    tint = if (video.platformName.lowercase().contains("youtube")) Color(0xFFFF0000) else platformColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = video.platformName,
                    color = Color(0xFF8A8A96),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
            imageVector = Icons.Default.ErrorOutline,
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
                            contentDescription = "No disponible",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(11.dp)
                        )
                    } else if (option.sizeStr == "...") {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 1.2f.dp,
                            modifier = Modifier.size(10.dp)
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

fun getOptionSize(option: DownloadOption, formatSizes: Map<String, Double>?): String {
    if (formatSizes == null) return "..."
    
    val qKey = option.quality.lowercase()
    val fKey = option.format.lowercase()
    
    var sizeInMb = if (formatSizes.isEmpty()) null else {
        formatSizes[qKey] 
            ?: formatSizes["${qKey}p"]
            ?: formatSizes[option.id]
            ?: formatSizes.entries.find { it.key.lowercase().contains(qKey) }?.value
            ?: formatSizes.entries.find { it.key.lowercase().contains(fKey) }?.value
    }
    
    // Proporcionar una estimación realista por defecto si no se encuentra o el mapa está vacío
    if (sizeInMb == null || sizeInMb <= 0.0) {
        sizeInMb = when (option.id) {
            "music_320" -> 8.5
            "music_192" -> 5.2
            "music_128" -> 3.5
            "music_64" -> 1.8
            "video_1080" -> 45.0
            "video_720" -> 25.0
            "video_480" -> 12.0
            "video_360" -> 7.5
            else -> 15.0
        }
    }
    
    return String.format(java.util.Locale.US, "%.1f MB", sizeInMb)
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
                                imageVector = Icons.Default.CloudDownload,
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
                                ctx.getString(R.string.share_not_now),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
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
                                ctx.getString(R.string.share_view),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SnaptubeFormatItem(
    option: DownloadOption,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0x1400E5FF) else Color(0xFF161619)
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isSelected) accentColor else Color(0xFF242428)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = option.title,
                    color = if (isSelected) accentColor else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val sizeLabel = if (option.sizeStr.isEmpty()) "X" else option.sizeStr
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = option.format,
                        color = Color(0xFF8A8A96),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF4A4A56),
                        fontSize = 10.sp
                    )
                    if (sizeLabel == "...") {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 1.2f.dp,
                            modifier = Modifier.size(10.dp)
                        )
                    } else {
                        Text(
                            text = sizeLabel,
                            color = if (sizeLabel == "X") Color(0xFFEF5350) else Color(0xFF8A8A96),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Selected checkmark badge in top-right corner
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, end = 6.dp)
                    .size(18.dp)
                    .background(accentColor, CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF0A0A0C),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}


