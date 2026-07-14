package com.fabian.downloader.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.pointerInput
import com.fabian.downloader.R
import com.fabian.downloader.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.core.content.FileProvider
import java.io.File
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import kotlin.random.Random
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DownloadsScreen(
    database: AppDatabase,
    modifier: Modifier = Modifier,
    initialPage: Int = 0
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val viewModel: DownloadsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DownloadsViewModel(database) as T
            }
        }
    )
    val downloads by viewModel.downloads.collectAsStateWithLifecycle(initialValue = emptyList())
    val sortDateStr = stringResource(R.string.downloads_sort_date)
    val sortNameStr = stringResource(R.string.downloads_sort_name)
    val sortSizeStr = stringResource(R.string.downloads_sort_size)
    var sortOrder by remember { mutableStateOf(sortDateStr) }
    val downloading = downloads.filter { !it.isCompleted }
    val completed = remember(downloads, sortOrder, sortDateStr, sortNameStr, sortSizeStr) {
        val list = downloads.filter { it.isCompleted }
        when (sortOrder) {
            sortDateStr -> list.sortedByDescending { it.timestamp }
            sortNameStr -> list.sortedBy { it.title }
            sortSizeStr -> list.sortedByDescending { it.size }
            else -> list
        }
    }
    
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var itemToDelete by remember { mutableStateOf<Long?>(null) }
    var menuRecord by remember { mutableStateOf<DownloadRecord?>(null) }
    var errorToShow by remember { mutableStateOf<String?>(null) }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val toggleSelection: (Long) -> Unit = { id ->
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    val onShareFile: (DownloadRecord) -> Unit = { record ->
        try {
            val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(ctx, record.title, record.id, record.format)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(ctx, "com.fabian.downloader.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = if (record.format == com.fabian.downloader.utils.Config.FORMAT_MP4) com.fabian.downloader.utils.Config.MIME_VIDEO else com.fabian.downloader.utils.Config.MIME_AUDIO
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.downloads_share_title)))
            } else {
                Toast.makeText(ctx, ctx.getString(R.string.main_error_file_not_found), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, ctx.getString(R.string.downloads_share_error_prefix, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(ctx, ctx.getString(R.string.main_error_file_not_found), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, ctx.getString(R.string.main_error_opening_file, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    val shareSelectedFiles: () -> Unit = {
        try {
            val uris = ArrayList<android.net.Uri>()
            selectedIds.forEach { id ->
                val record = downloads.find { it.id == id }
                if (record != null) {
                    val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(ctx, record.title, record.id, record.format)
                    
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(
                            ctx,
                            "com.fabian.downloader.fileprovider",
                            file
                        )
                        uris.add(uri)
                    }
                }
            }
            
            if (uris.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = "*/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.downloads_share_title)))
            } else {
                Toast.makeText(ctx, ctx.getString(R.string.downloads_share_empty), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, ctx.getString(R.string.downloads_share_error, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
        }
    }
    
    val fColors = MaterialTheme.fabiColors
    val C_bg = fColors.background
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_accentDim = fColors.accentDim
    val C_accentGlow = fColors.accentGlow
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_gray2 = fColors.textMuted
    val C_gray3 = fColors.textDisabled
    val C_red = fColors.error
    val C_redDim = fColors.errorDim
    val C_green = fColors.success
    val C_amber = fColors.amber

    if (menuRecord != null) {
        ModalBottomSheet(
            onDismissRequest = { menuRecord = null },
            containerColor = C_card,
            dragHandle = { BottomSheetDefaults.DragHandle(color = C_gray2) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                if (menuRecord!!.isCompleted) {
                    // Completed Options
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(menuRecord!!.url))
                            Toast.makeText(ctx, ctx.getString(R.string.downloads_link_copied), Toast.LENGTH_SHORT).show()
                            menuRecord = null
                        }.padding(20.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = C_accent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.downloads_copy_link), color = C_white, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.downloads_url_prefix, menuRecord!!.url.take(30)), color = C_gray1, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider(color = C_border)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.deleteDownloadHistory(menuRecord!!.id)
                            menuRecord = null
                        }.padding(20.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.History, contentDescription = null, tint = C_white, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.downloads_clear_history_item), color = C_white, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.downloads_clear_history_desc), color = C_gray1, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = C_border)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            itemToDelete = menuRecord!!.id
                            menuRecord = null
                        }.padding(20.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(C_redDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = C_red, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.downloads_delete_permanent), color = C_red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.downloads_delete_permanent_desc), color = C_gray1, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Active Download Options
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(menuRecord!!.url))
                            Toast.makeText(ctx, ctx.getString(R.string.downloads_link_copied), Toast.LENGTH_SHORT).show()
                            menuRecord = null
                        }.padding(20.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = C_accent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.downloads_copy_link), color = C_white, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.downloads_url_prefix, menuRecord!!.url.take(30)), color = C_gray1, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider(color = C_border)
                    // Active Download Controls
                    val isPaused = menuRecord!!.isPaused
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isPaused) viewModel.resumeDownload(menuRecord!!.id) else viewModel.pauseDownload(menuRecord!!.id)
                            menuRecord = null
                        }.padding(20.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(C_accentDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = C_accent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (isPaused) stringResource(R.string.downloads_resume) else stringResource(R.string.downloads_pause), color = C_white, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(if (isPaused) stringResource(R.string.downloads_resume_desc) else stringResource(R.string.downloads_pause_desc), color = C_gray1, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = C_border)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            itemToDelete = menuRecord!!.id
                            menuRecord = null
                        }.padding(20.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(C_redDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = C_red, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.downloads_cancel_delete), color = C_red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.downloads_cancel_delete_desc), color = C_gray1, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.downloads_delete_title), fontWeight = FontWeight.Bold, color = C_white) },
            containerColor = C_card,
            text = { Text(stringResource(R.string.downloads_delete_message), color = C_gray1) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDownload(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C_red)
                ) {
                    Text(stringResource(R.string.downloads_delete_button), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.downloads_cancel_button), color = C_accent)
                }
            }
        )
    }

    if (errorToShow != null) {
        AlertDialog(
            onDismissRequest = { errorToShow = null },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = C_red)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.downloads_error_details_title), fontWeight = FontWeight.Bold, color = C_white)
                }
            },
            containerColor = C_card,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 300.dp),
                        color = C_card2,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, C_border)
                    ) {
                        Text(
                            text = errorToShow!!,
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall,
                            color = C_gray1
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(errorToShow!!))
                        Toast.makeText(ctx, ctx.getString(R.string.downloads_error_copied), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C_accent, contentColor = Color(0xFF0A0A0C))
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.downloads_copy_all_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { errorToShow = null }) {
                    Text(stringResource(R.string.downloads_close_button), color = C_white)
                }
            }
        )
    }

    val handleDelete: (Long) -> Unit = { id ->
        if (AppSettings.confirmOnDelete) {
            itemToDelete = id
        } else {
            viewModel.deleteDownload(id)
        }
    }

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
            .background(C_bg)
    ) {
        // Modern Title Header
        if (isSelectionMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = C_accentDim,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, C_accentGlow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(Icons.Default.Close, stringResource(R.string.downloads_cancel_selection), tint = C_accent)
                    }
                    Text(
                        text = stringResource(R.string.downloads_selected_count, selectedIds.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = C_white,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    IconButton(onClick = { shareSelectedFiles() }) {
                        Icon(Icons.Default.Share, stringResource(R.string.downloads_share_icon), tint = C_accent)
                    }
                    IconButton(onClick = { 
                        selectedIds.forEach { viewModel.deleteDownload(it) }
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.downloads_delete_button), tint = C_red)
                    }
                }
            }
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
                    color = C_white,
                    modifier = Modifier.weight(1f)
                )
                
                if (pagerState.currentPage == 0) {
                    IconButton(
                        onClick = { viewModel.clearCompletedDownloads() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(C_card2, CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteSweep, stringResource(R.string.downloads_clear_history), tint = C_gray1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                var expanded by remember { mutableStateOf(false) }
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(C_card2, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.downloads_sort), tint = C_gray1)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(C_card)
                    ) {
                        listOf(stringResource(R.string.downloads_sort_date), stringResource(R.string.downloads_sort_name), stringResource(R.string.downloads_sort_size)).forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        fontWeight = if (sortOrder == option) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sortOrder == option) C_accent else C_white
                                    )
                                },
                                onClick = {
                                    sortOrder = option
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (sortOrder == option) {
                                        Icon(Icons.Default.Check, null, tint = C_accent, modifier = Modifier.size(16.dp))
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

        // Custom Tab Control with sliding accent indicator (exactly like Figma React prototype)
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = C_card,
            border = BorderStroke(1.dp, C_border)
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
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = C_accent.copy(alpha = animatedBgAlpha * 0.15f),
                        border = if (isSelected) BorderStroke(1.dp, C_accentGlow) else null,
                        contentColor = if (isSelected) C_accent else C_gray1
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
                    color = if (isSelected) C_accentDim else C_card2,
                    border = if (isSelected) BorderStroke(1.dp, C_accent) else BorderStroke(1.dp, C_border),
                    modifier = Modifier.height(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = type,
                            color = if (isSelected) C_accent else C_gray1,
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
                    ctx.getString(R.string.downloads_filter_music) -> completed.filter { it.format == com.fabian.downloader.utils.Config.FORMAT_MP3 || it.format == com.fabian.downloader.utils.Config.FORMAT_M4A }
                    ctx.getString(R.string.downloads_filter_video) -> completed.filter { it.format == com.fabian.downloader.utils.Config.FORMAT_MP4 }
                    else -> completed
                }
            }
            val filteredDownloading = remember(downloading, filterType) {
                when (filterType) {
                    ctx.getString(R.string.downloads_filter_music) -> downloading.filter { it.format == com.fabian.downloader.utils.Config.FORMAT_MP3 || it.format == com.fabian.downloader.utils.Config.FORMAT_M4A }
                    ctx.getString(R.string.downloads_filter_video) -> downloading.filter { it.format == com.fabian.downloader.utils.Config.FORMAT_MP4 }
                    else -> downloading
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (page == 0) {
                    // Descargados (Completados)
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
                                            .background(C_card2, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen, 
                                            contentDescription = null, 
                                            tint = C_gray1, 
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        stringResource(R.string.downloads_empty_completed_title), 
                                        color = C_white, 
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        stringResource(R.string.downloads_empty_completed_subtitle), 
                                        color = C_gray1, 
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // En progreso
                    if (filteredDownloading.isNotEmpty()) {
                        items(filteredDownloading, key = { it.id }) { record ->
                            MobileDownloadingItem(
                                record = record,
                                onPause = { viewModel.pauseDownload(record.id) },
                                onResume = { viewModel.resumeDownload(record.id) },
                                onDelete = { menuRecord = record },
                                onShowErrorDetails = { errorToShow = it },
                                isSelected = selectedIds.contains(record.id),
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
                                            .background(C_card2, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue, 
                                            contentDescription = null, 
                                            tint = C_gray1, 
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        stringResource(R.string.downloads_empty_progress_title), 
                                        color = C_white, 
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        stringResource(R.string.downloads_empty_progress_subtitle), 
                                        color = C_gray1, 
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MobileDownloadingItem(
    record: DownloadRecord, 
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onShowErrorDetails: (String) -> Unit,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    val fColors = MaterialTheme.fabiColors
    val C_bg = fColors.background
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_accentDim = fColors.accentDim
    val C_accentGlow = fColors.accentGlow
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_red = fColors.error
    val C_redDim = fColors.errorDim
    val C_green = fColors.success
    val C_amber = fColors.amber

    val failedPrefix = stringResource(R.string.downloads_failed_prefix)
    val isFailed = record.title.startsWith(failedPrefix)
    val cleanTitle = remember(record.title) {
        var t = record.title
        while (t.startsWith(failedPrefix)) {
            t = t.substringAfter(failedPrefix)
        }
        t
    }
    
    val isNetworkError = remember(record.size) {
        val s = record.size.lowercase()
        s.contains("network") || 
        s.contains("connection") || 
        s.contains("timeout") || 
        s.contains("red") || 
        s.contains("conexión") || 
        s.contains("host") || 
        s.contains("offline") || 
        s.contains("resolv") || 
        s.contains("espera") || 
        s.contains("internet")
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            isFailed -> C_red
            record.isPaused -> C_amber
            else -> C_accent
        },
        label = "statusColor"
    )

    Surface(
        color = if (isSelected) C_accentDim else C_card,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, if (isSelected) C_accent else (if (isFailed) statusColor.copy(alpha = 0.35f) else C_border)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { 
                    if (isFailed) onResume() 
                    else if (record.isPaused) onResume() 
                    else onPause() 
                },
                onLongClick = { onLongPress() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (platformIcon, platformColor) = getPlatformIconAndColor(record.url, record.format)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            color = if (isFailed) {
                                if (isNetworkError) C_amber.copy(alpha = 0.15f) else C_redDim
                            } else {
                                platformColor.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!record.thumbnailUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = record.thumbnailUrl,
                            contentDescription = stringResource(R.string.downloads_thumbnail),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (isFailed) {
                                if (isNetworkError) Icons.Default.WifiOff else Icons.Default.Error
                            } else {
                                platformIcon
                            },
                            contentDescription = null, 
                            modifier = Modifier.size(24.dp),
                            tint = if (isFailed) {
                                if (isNetworkError) C_amber else C_red
                            } else {
                                platformColor
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanTitle, 
                        style = MaterialTheme.typography.titleSmall,
                        color = C_white, 
                        fontWeight = FontWeight.Bold, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = platformColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${record.quality} • ${record.format}", 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = platformColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        if (isFailed) {
                            Surface(
                                color = if (isNetworkError) C_amber.copy(alpha = 0.12f) else C_redDim,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isNetworkError) stringResource(R.string.downloads_error_network_retry) else stringResource(R.string.downloads_error_retry), 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNetworkError) C_amber else C_red,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (record.isPaused) {
                            Surface(
                                color = C_amber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.downloads_status_paused), 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = C_amber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = { onDelete() }, 
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert, 
                        contentDescription = stringResource(R.string.downloads_action_options), 
                        modifier = Modifier.size(18.dp), 
                        tint = C_white
                    )
                }
            }

            if (!isFailed) {
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (record.progress < 0) stringResource(R.string.downloads_connecting) else "${record.progress}%", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val speedText = if (record.isPaused) stringResource(R.string.downloads_status_paused) else record.speed
                    Text(
                        text = speedText, 
                        style = MaterialTheme.typography.labelSmall,
                        color = C_gray1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                val animatedProgress by animateFloatAsState(
                    targetValue = if (record.progress < 0) 0f else record.progress / 100f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "progress"
                )

                if (record.progress < 0) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape),
                        color = statusColor,
                        trackColor = C_border,
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape),
                        color = statusColor,
                        trackColor = C_border,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobileDownloadedItem(record: DownloadRecord, onPlay: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit, isSelected: Boolean, onLongPress: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fColors = MaterialTheme.fabiColors
    val C_bg = fColors.background
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_accentDim = fColors.accentDim
    val C_accentGlow = fColors.accentGlow
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_red = fColors.error
    val C_green = fColors.success

    val cleanTitle = remember(record.title) {
        var t = record.title
        while (t.startsWith(com.fabian.downloader.utils.Config.STATUS_FAILED_PREFIX)) {
            t = t.substringAfter(com.fabian.downloader.utils.Config.STATUS_FAILED_PREFIX)
        }
        t
    }
    val (platformIcon, platformColor) = getPlatformIconAndColor(record.url, record.format)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) C_accentDim else C_card,
        border = if (isSelected) BorderStroke(2.dp, C_accent) else BorderStroke(1.dp, C_border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { onPlay() },
                onLongClick = { onLongPress() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(platformColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (!record.thumbnailUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = record.thumbnailUrl,
                        contentDescription = stringResource(R.string.downloads_thumbnail),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = platformIcon,
                        contentDescription = null, 
                        modifier = Modifier.size(24.dp),
                        tint = platformColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanTitle, 
                    style = MaterialTheme.typography.titleSmall,
                    color = C_white, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = platformColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${record.quality} • ${record.format}", 
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = platformColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Box(modifier = Modifier.size(3.dp).background(C_gray1, CircleShape))
                    Text(
                        text = record.size, 
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = C_gray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
                     
            IconButton(
                onClick = { onDelete() }, 
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert, 
                    contentDescription = stringResource(R.string.downloads_action_options), 
                    tint = C_white,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}