package com.fabian.downloader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudDownload
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class DownloadOption(
    val id: String,
    val title: String,
    val description: String? = null,
    val sizeStr: String,
    val format: String, // MP4, MP3, M4A
    val quality: String, // 360p, 720p, 1080p, etc.
    val badge: String? = null,
    val category: String // "Music" or "Video"
)

fun extractUrl(text: String): String {
    val regex = Regex("""https?://[^\s]+""")
    val match = regex.find(text)
    return match?.value ?: text
}

fun getDynamicOptionsForUrl(url: String, isMore: Boolean): List<DownloadOption> {
    val suffix = if (isMore) "more_" else ""
    
    if (isMore) {
        return listOf(
            DownloadOption(
                id = "${suffix}music_fast",
                title = "Audio rápido (M4A)",
                description = "M4A (128K), optimizado para reproducción móvil",
                sizeStr = "...",
                format = "M4A",
                quality = "128",
                badge = "Rápido",
                category = "Music"
            ),
            DownloadOption(
                id = "${suffix}music_classic_128",
                title = "Clásico MP3 (128K)",
                description = "Compatible con altavoces, vehículos, relojes, etc.",
                sizeStr = "...",
                format = "MP3",
                quality = "128",
                badge = null,
                category = "Music"
            ),
            DownloadOption(
                id = "${suffix}music_classic_320",
                title = "Alta Fidelidad MP3 (320K)",
                description = "Máxima calidad de audio, estéreo completo",
                sizeStr = "...",
                format = "MP3",
                quality = "320",
                badge = "HQ",
                category = "Music"
            ),
            DownloadOption(
                id = "${suffix}video_360",
                title = "Calidad Media (360p)",
                description = "Bajo consumo de datos y descarga rápida",
                sizeStr = "...",
                format = "MP4",
                quality = "360p",
                badge = null,
                category = "Video"
            ),
            DownloadOption(
                id = "${suffix}video_720",
                title = "Alta Definición (720p)",
                description = "Excelente resolución y nitidez estándar",
                sizeStr = "...",
                format = "MP4",
                quality = "720p",
                badge = "Recomendado",
                category = "Video"
            ),
            DownloadOption(
                id = "${suffix}video_1080",
                title = "Full HD (1080p)",
                description = "La mejor calidad visual para pantallas grandes",
                sizeStr = "...",
                format = "MP4",
                quality = "1080p",
                badge = "FHD",
                category = "Video"
            )
        )
    } else {
        return listOf(
            DownloadOption(
                id = "${suffix}music_fast",
                title = "Audio rápido (M4A)",
                description = null,
                sizeStr = "...",
                format = "M4A",
                quality = "128",
                badge = null,
                category = "Music"
            ),
            DownloadOption(
                id = "${suffix}music_classic",
                title = "Audio MP3",
                description = null,
                sizeStr = "...",
                format = "MP3",
                quality = "160",
                badge = null,
                category = "Music"
            ),
            DownloadOption(
                id = "${suffix}video_360",
                title = "Video rápido (360p)",
                description = null,
                sizeStr = "...",
                format = "MP4",
                quality = "360p",
                badge = null,
                category = "Video"
            ),
            DownloadOption(
                id = "${suffix}video_720",
                title = "Video HD (720p)",
                description = null,
                sizeStr = "...",
                format = "MP4",
                quality = "720p",
                badge = null,
                category = "Video"
            )
        )
    }
}

