package com.fabian.downloader.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.managers.YtdlpUpdateManager
import com.fabian.downloader.managers.YtdlpVersionInfo
import com.fabian.downloader.ui.theme.FabiColors
import kotlinx.coroutines.launch

private enum class UpdateState {
    IDLE,
    CHECKING,
    UPDATING,
    RESETTING
}

@Composable
fun YtdlpUpdateDialog(
    onDismiss: () -> Unit,
    colors: FabiColors
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(UpdateState.CHECKING) }
    var versionInfo by remember { mutableStateOf<YtdlpVersionInfo?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun checkVersions() {
        scope.launch {
            state = UpdateState.CHECKING
            statusMessage = null
            isError = false
            val result = YtdlpUpdateManager.checkYtdlpUpdate(ctx)
            result.onSuccess { info ->
                versionInfo = info
                state = UpdateState.IDLE
            }.onFailure { err ->
                versionInfo = YtdlpVersionInfo(
                    currentVersion = YtdlpUpdateManager.getLocalVersion(ctx),
                    latestVersion = "Unknown",
                    publishedDate = "",
                    releaseNotes = "",
                    hasUpdate = false
                )
                statusMessage = ctx.getString(R.string.ytdlp_update_error_msg, err.localizedMessage ?: "")
                isError = true
                state = UpdateState.IDLE
            }
        }
    }

    LaunchedEffect(Unit) {
        checkVersions()
    }

    AlertDialog(
        onDismissRequest = {
            if (state == UpdateState.IDLE) onDismiss()
        },
        containerColor = colors.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.accentDim, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Download,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.ytdlp_update_dialog_title),
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Versión actual vs Versión remota
                Surface(
                    color = colors.cardSecondary,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.ytdlp_update_installed_version),
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                            Surface(
                                color = colors.card,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = versionInfo?.currentVersion ?: YtdlpUpdateManager.getLocalVersion(ctx),
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.ytdlp_update_latest_version),
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                            val hasUpdate = versionInfo?.hasUpdate == true
                            Surface(
                                color = if (hasUpdate) colors.accentDim else colors.card,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (hasUpdate) colors.accent else colors.border)
                            ) {
                                Text(
                                    text = versionInfo?.latestVersion ?: "...",
                                    color = if (hasUpdate) colors.accent else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Indicador de Estado / Progreso
                when (state) {
                    UpdateState.CHECKING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.ytdlp_update_state_checking),
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    UpdateState.UPDATING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.ytdlp_update_state_updating),
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    UpdateState.RESETTING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.amber,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.ytdlp_update_state_resetting),
                                color = colors.amber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    UpdateState.IDLE -> {
                        if (statusMessage != null) {
                            Surface(
                                color = if (isError) colors.errorDim else colors.success.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isError) colors.error else colors.success)
                            ) {
                                Text(
                                    text = statusMessage!!,
                                    color = if (isError) colors.error else colors.success,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else if (versionInfo != null) {
                            if (versionInfo!!.hasUpdate) {
                                Surface(
                                    color = colors.accentDim,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.accent)
                                ) {
                                    Text(
                                        text = stringResource(R.string.ytdlp_update_available),
                                        color = colors.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = colors.cardSecondary,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = stringResource(R.string.ytdlp_update_up_to_date),
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Changelog / Release Notes
                if (!versionInfo?.releaseNotes.isNullOrEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.ytdlp_update_notes_title),
                            color = colors.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(colors.cardSecondary, RoundedCornerShape(10.dp))
                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = versionInfo!!.releaseNotes,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Opción de Restablecimiento
                Text(
                    text = stringResource(R.string.ytdlp_update_btn_reset_desc),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            val isBusy = state != UpdateState.IDLE
            Button(
                onClick = {
                    scope.launch {
                        state = UpdateState.UPDATING
                        statusMessage = null
                        val result = YtdlpUpdateManager.updateYtdlp(ctx)
                        result.onSuccess { newVer ->
                            statusMessage = ctx.getString(R.string.ytdlp_update_success_msg)
                            isError = false
                            versionInfo = versionInfo?.copy(currentVersion = newVer, hasUpdate = false)
                            state = UpdateState.IDLE
                        }.onFailure { err ->
                            statusMessage = ctx.getString(R.string.ytdlp_update_error_msg, err.localizedMessage ?: "")
                            isError = true
                            state = UpdateState.IDLE
                        }
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color(0xFF0A0A0C)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ytdlp_update_btn_update),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            val isBusy = state != UpdateState.IDLE
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        scope.launch {
                            state = UpdateState.RESETTING
                            statusMessage = null
                            val result = YtdlpUpdateManager.resetEngine(ctx)
                            result.onSuccess {
                                statusMessage = ctx.getString(R.string.ytdlp_update_reset_success_msg)
                                isError = false
                                versionInfo = versionInfo?.copy(
                                    currentVersion = YtdlpUpdateManager.getLocalVersion(ctx),
                                    hasUpdate = true
                                )
                                state = UpdateState.IDLE
                            }.onFailure { err ->
                                statusMessage = ctx.getString(R.string.ytdlp_update_error_msg, err.localizedMessage ?: "")
                                isError = true
                                state = UpdateState.IDLE
                            }
                        }
                    },
                    enabled = !isBusy
                ) {
                    Text(
                        text = stringResource(R.string.ytdlp_update_btn_reset),
                        color = colors.amber,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isBusy
                ) {
                    Text(
                        text = stringResource(R.string.ytdlp_update_btn_close),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    )
}
