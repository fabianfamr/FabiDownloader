package com.fabian.downloader.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fabian.downloader.R
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.InputDialog
import com.fabian.downloader.ui.components.KeyValueSelectionDialog
import com.fabian.downloader.ui.components.SelectionDialog
import com.fabian.downloader.ui.screens.SettingsHeader
import com.fabian.downloader.ui.screens.SettingsRow
import com.fabian.downloader.ui.screens.SettingsToggleRow
import com.fabian.downloader.ui.theme.FabiColors
import com.fabian.downloader.utils.SettingsLabels

@Composable
fun AdvancedSettingsSection(fColors: FabiColors) {
    val ctx = LocalContext.current
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_gray2 = fColors.textMuted
    val C_red = fColors.error
    val C_bg = fColors.background

    var keepHistory by remember { mutableStateOf(AppSettings.keepHistory) }
    var autoRetry by remember { mutableStateOf(AppSettings.autoRetry) }
    var allowDuplicateDownloads by remember { mutableStateOf(AppSettings.allowDuplicateDownloads) }
    var confirmOnDelete by remember { mutableStateOf(AppSettings.confirmOnDelete) }
    var quickShareMode by remember { mutableStateOf(AppSettings.quickShareMode) }
    var clipboardActionState by remember { mutableStateOf(AppSettings.clipboardAction) }

    var showUserAgentDialog by remember { mutableStateOf(false) }
    var showCustomArgsDialog by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }

    if (showUserAgentDialog) {
        InputDialog(
            title = stringResource(R.string.settings_user_agent_title),
            placeholder = stringResource(R.string.settings_user_agent_placeholder),
            initialValue = AppSettings.customUserAgent,
            onConfirm = { AppSettings.customUserAgent = it },
            onDismiss = { showUserAgentDialog = false }
        )
    }

    if (showCustomArgsDialog) {
        InputDialog(
            title = stringResource(R.string.settings_yt_args),
            placeholder = stringResource(R.string.settings_yt_args_placeholder),
            initialValue = AppSettings.customArguments,
            onConfirm = { AppSettings.customArguments = it },
            onDismiss = { showCustomArgsDialog = false }
        )
    }

    if (showClipboardDialog) {
        KeyValueSelectionDialog(
            title = stringResource(R.string.settings_clipboard_action),
            items = SettingsLabels.getClipboardActionOptions(ctx),
            selectedKey = clipboardActionState,
            onSelection = {
                AppSettings.clipboardAction = it
                clipboardActionState = it
                showClipboardDialog = false
            },
            onDismiss = { showClipboardDialog = false }
        )
    }

    // 1. Comportamiento de Descarga
    SettingsHeader(stringResource(R.string.settings_header_download_behavior), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.History,
                title = stringResource(R.string.settings_history),
                subtitle = stringResource(R.string.settings_history_desc),
                checked = keepHistory,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                keepHistory = it
                AppSettings.keepHistory = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.Replay,
                title = stringResource(R.string.settings_retry),
                subtitle = stringResource(R.string.settings_retry_desc),
                checked = autoRetry,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                autoRetry = it
                AppSettings.autoRetry = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.ControlPointDuplicate,
                title = stringResource(R.string.settings_allow_duplicates),
                subtitle = stringResource(R.string.settings_allow_duplicates_desc),
                checked = allowDuplicateDownloads,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                allowDuplicateDownloads = it
                AppSettings.allowDuplicateDownloads = it
            }
        }
    }

    // 2. Acciones y Confirmaciones
    SettingsHeader(stringResource(R.string.settings_header_actions), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.DeleteForever,
                title = stringResource(R.string.settings_confirm_delete),
                subtitle = stringResource(R.string.settings_confirm_delete_desc),
                checked = confirmOnDelete,
                colorAccent = C_red,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                confirmOnDelete = it
                AppSettings.confirmOnDelete = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.Bolt,
                title = stringResource(R.string.settings_quick_share),
                subtitle = stringResource(R.string.settings_quick_share_desc),
                checked = quickShareMode,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                quickShareMode = it
                AppSettings.quickShareMode = it
            }
        }
    }

    // 3. Motor de Extracción (yt-dlp)
    SettingsHeader(stringResource(R.string.settings_header_engine), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsRow(
                icon = AppIcons.Person,
                title = stringResource(R.string.settings_user_agent_title),
                trailing = AppSettings.customUserAgent.ifEmpty { stringResource(R.string.settings_value_default) },
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                showUserAgentDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(
                icon = AppIcons.Code,
                title = stringResource(R.string.settings_yt_args),
                trailing = AppSettings.customArguments.ifEmpty { stringResource(R.string.settings_yt_args_default) },
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                showCustomArgsDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(
                icon = AppIcons.ContentPaste,
                title = stringResource(R.string.settings_clipboard_action),
                trailing = SettingsLabels.getClipboardActionLabel(ctx, clipboardActionState),
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                showClipboardDialog = true
            }
        }
    }
}
