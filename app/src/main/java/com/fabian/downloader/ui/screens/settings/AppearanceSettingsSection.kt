package com.fabian.downloader.ui.screens.settings

import android.os.Build
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.components.SelectionDialog
import com.fabian.downloader.ui.screens.SettingsHeader
import com.fabian.downloader.ui.screens.SettingsRow
import com.fabian.downloader.ui.screens.SettingsToggleRow
import com.fabian.downloader.ui.theme.FabiColors

@Composable
fun AppearanceSettingsSection(fColors: FabiColors) {
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_gray2 = fColors.textMuted
    val C_bg = fColors.background

    var themePreferenceState by remember { mutableStateOf(AppSettings.themePreference) }
    var dynamicColor by remember { mutableStateOf(AppSettings.dynamicColor) }
    var accentColorNameState by remember { mutableStateOf(AppSettings.accentColorName) }
    var amoledMode by remember { mutableStateOf(AppSettings.amoledMode) }
    var cardStyleState by remember { mutableStateOf(AppSettings.cardStyle) }
    var showQualityBadge by remember { mutableStateOf(AppSettings.showQualityBadge) }
    var showRealtimeSpeedCard by remember { mutableStateOf(AppSettings.showRealtimeSpeedCard) }
    var notificationsEnabled by remember { mutableStateOf(AppSettings.notificationsEnabled) }
    var showSpeedInNotif by remember { mutableStateOf(AppSettings.showDownloadSpeedInNotification) }
    var notifyBatchComplete by remember { mutableStateOf(AppSettings.notifyBatchComplete) }

    var showAccentDialog by remember { mutableStateOf(false) }
    var showCardStyleDialog by remember { mutableStateOf(false) }

    if (showAccentDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_accent_color),
            options = AppSettings.accentColorOptions,
            selectedOption = accentColorNameState,
            onSelection = {
                AppSettings.accentColorName = it
                accentColorNameState = it
                showAccentDialog = false
            },
            onDismiss = { showAccentDialog = false }
        )
    }

    if (showCardStyleDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_card_style_dialog),
            options = AppSettings.cardStyleOptions,
            selectedOption = cardStyleState,
            onSelection = {
                AppSettings.cardStyle = it
                cardStyleState = it
                showCardStyleDialog = false
            },
            onDismiss = { showCardStyleDialog = false }
        )
    }

    // 1. Tema y Colores
    SettingsHeader(stringResource(R.string.settings_section_theme_colors), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Theme Toggle Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_theme_mode),
                    color = C_white,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C_card2, RoundedCornerShape(12.dp))
                        .border(1.dp, C_border, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        Triple("Sistema", AppIcons.Smartphone, stringResource(R.string.settings_theme_system)),
                        Triple("Claro", AppIcons.LightMode, stringResource(R.string.settings_theme_light)),
                        Triple("Oscuro", AppIcons.DarkMode, stringResource(R.string.settings_theme_dark))
                    )
                    modes.forEach { (modeKey, icon, label) ->
                        val isSelected = themePreferenceState == modeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) C_accent else Color.Transparent)
                                .clickable {
                                    AppSettings.themePreference = modeKey
                                    themePreferenceState = modeKey
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.Black else C_gray1,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else C_white,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = C_border, thickness = 1.dp)

            // Dynamic Colors Material 3 Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(C_card2, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.Palette, contentDescription = null, tint = C_accent, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.settings_dynamic_color),
                            color = C_white,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Surface(
                                color = C_accent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_material3),
                                    color = C_accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.settings_dynamic_color_desc),
                        color = C_gray1,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = {
                        dynamicColor = it
                        AppSettings.dynamicColor = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = C_bg,
                        checkedTrackColor = C_accent,
                        uncheckedThumbColor = C_gray1,
                        uncheckedTrackColor = C_card2,
                        uncheckedBorderColor = C_border
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            if (!dynamicColor) {
                HorizontalDivider(color = C_border, thickness = 1.dp)
                SettingsRow(AppIcons.ColorLens, stringResource(R.string.settings_accent_color), accentColorNameState, C_accent, C_white, C_gray1, C_card2) {
                    showAccentDialog = true
                }
            }

            HorizontalDivider(color = C_border, thickness = 1.dp)

            // AMOLED Mode
            SettingsToggleRow(
                icon = AppIcons.Brightness1,
                title = stringResource(R.string.settings_amoled_mode),
                subtitle = stringResource(R.string.settings_amoled_mode_desc),
                checked = amoledMode,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                amoledMode = it
                AppSettings.amoledMode = it
            }
        }
    }

    // 2. Estilo Visual
    SettingsHeader(stringResource(R.string.settings_header_visual_style), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsRow(AppIcons.ViewStream, stringResource(R.string.settings_card_style), cardStyleState, C_accent, C_white, C_gray1, C_card2) {
                showCardStyleDialog = true
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.HighQuality,
                title = stringResource(R.string.settings_quality_badge),
                subtitle = stringResource(R.string.settings_quality_badge_desc),
                checked = showQualityBadge,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                showQualityBadge = it
                AppSettings.showQualityBadge = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.Speed,
                title = stringResource(R.string.settings_realtime_speed),
                subtitle = stringResource(R.string.settings_realtime_speed_desc),
                checked = showRealtimeSpeedCard,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                showRealtimeSpeedCard = it
                AppSettings.showRealtimeSpeedCard = it
            }
        }
    }

    // 3. Notificaciones y Alertas
    SettingsHeader(stringResource(R.string.settings_header_notifications), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.Notifications,
                title = stringResource(R.string.settings_notif_global),
                subtitle = stringResource(R.string.settings_notif_global_desc),
                checked = notificationsEnabled,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                notificationsEnabled = it
                AppSettings.notificationsEnabled = it
            }
            if (notificationsEnabled) {
                HorizontalDivider(color = C_border, thickness = 1.dp)
                SettingsToggleRow(
                    icon = AppIcons.Speed,
                    title = stringResource(R.string.settings_notif_speed),
                    subtitle = stringResource(R.string.settings_notif_speed_desc),
                    checked = showSpeedInNotif,
                    colorAccent = C_accent,
                    textColor = C_white,
                    grayColor = C_gray1,
                    card2Color = C_card2,
                    borderColor = C_border,
                    bgColor = C_bg
                ) {
                    showSpeedInNotif = it
                    AppSettings.showDownloadSpeedInNotification = it
                }
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.CheckCircle,
                title = stringResource(R.string.settings_notify_batch),
                subtitle = stringResource(R.string.settings_notify_batch_desc),
                checked = notifyBatchComplete,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                notifyBatchComplete = it
                AppSettings.notifyBatchComplete = it
            }
        }
    }
}
