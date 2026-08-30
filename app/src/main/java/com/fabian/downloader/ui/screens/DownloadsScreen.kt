package com.fabian.downloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.RealtimeSpeedCardBanner
import com.fabian.downloader.ui.components.SpeedSliderDialog
import com.fabian.downloader.ui.theme.fabiColors
import com.fabian.downloader.ui.viewmodels.DownloadsViewModel
import com.fabian.downloader.ui.viewmodels.MainViewModel
import com.fabian.downloader.utils.PathUtils
import com.fabian.downloader.utils.ToastUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    database: AppDatabase,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onNavigateToSettings: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val viewModel: DownloadsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DownloadsViewModel(database) as T
            }
        }
    )
    val mainViewModel: MainViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(ctx.applicationContext as android.app.Application, database) as T
            }
        }
    )

    val downloads by viewModel.downloads.collectAsStateWithLifecycle(initialValue = emptyList())
    val sortDateStr = stringResource(R.string.downloads_sort_date)
    val sortNameStr = stringResource(R.string.downloads_sort_name)
    val sortSizeStr = stringResource(R.string.downloads_sort_size)
    var sortOrder by remember { mutableStateOf(sortDateStr) }

    val downloading = remember(downloads) { downloads.filter { !it.isCompleted } }
    val completed = remember(downloads, sortOrder, sortDateStr, sortNameStr, sortSizeStr) {
        val list = downloads.filter { it.isCompleted }
        when (sortOrder) {
            sortDateStr -> list.sortedByDescending { it.timestamp }
            sortNameStr -> list.sortedBy { it.title }
            sortSizeStr -> list.sortedByDescending { rec ->
                val s = rec.size.uppercase().trim()
                val num = Regex("""[\d.]+""").find(s)?.value?.toDoubleOrNull() ?: 0.0
                when {
                    s.contains("GB") -> (num * 1024 * 1024 * 1024).toLong()
                    s.contains("MB") -> (num * 1024 * 1024).toLong()
                    s.contains("KB") -> (num * 1024).toLong()
                    s.contains("B") -> num.toLong()
                    else -> 0L
                }
            }
            else -> list
        }
    }

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var itemToDelete by remember { mutableStateOf<Long?>(null) }
    var menuRecord by remember { mutableStateOf<DownloadRecord?>(null) }
    var recordToConvert by remember { mutableStateOf<DownloadRecord?>(null) }
    var errorToShow by remember { mutableStateOf<String?>(null) }
    var showSpeedSliderDialog by remember { mutableStateOf(false) }

    val toggleSelection: (Long) -> Unit = remember(selectedIds) { { id ->
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    } }

    val onShareFile: (DownloadRecord) -> Unit = remember(ctx) { { record ->
        try {
            val file = PathUtils.getDownloadFile(ctx, record.title, record.id, record.format)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(ctx, "com.fabian.downloader.fileprovider", file)
                val mimeType = when (record.format.uppercase()) {
                    Config.FORMAT_MP4, Config.FORMAT_WEBM -> Config.MIME_VIDEO
                    Config.FORMAT_JPG, Config.FORMAT_PNG, Config.FORMAT_WEBP, "JPEG" -> Config.MIME_IMAGE
                    else -> Config.MIME_AUDIO
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = mimeType
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, ctx.getString(R.string.downloads_share_title)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(chooser)
            } else {
                ToastUtils.showShort(ctx, R.string.main_error_file_not_found)
            }
        } catch (e: Exception) {
            ToastUtils.showShort(ctx, R.string.downloads_share_error_prefix, e.localizedMessage ?: "")
        }
    } }

    val openFile: (DownloadRecord) -> Unit = remember(ctx) { { record ->
        try {
            val file = PathUtils.getDownloadFile(ctx, record.title, record.id, record.format)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(ctx, "com.fabian.downloader.fileprovider", file)
                val mimeType = when (record.format.uppercase()) {
                    Config.FORMAT_MP4, Config.FORMAT_WEBM -> Config.MIME_VIDEO
                    Config.FORMAT_JPG, Config.FORMAT_PNG, Config.FORMAT_WEBP, "JPEG" -> Config.MIME_IMAGE
                    else -> Config.MIME_AUDIO
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            } else {
                ToastUtils.showShort(ctx, R.string.main_error_file_not_found)
            }
        } catch (e: Exception) {
            ToastUtils.showShort(ctx, R.string.main_error_opening_file, e.localizedMessage ?: "")
        }
    } }

    val shareSelectedFiles: () -> Unit = {
        try {
            val uris = ArrayList<Uri>()
            selectedIds.forEach { id ->
                val record = downloads.find { it.id == id }
                if (record != null) {
                    val file = PathUtils.getDownloadFile(ctx, record.title, record.id, record.format)
                    if (file.exists()) {
                        uris.add(FileProvider.getUriForFile(ctx, "com.fabian.downloader.fileprovider", file))
                    }
                }
            }
            if (uris.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = "*/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.downloads_share_title)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } else {
                ToastUtils.showShort(ctx, R.string.downloads_share_empty)
            }
        } catch (e: Exception) {
            ToastUtils.showShort(ctx, R.string.downloads_share_error, e.localizedMessage ?: "")
        }
    }

    val fColors = MaterialTheme.fabiColors
    val tabs = listOf(stringResource(R.string.downloads_tab_completed), ctx.getString(R.string.downloads_tab_progress))
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialPage) {
        if (initialPage in 0 until tabs.size) {
            pagerState.scrollToPage(initialPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fColors.background)
    ) {
        if (isSelectionMode) {
            DownloadsSelectionBar(
                selectedCount = selectedIds.size,
                onCancelSelection = { selectedIds = emptySet() },
                onShareSelected = shareSelectedFiles,
                onDeleteSelected = {
                    selectedIds.forEach { viewModel.deleteDownload(it) }
                    selectedIds = emptySet()
                },
                accentDimColor = fColors.accentDim,
                accentGlowColor = fColors.accentGlow,
                accentColor = fColors.accent,
                textColor = fColors.textPrimary,
                errorColor = fColors.error
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.downloads_library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = fColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (pagerState.currentPage == 0) {
                    IconButton(
                        onClick = { viewModel.clearCompletedDownloads() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(fColors.cardSecondary, CircleShape)
                    ) {
                        Icon(AppIcons.DeleteSweep, stringResource(R.string.downloads_clear_history), tint = fColors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                var expanded by remember { mutableStateOf(false) }
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(fColors.cardSecondary, CircleShape)
                    ) {
                        Icon(AppIcons.Sort, stringResource(R.string.downloads_sort), tint = fColors.textSecondary)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(fColors.card)
                    ) {
                        listOf(sortDateStr, sortNameStr, sortSizeStr).forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        fontWeight = if (sortOrder == option) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sortOrder == option) fColors.accent else fColors.textPrimary
                                    )
                                },
                                onClick = {
                                    sortOrder = option
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (sortOrder == option) {
                                        Icon(AppIcons.Check, null, tint = fColors.accent, modifier = Modifier.size(16.dp))
                                    } else {
                                        Spacer(Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = fColors.card,
            border = BorderStroke(1.dp, fColors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    val animatedBgAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(250, easing = FastOutSlowInEasing),
                        label = "tabBg"
                    )
                    Surface(
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = fColors.accent.copy(alpha = animatedBgAlpha * 0.15f),
                        border = if (isSelected) BorderStroke(1.dp, fColors.accentGlow) else null,
                        contentColor = if (isSelected) fColors.accent else fColors.textSecondary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        var filterType by remember { mutableStateOf(ctx.getString(R.string.downloads_filter_all)) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                stringResource(R.string.downloads_filter_all),
                stringResource(R.string.downloads_filter_music),
                stringResource(R.string.downloads_filter_video)
            ).forEach { type ->
                val isSelected = filterType == type
                Surface(
                    onClick = { filterType = type },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) fColors.accentDim else fColors.cardSecondary,
                    border = if (isSelected) BorderStroke(1.dp, fColors.accent) else BorderStroke(1.dp, fColors.border),
                    modifier = Modifier.height(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = type,
                            color = if (isSelected) fColors.accent else fColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Top
        ) { page ->
            val filteredCompleted = remember(completed, filterType) {
                when (filterType) {
                    ctx.getString(R.string.downloads_filter_music) -> completed.filter { 
                        it.format.equals(Config.FORMAT_MP3, true) || 
                        it.format.equals(Config.FORMAT_M4A, true) || 
                        it.format.equals(Config.FORMAT_OGG, true) || 
                        it.format.equals(Config.FORMAT_WAV, true) 
                    }
                    ctx.getString(R.string.downloads_filter_video) -> completed.filter { 
                        it.format.equals(Config.FORMAT_MP4, true) || 
                        it.format.equals(Config.FORMAT_WEBM, true) || 
                        it.format.equals("MKV", true) 
                    }
                    else -> completed
                }
            }
            val filteredDownloading = remember(downloading, filterType) {
                when (filterType) {
                    ctx.getString(R.string.downloads_filter_music) -> downloading.filter { 
                        it.format.equals(Config.FORMAT_MP3, true) || 
                        it.format.equals(Config.FORMAT_M4A, true) || 
                        it.format.equals(Config.FORMAT_OGG, true) || 
                        it.format.equals(Config.FORMAT_WAV, true) 
                    }
                    ctx.getString(R.string.downloads_filter_video) -> downloading.filter { 
                        it.format.equals(Config.FORMAT_MP4, true) || 
                        it.format.equals(Config.FORMAT_WEBM, true) || 
                        it.format.equals("MKV", true) 
                    }
                    else -> downloading
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (AppSettings.cardStyle == "Minimalista") 8.dp else 14.dp)
            ) {
                if (page == 0) {
                    if (filteredCompleted.isNotEmpty()) {
                        items(filteredCompleted, key = { it.id }) { record ->
                            MobileDownloadedItem(
                                record = record, 
                                onPlay = { 
                                    if (isSelectionMode) toggleSelection(record.id) else openFile(record) 
                                }, 
                                onDelete = { menuRecord = record },
                                onShare = { onShareFile(record) },
                                isSelected = selectedIds.contains(record.id),
                                isSelectionMode = isSelectionMode,
                                onLongPress = { toggleSelection(record.id) }
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(bottom = 60.dp), 
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(fColors.cardSecondary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AppIcons.FolderOpen, contentDescription = null, tint = fColors.textSecondary, modifier = Modifier.size(36.dp))
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(stringResource(R.string.downloads_empty_completed_title), color = fColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.downloads_empty_completed_subtitle), color = fColors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    if (AppSettings.showRealtimeSpeedCard) {
                        item {
                            RealtimeSpeedCardBanner(
                                activeDownloads = downloading,
                                accentColor = fColors.accent,
                                cardBg = fColors.card,
                                card2Bg = fColors.cardSecondary,
                                borderColor = fColors.border,
                                textColor = fColors.textPrimary,
                                grayColor = fColors.textSecondary,
                                onClick = { showSpeedSliderDialog = true }
                            )
                        }
                    }
                    if (filteredDownloading.isNotEmpty()) {
                        @OptIn(ExperimentalFoundationApi::class)
                        stickyHeader {
                            val anyActive = filteredDownloading.any { !it.isPaused && it.speed != "FAILED" }
                            Surface(modifier = Modifier.fillMaxWidth(), color = fColors.background) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (anyActive) {
                                                filteredDownloading.forEach { if (!it.isPaused) viewModel.pauseDownload(it.id) }
                                            } else {
                                                filteredDownloading.forEach { if (it.isPaused) viewModel.resumeDownload(it.id) }
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .border(1.5.dp, fColors.textPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (anyActive) AppIcons.Pause else AppIcons.PlayArrow,
                                            contentDescription = null,
                                            tint = fColors.textPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (anyActive) stringResource(R.string.downloads_pause_all) else stringResource(R.string.downloads_resume_all),
                                        color = fColors.textPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        items(filteredDownloading, key = { it.id }) { record ->
                            MobileDownloadingItem(
                                record = record,
                                onPause = { viewModel.pauseDownload(record.id) },
                                onResume = { viewModel.resumeDownload(record.id) },
                                onDelete = { menuRecord = record },
                                onShowErrorDetails = { errorToShow = it },
                                isSelected = selectedIds.contains(record.id),
                                isSelectionMode = isSelectionMode,
                                onLongPress = { toggleSelection(record.id) }
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(bottom = 60.dp), 
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(fColors.cardSecondary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AppIcons.CloudQueue, contentDescription = null, tint = fColors.textSecondary, modifier = Modifier.size(36.dp))
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(stringResource(R.string.downloads_empty_progress_title), color = fColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.downloads_empty_progress_subtitle), color = fColors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DownloadsOptionBottomSheet(
        menuRecord = menuRecord,
        onDismiss = { menuRecord = null },
        onShareFile = { onShareFile(it) },
        onConvertClick = { recordToConvert = it },
        onDeletePermanent = { id ->
            if (AppSettings.confirmOnDelete) itemToDelete = id else viewModel.deleteDownload(id)
        },
        viewModel = mainViewModel,
        colors = fColors
    )

    DownloadsConvertDialog(
        record = recordToConvert,
        onDismiss = { recordToConvert = null },
        viewModel = mainViewModel,
        colors = fColors
    )

    DownloadsErrorDialog(
        errorMsg = errorToShow,
        onDismiss = { errorToShow = null },
        colors = fColors
    )

    DownloadsDeleteConfirmDialog(
        show = itemToDelete != null,
        onConfirm = {
            itemToDelete?.let { viewModel.deleteDownload(it) }
            itemToDelete = null
        },
        onDismiss = { itemToDelete = null },
        colors = fColors
    )

    if (showSpeedSliderDialog) {
        SpeedSliderDialog(
            initialSpeed = AppSettings.maxSpeed,
            speedOptions = AppSettings.speedOptions,
            onSpeedSelected = { AppSettings.maxSpeed = it },
            onDismiss = { showSpeedSliderDialog = false }
        )
    }
}