package com.fabian.downloader.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.theme.FabiColorScheme
import com.fabian.downloader.ui.viewmodels.MainViewModel
import com.fabian.downloader.utils.ToastUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsOptionBottomSheet(
    menuRecord: DownloadRecord?,
    onDismiss: () -> Unit,
    onShareFile: (DownloadRecord) -> Unit,
    onConvertClick: (DownloadRecord) -> Unit,
    onDeletePermanent: (Long) -> Unit,
    viewModel: MainViewModel,
    colors: FabiColorScheme
) {
    if (menuRecord == null) return
    val ctx = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textMuted) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            if (menuRecord.isCompleted) {
                // Completed Options
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onShareFile(menuRecord)
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.accentDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Share, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_share_file), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_share_file_desc), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = colors.border)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        clipboardManager.setText(AnnotatedString(menuRecord.url))
                        ToastUtils.showShort(ctx, R.string.downloads_link_copied)
                        onDismiss()
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.cardSecondary, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Copy, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_copy_link), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_url_prefix, menuRecord.url.take(30)), color = colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                HorizontalDivider(color = colors.border)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.deleteDownloadHistory(menuRecord.id)
                        onDismiss()
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.cardSecondary, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.History, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_clear_history_item), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_clear_history_desc), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = colors.border)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onConvertClick(menuRecord)
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.accentDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Transform, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_convert_format), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_convert_format_desc), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = colors.border)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onDeletePermanent(menuRecord.id)
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.errorDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.DeleteForever, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_delete_permanent), color = colors.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_delete_permanent_desc), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                // Active Download Options
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        clipboardManager.setText(AnnotatedString(menuRecord.url))
                        ToastUtils.showShort(ctx, R.string.downloads_link_copied)
                        onDismiss()
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.cardSecondary, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Copy, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_copy_link), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_url_prefix, menuRecord.url.take(30)), color = colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                HorizontalDivider(color = colors.border)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.forceDownload(menuRecord.id)
                        onDismiss()
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.accentDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Bolt, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_force_download_title), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_force_download_desc), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = colors.border)
                val isPaused = menuRecord.isPaused
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (isPaused) viewModel.resumeDownload(menuRecord.id) else viewModel.pauseDownload(menuRecord.id)
                        onDismiss()
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.accentDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (isPaused) AppIcons.PlayArrow else AppIcons.Pause, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(if (isPaused) stringResource(R.string.downloads_resume) else stringResource(R.string.downloads_pause), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (isPaused) stringResource(R.string.downloads_resume_desc) else stringResource(R.string.downloads_pause_desc), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = colors.border)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onDeletePermanent(menuRecord.id)
                    }.padding(20.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).background(colors.errorDim, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.DeleteForever, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.downloads_cancel_delete), color = colors.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.downloads_cancel_delete_desc), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsConvertDialog(
    record: DownloadRecord?,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    colors: FabiColorScheme
) {
    if (record == null) return
    val ctx = LocalContext.current
    val formats = listOf("MP4", "MKV", "MP3", "M4A", "AAC", "FLAC", "OPUS")
    var selectedFormat by remember(record) { 
        mutableStateOf(formats.firstOrNull { !it.equals(record.format, ignoreCase = true) } ?: "MP3") 
    }
    var isConverting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isConverting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Transform, contentDescription = null, tint = colors.accent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.downloads_convert_dialog_title), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        },
        containerColor = colors.card,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = record.title,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.downloads_convert_dialog_subtitle),
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 160.dp)
                ) {
                    items(formats) { fmt ->
                        val isSelected = selectedFormat.equals(fmt, ignoreCase = true)
                        val isCurrent = record.format.equals(fmt, ignoreCase = true)
                        Surface(
                            onClick = { if (!isConverting) selectedFormat = fmt },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected -> colors.accentDim
                                isCurrent -> colors.cardSecondary.copy(alpha = 0.5f)
                                else -> colors.cardSecondary
                            },
                            border = when {
                                isSelected -> BorderStroke(1.5.dp, colors.accent)
                                else -> BorderStroke(1.dp, colors.border)
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = fmt,
                                    color = if (isSelected) colors.accent else if (isCurrent) colors.textMuted else colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                if (isConverting) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.downloads_converting),
                            color = colors.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isConverting,
                onClick = {
                    isConverting = true
                    viewModel.convertDownloadFormat(record, selectedFormat) { success, errorMsg ->
                        isConverting = false
                        onDismiss()
                        if (success) {
                            ToastUtils.showShort(ctx, ctx.getString(R.string.downloads_convert_success, selectedFormat))
                        } else {
                            ToastUtils.showShort(ctx, ctx.getString(R.string.downloads_convert_error, errorMsg ?: ""))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF0A0A0C))
            ) {
                Text(stringResource(R.string.downloads_convert_button), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!isConverting) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.downloads_cancel_button), color = colors.textSecondary)
                }
            }
        }
    )
}

@Composable
fun DownloadsErrorDialog(
    errorMsg: String?,
    onDismiss: () -> Unit,
    colors: FabiColorScheme
) {
    if (errorMsg == null) return
    val ctx = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.ErrorOutline, null, tint = colors.error)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.downloads_error_details_title), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        },
        containerColor = colors.card,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 300.dp),
                    color = colors.cardSecondary,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(errorMsg))
                    ToastUtils.showShort(ctx, R.string.downloads_error_copied)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF0A0A0C))
            ) {
                Icon(AppIcons.Copy, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.downloads_copy_all_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.downloads_close_button), color = colors.textPrimary)
            }
        }
    )
}

@Composable
fun DownloadsDeleteConfirmDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    colors: FabiColorScheme
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.downloads_delete_title), fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        containerColor = colors.card,
        text = { Text(stringResource(R.string.downloads_delete_message), color = colors.textSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error)
            ) {
                Text(stringResource(R.string.downloads_delete_button), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.downloads_cancel_button), color = colors.accent)
            }
        }
    )
}
