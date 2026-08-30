package com.fabian.downloader.ui.screens

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.services.ExtractionService
import com.fabian.downloader.services.sites.BaseSiteService
import com.fabian.downloader.services.sites.SiteServiceProvider
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.PlaylistBatchView
import com.fabian.downloader.ui.components.getPlatformIconAndColor
import com.fabian.downloader.ui.theme.LocalFabiColors
import com.fabian.downloader.ui.viewmodels.MainViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    val ctx = LocalContext.current
    val fColors = LocalFabiColors.current
    val cleanUrl = remember(url) { extractUrl(url) }

    var title by remember { mutableStateOf<String?>(null) }
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }
    var formatSizes by remember { mutableStateOf<Map<String, Double>?>(null) }
    var platformInfoState by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var extractedPlaylist by remember { mutableStateOf<ExtractionService.ExtractedPlaylist?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDownloadStartedDialog by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(cleanUrl, retryTrigger) {
        if (cleanUrl.isEmpty()) {
            errorMsg = ctx.getString(R.string.share_error_empty)
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMsg = null

        val service = SiteServiceProvider.getServiceForUrl(cleanUrl)
        platformInfoState = Triple(service.siteId, service.displayName, service.brandColorHex)

        val isLikelyPlaylist = cleanUrl.contains("playlist", ignoreCase = true) || 
                               cleanUrl.contains("list=", ignoreCase = true) ||
                               cleanUrl.contains("album", ignoreCase = true) ||
                               cleanUrl.contains("series", ignoreCase = true) ||
                               (cleanUrl.contains("tiktok.com/@") && !cleanUrl.contains("/video/"))

        val jobPlaylist = launch {
            if (isLikelyPlaylist) {
                try {
                    val playlist = viewModel.extractPlaylist(cleanUrl)
                    if (playlist != null && playlist.items.isNotEmpty()) {
                        extractedPlaylist = playlist
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error extracting playlist", e)
                }
            }
        }

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
                if (e is CancellationException) throw e
                Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error extracting title/icon", e)
            }
        }

        val jobSizes = launch(Dispatchers.IO) {
            try {
                val extractedSizes = withTimeoutOrNull(15000) { viewModel.extractFormatSizes(cleanUrl) }
                withContext(Dispatchers.Main) { formatSizes = extractedSizes ?: emptyMap() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error extracting sizes", e)
                withContext(Dispatchers.Main) { formatSizes = emptyMap() }
            }
        }

        try {
            if (AppSettings.quickShareMode) {
                title = if (service.siteId == "generic") ctx.getString(R.string.share_direct_link) else ctx.getString(R.string.share_video_of, service.displayName)
                isLoading = false
            } else {
                withTimeoutOrNull(800) {
                    if (isLikelyPlaylist) jobPlaylist.join()
                    jobTitleAndIcon.join()
                }
                if (title == null && extractedPlaylist == null) {
                    title = if (service.siteId == "generic") ctx.getString(R.string.share_direct_link) else ctx.getString(R.string.share_video_of, service.displayName)
                }
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_SHARE_POPUP_SCREEN, "Error in extraction", e)
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

    val extractedVideo = remember(title, thumbnailUrl, formatSizes, currentPlatform, sizeText) {
        val effectiveTitle = title ?: if (currentPlatform.first == "generic") ctx.getString(R.string.share_direct_link) else ctx.getString(R.string.share_video_of, currentPlatform.second)
        ExtractionService.ExtractedVideo(
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
        ).map { it.copy(sizeStr = getOptionSize(ctx, it, formatSizes)) }
    }

    val videoOptions = remember(formatSizes) {
        listOf(
            DownloadOption("video_1080", ctx.getString(R.string.share_quality_fhd_1080), Config.FORMAT_MP4, "1080p", ctx.getString(R.string.share_category_video)),
            DownloadOption("video_720", ctx.getString(R.string.share_quality_hq_720), Config.FORMAT_MP4, "720p", ctx.getString(R.string.share_category_video)),
            DownloadOption("video_480", ctx.getString(R.string.share_quality_std_480), Config.FORMAT_MP4, "480p", ctx.getString(R.string.share_category_video)),
            DownloadOption("video_360", ctx.getString(R.string.share_quality_fast_360), Config.FORMAT_MP4, "360p", ctx.getString(R.string.share_category_video))
        ).map { it.copy(sizeStr = getOptionSize(ctx, it, formatSizes)) }
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
    val platformInfo = remember(cleanUrl) { getPlatformIconAndColor(cleanUrl, Config.FORMAT_MP4) }
    val platformColor = platformInfo.second
    val platformIcon = platformInfo.first

    val handleClose: () -> Unit = remember(onClose) {
        {
            BaseSiteService.cancelAllExtractions()
            onClose()
        }
    }

    ModalBottomSheet(
        onDismissRequest = handleClose,
        sheetState = sheetState,
        containerColor = fColors.sheet,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = fColors.border, modifier = Modifier.padding(top = 12.dp)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                    modifier = Modifier.size(36.dp).background(fColors.cardSecondary, CircleShape)
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

            Crossfade(
                targetState = Quadruple(isLoading, errorMsg, extractedPlaylist, extractedVideo),
                label = "PopupContentState"
            ) { state ->
                val (loading, error, playlist, video) = state
                when {
                    loading -> LoadingStateView(platformColor, cleanUrl)
                    error != null -> ErrorStateView(
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
                            viewModel.downloadVideo(url = cleanUrl, quality = selected.quality, format = selected.format, title = title, thumbnailUrl = null)
                            onClose()
                        }
                    )
                    playlist != null -> PlaylistBatchView(
                        playlist = playlist,
                        onStartBatchDownload = { selectedItems, quality, format ->
                            viewModel.downloadBatch(selectedItems, quality, format)
                            showDownloadStartedDialog = true
                        }
                    )
                    else -> {
                        extractedVideo.let { vid ->
                            Column {
                                VideoMetadataHeader(vid, platformIcon, platformColor, cleanUrl)
                                Spacer(modifier = Modifier.height(16.dp))
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
                                                title = vid.title,
                                                thumbnailUrl = vid.thumbnailUrl
                                            )
                                            showDownloadStartedDialog = true
                                        }
                                    },
                                    enabled = isDownloadEnabled,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
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
