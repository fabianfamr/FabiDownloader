package com.fabian.downloader.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import coil.compose.AsyncImage
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
fun SharePopupScreen(url: String, viewModel: MainViewModel, onClose: () -> Unit) {
    val cleanUrl = remember(url) { extractUrl(url) }
    
    // Extraction states
    var title by remember { mutableStateOf<String?>(null) }
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    var formatSizes by remember { mutableStateOf<Map<String, Double>?>(null) }
    var platformInfoState by remember { mutableStateOf<Triple<String, String, String>?>(null) } // platformId, platformName, brandColorHex
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // Trigger metadata extraction when dialog opens
    LaunchedEffect(cleanUrl) {
        if (cleanUrl.isEmpty()) {
            errorMsg = "El enlace compartido está vacío"
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMsg = null
        
        val service = com.fabian.downloader.services.sites.SiteServiceProvider.getServiceForUrl(cleanUrl)
        platformInfoState = Triple(service.siteId, service.displayName, service.brandColorHex)
        
        // Parallel extraction: Title/Icon (FAST) and Sizes (SLOW)
        kotlinx.coroutines.coroutineScope {
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
                    val extractedSizes = viewModel.extractFormatSizes(cleanUrl)
                    formatSizes = extractedSizes
                } catch (e: Exception) {
                    Log.e("SharePopupScreen", "Error extracting sizes", e)
                }
            }
            
            try {
                // Wait for Title and Icon to load so we can transition out of loading state quickly
                jobTitleAndIcon.join()
                if (title == null) {
                    title = if (service.siteId == "generic") "Enlace Directo" else "Video de ${service.displayName}"
                }
                isLoading = false
            } catch (e: Exception) {
                Log.e("SharePopupScreen", "Error in extraction coroutines", e)
                errorMsg = "No se pudo analizar el enlace. ¿Quieres intentar una descarga rápida?"
                isLoading = false
            }
        }
    }
    
    val currentPlatform = platformInfoState ?: Triple("generic", "Enlace Directo", "#607D8B")
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
            DownloadOption("music_fast", "M4A - Rápido", "M4A", "128", "Music"),
            DownloadOption("music_classic", "MP3 - Calidad Alta (320K)", "MP3", "320", "Music")
        ).map { option ->
            option.copy(sizeStr = getOptionSize(option, formatSizes))
        }
    }
    
    val videoOptions = remember(formatSizes) {
        listOf(
            DownloadOption("video_fast", "MP4 - Rápido (360p)", "MP4", "360p", "Video"),
            DownloadOption("video_hq", "MP4 - Calidad Alta (720p)", "MP4", "720p", "Video")
        ).map { option ->
            option.copy(sizeStr = getOptionSize(option, formatSizes))
        }
    }
    
    var selectedOptionId by remember { mutableStateOf("video_fast") }
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
                    text = "Opciones de Descarga",
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
                        contentDescription = "Cerrar",
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
                                    title = "Descarga_${System.currentTimeMillis() % 100000}",
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
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Options List
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF161619))
                                    .padding(16.dp)
                            ) {
                                // --- MUSIC SECTION ---
                                Text(
                                    text = "MÚSICA (AUDIO)",
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    musicOptions.forEach { option ->
                                        FormatRow(
                                            option = option,
                                            icon = Icons.Default.MusicNote,
                                            isSelected = selectedOptionId == option.id,
                                            onSelect = { selectedOptionId = option.id }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFF242428), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // --- VIDEO SECTION ---
                                Text(
                                    text = "VIDEO (MP4)",
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    videoOptions.forEach { option ->
                                        FormatRow(
                                            option = option,
                                            icon = Icons.Default.PlayCircleOutline,
                                            isSelected = selectedOptionId == option.id,
                                            onSelect = { selectedOptionId = option.id }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            // Download button
                            Button(
                                onClick = {
                                    val allOptions = musicOptions + videoOptions
                                    val selected = allOptions.find { it.id == selectedOptionId }
                                    if (selected != null) {
                                        viewModel.downloadVideo(
                                            url = cleanUrl,
                                            quality = selected.quality,
                                            format = selected.format,
                                            title = video.title,
                                            thumbnailUrl = video.thumbnailUrl
                                        )
                                        onClose()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary, // Premium Cyan
                                    contentColor = Color.Black
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Descargar",
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
}

@Composable
fun VideoMetadataHeader(
    video: ExtractionService.ExtractedVideo,
    platformIcon: ImageVector,
    platformColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161619))
            .border(1.dp, Color(0xFF242428), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail image or placeholder
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!video.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = "Miniatura del video",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = platformIcon,
                    contentDescription = null,
                    tint = platformColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Title & Source Info
        Column(
            modifier = Modifier.weight(1f)
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
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = platformIcon,
                    contentDescription = null,
                    tint = platformColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = video.platformName,
                    color = platformColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LoadingStateView(platformColor: Color, url: String) {
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
            text = "Analizando enlace...",
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
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
                Text("Reintentar")
            }
            
            Button(
                onClick = onQuickDownload,
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text("Descarga Rápida", fontWeight = FontWeight.Bold)
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
                Text(
                    text = "Tamaño: ${option.sizeStr}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
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
                    imageVector = Icons.Default.Check,
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
    if (formatSizes.isEmpty()) return "Auto"
    
    val qKey = option.quality.lowercase()
    val fKey = option.format.lowercase()
    
    val sizeInMb = formatSizes[qKey] 
        ?: formatSizes["${qKey}p"]
        ?: formatSizes[option.id]
        ?: formatSizes.entries.find { it.key.lowercase().contains(qKey) }?.value
        ?: formatSizes.entries.find { it.key.lowercase().contains(fKey) }?.value
        
    return if (sizeInMb != null && sizeInMb > 0.0) {
        String.format(java.util.Locale.US, "%.1f MB", sizeInMb)
    } else {
        "Auto"
    }
}
