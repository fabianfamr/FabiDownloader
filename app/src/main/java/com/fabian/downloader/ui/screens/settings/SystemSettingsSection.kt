package com.fabian.downloader.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.BuildConfig
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.ErrorLogManager
import com.fabian.downloader.managers.UpdateInfo
import com.fabian.downloader.managers.UpdateManager
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.KeyValueSelectionDialog
import com.fabian.downloader.ui.components.SelectionDialog
import com.fabian.downloader.ui.screens.SettingsHeader
import com.fabian.downloader.ui.screens.SettingsRow
import com.fabian.downloader.ui.screens.SettingsToggleRow
import com.fabian.downloader.ui.theme.FabiColors
import com.fabian.downloader.utils.LocaleHelper
import com.fabian.downloader.utils.SettingsLabels
import com.fabian.downloader.workers.CacheCleanupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SystemSettingsSection(
    fColors: FabiColors,
    ctx: Context,
    onUpdateFound: (UpdateInfo) -> Unit
) {
    val scope = rememberCoroutineScope()

    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_gray2 = fColors.textMuted
    val C_green = fColors.success
    val C_amber = fColors.amber
    val C_bg = fColors.background

    var cacheState by remember { mutableStateOf(0) } // 0: Idle, 1: Clearing, 2: Done
    var storageMarginState by remember { mutableStateOf(AppSettings.selectedStorageMargin) }
    var cleanTempOnCancel by remember { mutableStateOf(AppSettings.cleanTempOnCancel) }
    var batteryOptimizationEnabled by remember { mutableStateOf(AppSettings.batteryOptimizationEnabled) }
    var batteryLowThresholdState by remember { mutableStateOf(AppSettings.selectedBatteryLowThreshold) }
    var batteryLowActionState by remember { mutableStateOf(AppSettings.selectedBatteryLowAction) }
    var languageState by remember { mutableStateOf(LocaleHelper.getDisplayName(AppSettings.language)) }

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var isUpdatingYtdlp by remember { mutableStateOf(false) }
    var isCopyingErrors by remember { mutableStateOf(false) }

    var showStorageMarginDialog by remember { mutableStateOf(false) }
    var showBatteryLowThresholdDialog by remember { mutableStateOf(false) }
    var showBatteryLowActionDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showStorageMarginDialog) {
        KeyValueSelectionDialog(
            title = stringResource(R.string.settings_select_storage_margin),
            items = SettingsLabels.getStorageMarginOptions(ctx, AppSettings.storageMarginOptions),
            selectedKey = storageMarginState,
            onSelection = {
                AppSettings.selectedStorageMargin = it
                storageMarginState = it
                showStorageMarginDialog = false
            },
            onDismiss = { showStorageMarginDialog = false }
        )
    }

    if (showBatteryLowThresholdDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_battery_threshold),
            options = AppSettings.batteryLowThresholdOptions,
            selectedOption = batteryLowThresholdState,
            onSelection = {
                AppSettings.selectedBatteryLowThreshold = it
                batteryLowThresholdState = it
                showBatteryLowThresholdDialog = false
            },
            onDismiss = { showBatteryLowThresholdDialog = false }
        )
    }

    if (showBatteryLowActionDialog) {
        KeyValueSelectionDialog(
            title = stringResource(R.string.settings_battery_action),
            items = SettingsLabels.getBatteryActionOptions(ctx, AppSettings.batteryLowActionOptions),
            selectedKey = batteryLowActionState,
            onSelection = {
                AppSettings.selectedBatteryLowAction = it
                batteryLowActionState = it
                showBatteryLowActionDialog = false
            },
            onDismiss = { showBatteryLowActionDialog = false }
        )
    }

    if (showLanguageDialog) {
        val languageOptions = LocaleHelper.SUPPORTED_LANGUAGES
        val selectedOption = LocaleHelper.getDisplayName(languageState)
        SelectionDialog(
            title = stringResource(R.string.settings_select_language),
            options = languageOptions,
            selectedOption = selectedOption,
            onSelection = {
                AppSettings.language = it
                languageState = LocaleHelper.getDisplayName(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // 1. Limpieza y Caché
    SettingsHeader(stringResource(R.string.settings_header_cache), C_gray2)

    val cacheBg = when(cacheState) {
        2 -> C_green.copy(alpha = 0.12f)
        1 -> C_card
        else -> C_amber.copy(alpha = 0.12f)
    }
    val cacheBorder = when(cacheState) {
        2 -> C_green.copy(alpha = 0.33f)
        1 -> C_accent.copy(alpha = 0.2f)
        else -> C_amber.copy(alpha = 0.26f)
    }
    val cacheIconBg = when(cacheState) {
        2 -> C_green.copy(alpha = 0.13f)
        1 -> C_accent.copy(alpha = 0.13f)
        else -> C_amber.copy(alpha = 0.13f)
    }
    val cacheAccent = when(cacheState) {
        2 -> C_green
        1 -> C_accent
        else -> C_amber
    }

    Surface(
        color = cacheBg, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, cacheBorder),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable {
            if (cacheState == 0) {
                scope.launch {
                    cacheState = 1
                    withContext(Dispatchers.IO) {
                        CacheCleanupWorker.performDirectCleanup(ctx)
                    }
                    cacheState = 2
                    delay(2000)
                    cacheState = 0
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(14.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(32.dp).background(cacheIconBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                if (cacheState == 1) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = C_accent)
                } else {
                    Icon(imageVector = if (cacheState == 2) AppIcons.Check else AppIcons.Delete, contentDescription = null, tint = cacheAccent, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = when(cacheState) {
                        1 -> stringResource(R.string.settings_clearing)
                        2 -> stringResource(R.string.settings_cache_cleared)
                        else -> stringResource(R.string.settings_clear_cache)
                    },
                    color = cacheAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(stringResource(R.string.settings_clear_cache_desc), color = C_gray1, fontSize = 11.sp)
            }
        }
    }

    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsRow(
                icon = AppIcons.Storage,
                title = stringResource(R.string.settings_storage_margin),
                trailing = SettingsLabels.getStorageMarginLabel(ctx, storageMarginState),
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2
            ) {
                showStorageMarginDialog = true
            }
        }
    }

    // 2. Archivos Temporales
    SettingsHeader(stringResource(R.string.settings_header_temp_files), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.CleaningServices,
                title = stringResource(R.string.settings_clean_temp),
                subtitle = stringResource(R.string.settings_clean_temp_desc),
                checked = cleanTempOnCancel,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                cleanTempOnCancel = it
                AppSettings.cleanTempOnCancel = it
            }
        }
    }

    // 3. Energía y Batería
    SettingsHeader(stringResource(R.string.settings_header_battery), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.BatteryChargingFull,
                title = stringResource(R.string.settings_battery_optimization),
                subtitle = stringResource(R.string.settings_battery_optimization_desc),
                checked = batteryOptimizationEnabled,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                batteryOptimizationEnabled = it
                AppSettings.batteryOptimizationEnabled = it
            }

            if (batteryOptimizationEnabled) {
                HorizontalDivider(color = C_border, thickness = 1.dp)
                SettingsRow(
                    icon = AppIcons.BatteryAlert,
                    title = stringResource(R.string.settings_battery_threshold),
                    trailing = batteryLowThresholdState,
                    colorAccent = C_accent,
                    textColor = C_white,
                    grayColor = C_gray1,
                    card2Color = C_card2
                ) {
                    showBatteryLowThresholdDialog = true
                }
                HorizontalDivider(color = C_border, thickness = 1.dp)
                SettingsRow(
                    icon = AppIcons.SettingsApplications,
                    title = stringResource(R.string.settings_battery_action),
                    trailing = SettingsLabels.getBatteryActionLabel(ctx, batteryLowActionState),
                    colorAccent = C_accent,
                    textColor = C_white,
                    grayColor = C_gray1,
                    card2Color = C_card2
                ) {
                    showBatteryLowActionDialog = true
                }
            }
        }
    }

    // 4. Información y Diagnóstico
    SettingsHeader(stringResource(R.string.settings_section_general_header), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp, 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(32.dp).background(C_card2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Info, contentDescription = null, tint = C_accent, modifier = Modifier.size(16.dp))
                    }
                    Text(stringResource(R.string.settings_version_title), color = C_white, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.background(C_card2, RoundedCornerShape(20.dp)).border(1.dp, C_border, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("v${BuildConfig.VERSION_NAME}", color = C_gray1, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.Language, stringResource(R.string.settings_language), languageState, C_accent, C_white, C_gray1, C_card2) {
                showLanguageDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.Update, stringResource(R.string.settings_check_updates), if (isCheckingUpdates) stringResource(R.string.settings_update_searching) else stringResource(R.string.settings_update_now_btn), C_accent, C_white, C_gray1, C_card2) {
                if (!isCheckingUpdates) {
                    scope.launch {
                        isCheckingUpdates = true
                        val result = UpdateManager.checkForUpdates()
                        isCheckingUpdates = false
                        result.onSuccess { info ->
                            if (info != null && UpdateManager.isNewerVersion(info.latestVersion, BuildConfig.VERSION_NAME)) {
                                onUpdateFound(info)
                            } else {
                                Toast.makeText(ctx, ctx.getString(R.string.settings_update_not_available), Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { e ->
                            Toast.makeText(ctx, ctx.getString(R.string.settings_error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.Download, stringResource(R.string.settings_update_engine), if (isUpdatingYtdlp) stringResource(R.string.settings_updating) else stringResource(R.string.settings_update_binary), C_accent, C_white, C_gray1, C_card2) {
                if (!isUpdatingYtdlp) {
                    scope.launch(Dispatchers.IO) {
                        isUpdatingYtdlp = true
                        val success = com.fabian.downloader.MyApplication.getInstance().forceUpdateYtdlpBinary(ctx, ignoreThrottle = true)
                        isUpdatingYtdlp = false
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(ctx, ctx.getString(R.string.settings_update_ytdlp_success), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(ctx, ctx.getString(R.string.settings_update_ytdlp_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(AppIcons.Code, stringResource(R.string.settings_github_repo), stringResource(R.string.settings_view_code), C_accent, C_white, C_gray1, C_card2) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Config.GITHUB_URL)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsRow(
                AppIcons.BugReport,
                stringResource(R.string.settings_copy_errors),
                if (isCopyingErrors) stringResource(R.string.settings_copying) else stringResource(R.string.settings_copy_clipboard),
                C_amber,
                C_white,
                C_gray1,
                C_card2
            ) {
                if (!isCopyingErrors) {
                    scope.launch {
                        isCopyingErrors = true
                        ErrorLogManager.copyErrorsToClipboard(ctx)
                        isCopyingErrors = false
                        Toast.makeText(ctx, ctx.getString(R.string.settings_logs_copied), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