fun getDynamicOptionsFromExtraction(
    formatSizes: Map<String, Double>,
    isMore: Boolean
): List<DownloadOption> {
    val list = mutableListOf<DownloadOption>()
    val suffix = if (isMore) "more_" else ""

    // Las opciones de Audio siempre se agregan como base
    list.add(
        DownloadOption(
            id = "${suffix}music_fast",
            title = "Audio rápido (M4A)",
            description = "M4A (128K), optimizado para reproducción móvil",
            sizeStr = formatSizes["audio_m4a"]?.let { String.format(java.util.Locale.US, "%.1f MB", it) } ?: "Auto",
            format = "M4A",
            quality = "128",
            badge = "Rápido",
            category = "Music"
        )
    )
    
    val mp3ClassicSize = formatSizes["audio_mp3"]
    list.add(
        DownloadOption(
            id = "${suffix}music_classic_128",
            title = "Clásico MP3 (128K)",
            description = "Compatible con altavoces, vehículos, reproductores de audio",
            sizeStr = mp3ClassicSize?.let { String.format(java.util.Locale.US, "%.1f MB", it) } ?: "Auto",
            format = "MP3",
            quality = "128",
            badge = null,
            category = "Music"
        )
    )

    if (isMore) {
        list.add(
            DownloadOption(
                id = "${suffix}music_classic_320",
                title = "Alta Fidelidad MP3 (320K)",
                description = "Máxima calidad de audio, espectro completo de sonido",
                sizeStr = mp3ClassicSize?.let { String.format(java.util.Locale.US, "%.1f MB", it * 2.2) } ?: "Auto",
                format = "MP3",
                quality = "320",
                badge = "HQ",
                category = "Music"
            )
        )
    }

    // Extraer todas las alturas de video reales que existan en formatSizes
    val detectedHeights = formatSizes.keys
        .filter { it.startsWith("video_") && it.endsWith("p") }
        .mapNotNull { key ->
            key.substringAfter("video_").substringBefore("p").toIntOrNull()
        }
        .distinct()
        .sortedDescending() // De mayor a menor calidad

    var addedVideo = false
    if (detectedHeights.isNotEmpty()) {
        // En la vista normal, limitamos las opciones para no saturar
        val targetHeights = if (!isMore) {
            val subset = mutableListOf<Int>()
            val best = detectedHeights.firstOrNull { it <= 1080 } ?: detectedHeights.firstOrNull()
            if (best != null) subset.add(best)
            
            val middle = detectedHeights.find { it == 720 } ?: detectedHeights.find { it == 480 }
            if (middle != null && !subset.contains(middle)) subset.add(middle)
            
            val low = detectedHeights.find { it == 360 } ?: detectedHeights.find { it == 240 }
            if (low != null && !subset.contains(low)) subset.add(low)
            
            subset.sortedDescending()
        } else {
            detectedHeights
        }

        for (height in targetHeights) {
            val key = "video_${height}p"
            val sizeMb = formatSizes[key] ?: 0.0
            val sizeStr = if (sizeMb > 0.0) String.format(java.util.Locale.US, "%.1f MB", sizeMb) else "Auto"
            
            val (qualityName, badge) = when (height) {
                2160 -> Pair("2160p (4K)", "4K 🔥")
                1440 -> Pair("1440p (2K)", "2K ⭐")
                1080 -> Pair("1080p (Full HD)", "1080p ✨")
                720 -> Pair("720p (HD)", "720p")
                480 -> Pair("480p", "480p")
                360 -> Pair("360p", "360p")
                240 -> Pair("240p", "Baja")
                144 -> Pair("144p", "Ahorro")
                else -> Pair("${height}p", null)
            }

            list.add(
                DownloadOption(
                    id = "${suffix}video_$height",
                    title = "Video ($qualityName)",
                    description = "Resolución $qualityName en formato MP4 compatible",
                    sizeStr = sizeStr,
                    format = "MP4",
                    quality = "${height}p",
                    badge = badge,
                    category = "Video"
                )
            )
            addedVideo = true
        }
    }

    // Si por alguna razón de red o de parseo no hay formatos de video detectados, usar fallbacks compatibles
    if (!addedVideo) {
        list.add(
            DownloadOption(
                id = "${suffix}video_720",
                title = "Video HD (720p)",
                description = "Excelente resolución y nitidez estándar",
                sizeStr = "Auto",
                format = "MP4",
                quality = "720p",
                badge = "Recomendado",
                category = "Video"
            )
        )
        if (isMore) {
            list.add(
                DownloadOption(
                    id = "${suffix}video_1080",
                    title = "Video Full HD (1080p)",
                    description = "La mejor calidad visual para pantallas grandes",
                    sizeStr = "Auto",
                    format = "MP4",
                    quality = "1080p",
                    badge = "FHD",
                    category = "Video"
                )
            )
        }
        list.add(
            DownloadOption(
                id = "${suffix}video_360",
                title = "Video rápido (360p)",
                description = "Bajo consumo de datos y descarga rápida",
                sizeStr = "Auto",
                format = "MP4",
                quality = "360p",
                badge = null,
                category = "Video"
            )
        )
    }

    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePopupScreen(url: String, viewModel: MainViewModel, onClose: () -> Unit) {
    var isMoreFormats by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val cleanUrl = remember(url) { extractUrl(url) }
    val initialTitle = remember(url, cleanUrl) { 
        val title = url.replace(cleanUrl, "").trim()
        if (title.isNotEmpty()) title else "Analizando enlace compartido..." 
    }
    
    var videoTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var videoThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var isExtracting by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sourceDomain by remember { mutableStateOf("Enlace Externo") }
    
    var platformId by remember { mutableStateOf("generic") }
    var platformName by remember { mutableStateOf("Enlace Directo") }
    var brandColorHex by remember { mutableStateOf("#607D8B") }
    
    var defaultOptions by remember(cleanUrl) { mutableStateOf(getDynamicOptionsForUrl(cleanUrl, false)) }
    var moreOptions by remember(cleanUrl) { mutableStateOf(getDynamicOptionsForUrl(cleanUrl, true)) }
    var selectedOptionId by remember { mutableStateOf("video_720") }
    
    var selectedTab by remember { mutableStateOf("Video") } // "Video" or "Music"

    val platformColor = remember(brandColorHex) {
        try {
            Color(android.graphics.Color.parseColor(brandColorHex))
        } catch (e: Exception) {
            Color(0xFF2979FF)
        }
    }

    LaunchedEffect(cleanUrl) {
        isExtracting = true
        errorMessage = null
        videoThumbnailUrl = null
        
        try {
            val result = viewModel.extractVideoInfo(cleanUrl)
            if (result.title != "Video sin título" && result.title != "Desconocido") {
                videoTitle = result.title
            } else if (initialTitle == "Analizando enlace compartido...") {
                videoTitle = result.title
            }
            videoThumbnailUrl = result.thumbnailUrl
            platformId = result.platformId
            platformName = result.platformName
            brandColorHex = result.brandColorHex
            sourceDomain = result.platformName
            
            // Generar dinámicamente las opciones basadas en lo que realmente existe en el video
            defaultOptions = getDynamicOptionsFromExtraction(result.formatSizes, false)
            moreOptions = getDynamicOptionsFromExtraction(result.formatSizes, true)
            
            val allOptions = defaultOptions + moreOptions
            if (allOptions.none { it.id == selectedOptionId }) {
                val defaultVideo = defaultOptions.find { it.category == "Video" } ?: defaultOptions.firstOrNull()
                if (defaultVideo != null) {
                    selectedOptionId = defaultVideo.id
                    selectedTab = "Video"
                } else {
                    val firstOpt = defaultOptions.firstOrNull()
                    if (firstOpt != null) {
                        selectedOptionId = firstOpt.id
                        selectedTab = firstOpt.category
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error al conectar con el servicio: ${e.message}"
        } finally {
            isExtracting = false
        }
    }

    var isResolvingSize by remember { mutableStateOf(false) }

    val selectedOption = remember(selectedOptionId, defaultOptions, moreOptions) {
        val allOptions = defaultOptions + moreOptions
        allOptions.find { it.id == selectedOptionId } ?: defaultOptions.lastOrNull() ?: defaultOptions.first()
    }

    LaunchedEffect(selectedOptionId, cleanUrl, isExtracting) {
        if (cleanUrl.isNotEmpty() && !isExtracting) {
            isResolvingSize = true
            try {
                val (realSize, _) = viewModel.getRealSizeAndUrl(cleanUrl, selectedOption.quality, selectedOption.format)
                if (realSize != "Auto") {
                    defaultOptions = defaultOptions.map {
                        if (it.id == selectedOptionId) it.copy(sizeStr = realSize) else it
                    }
                    moreOptions = moreOptions.map {
                        if (it.id == selectedOptionId) it.copy(sizeStr = realSize) else it
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SharePopupScreen", "Failed to resolve real size", e)
            } finally {
                isResolvingSize = false
            }
        }
    }

    // Auto-align tab when option is selected programmatically
    LaunchedEffect(selectedOptionId) {
        val allOptions = defaultOptions + moreOptions
        val opt = allOptions.find { it.id == selectedOptionId }
        if (opt != null && opt.category != selectedTab) {
            selectedTab = opt.category
        }
    }

    // Auto-select first option of the category when changing tab manually
    LaunchedEffect(selectedTab) {
        val currentCategoryList = if (isMoreFormats) moreOptions else defaultOptions
        val filtered = currentCategoryList.filter { it.category == selectedTab }
        if (filtered.isNotEmpty() && filtered.none { it.id == selectedOptionId }) {
            selectedOptionId = filtered.first().id
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            if (showSuccessDialog) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp, start = 24.dp, end = 24.dp, top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Accent color matched dynamic background icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(platformColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.5.dp, platformColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = platformColor,
                                modifier = Modifier.fillMaxSize()
                            )
                            val arrowOffset = remember { Animatable(-32f) }
                            LaunchedEffect(Unit) {
                                delay(150)
                                arrowOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.48f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(y = arrowOffset.value.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¡Descarga en cola!",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "El servicio de $platformName ha recibido la solicitud con éxito.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // "Ahora no" Button
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Volver",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // "Ver" Button
                        val context = LocalContext.current
                        Button(
                            onClick = {
                                val intent = Intent(context, com.fabian.downloader.MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("navigate_to_downloads", true)
                                }
                                context.startActivity(intent)
                                onClose()
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = platformColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ver Descargas",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, start = 18.dp, end = 18.dp)
                ) {

                    // Header with app theme colors and title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Prepárate para Descargar",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isExtracting) "Analizando metadatos..." else "Configura tus preferencias",
                                fontSize = 13.sp,
                                color = if (isExtracting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isExtracting) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Dynamic Title Card displaying platform branding and video details
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Box {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(62.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(platformColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!videoThumbnailUrl.isNullOrEmpty()) {
                                        coil.compose.AsyncImage(
                                            model = videoThumbnailUrl,
                                            contentDescription = "Miniatura",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        val (platformIcon, _) = getPlatformIconAndColor(cleanUrl, selectedOption.format)
                                        Icon(
                                            imageVector = platformIcon,
                                            contentDescription = null,
                                            tint = platformColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    
                                    if (isExtracting) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (videoTitle == "Analizando enlace compartido...") "Obteniendo título..." else videoTitle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                    if (sourceDomain != "Enlace Externo") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = platformColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = sourceDomain,
                                                color = platformColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (isExtracting) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .height(2.dp),
                                    color = platformColor,
                                    trackColor = Color.Transparent
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Premium Segmented Switch Tab Row
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Video Tab Option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (selectedTab == "Video") platformColor else Color.Transparent)
                                        .clickable { selectedTab = "Video" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OndemandVideo,
                                            contentDescription = null,
                                            tint = if (selectedTab == "Video") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Videos",
                                            color = if (selectedTab == "Video") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Audio Tab Option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (selectedTab == "Music") platformColor else Color.Transparent)
                                        .clickable { selectedTab = "Music" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = null,
                                            tint = if (selectedTab == "Music") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Audio",
                                            color = if (selectedTab == "Music") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Formats list for the selected Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(weight = 1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val currentCategoryList = if (isMoreFormats) moreOptions else defaultOptions
                            val filteredOptions = currentCategoryList.filter { it.category == selectedTab }
                            
                            if (filteredOptions.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay formatos disponibles para esta categoría",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                filteredOptions.forEach { opt ->
                                    FormatRow(
                                        option = opt,
                                        isSelected = selectedOptionId == opt.id,
                                        onSelect = { selectedOptionId = opt.id }
                                    )
                                }
                            }

                            // Show more / high-quality formats switch button at the end of the list
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                onClick = { isMoreFormats = !isMoreFormats },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isMoreFormats) "Ocultar formatos avanzados" else "Mostrar todos los formatos (Full HD / Audio 320K)",
                                        color = platformColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Unified High-Fidelity Primary Action Button
                        Button(
                            onClick = {
                                if (cleanUrl.isNotEmpty()) {
                                    viewModel.downloadVideo(
                                        url = cleanUrl,
                                        quality = selectedOption.quality,
                                        format = selectedOption.format,
                                        title = videoTitle,
                                        thumbnailUrl = videoThumbnailUrl
                                    )
                                    showSuccessDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = platformColor,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isResolvingSize) "Calculando peso..." else "Descargar ${selectedOption.format} (${selectedOption.sizeStr})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun FormatRow(
    option: DownloadOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
    }

    Surface(
        onClick = onSelect,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (option.category == "Music") Icons.Default.Audiotrack else Icons.Default.OndemandVideo,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = option.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (option.badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = option.badge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                if (option.description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = option.sizeStr,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = option.format,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        width = if (isSelected) 0.dp else 2.dp,
                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
