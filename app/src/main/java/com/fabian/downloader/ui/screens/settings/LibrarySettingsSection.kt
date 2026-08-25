package com.fabian.downloader.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fabian.downloader.R
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.screens.SettingsHeader
import com.fabian.downloader.ui.screens.SettingsToggleRow
import com.fabian.downloader.ui.theme.FabiColors

@Composable
fun LibrarySettingsSection(fColors: FabiColors) {
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary
    val C_gray2 = fColors.textMuted
    val C_amber = fColors.amber
    val C_bg = fColors.background

    var embedThumbnail by remember { mutableStateOf(AppSettings.embedThumbnail) }
    var embedMetadata by remember { mutableStateOf(AppSettings.embedMetadata) }
    var markAsMV by remember { mutableStateOf(AppSettings.markAsMV) }
    var embedSubtitles by remember { mutableStateOf(AppSettings.embedSubtitles) }
    var embedChapters by remember { mutableStateOf(AppSettings.embedChapters) }
    var sponsorBlock by remember { mutableStateOf(AppSettings.sponsorBlockEnabled) }
    var bypassGeo by remember { mutableStateOf(AppSettings.bypassGeo) }

    // 1. Metadatos y Etiquetas
    SettingsHeader(stringResource(R.string.settings_header_metadata), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.Image,
                title = stringResource(R.string.settings_thumbnail),
                subtitle = stringResource(R.string.settings_thumbnail_desc),
                checked = embedThumbnail,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                embedThumbnail = it
                AppSettings.embedThumbnail = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.Label,
                title = stringResource(R.string.settings_metadata),
                subtitle = stringResource(R.string.settings_metadata_desc),
                checked = embedMetadata,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                embedMetadata = it
                AppSettings.embedMetadata = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.MusicVideo,
                title = stringResource(R.string.settings_mark_as_mv),
                subtitle = stringResource(R.string.settings_mark_as_mv_desc),
                checked = markAsMV,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                markAsMV = it
                AppSettings.markAsMV = it
            }
        }
    }

    // 2. Subtítulos y Capítulos
    SettingsHeader(stringResource(R.string.settings_header_subtitles), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.Subtitles,
                title = stringResource(R.string.settings_subtitles),
                subtitle = stringResource(R.string.settings_subtitles_desc),
                checked = embedSubtitles,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                embedSubtitles = it
                AppSettings.embedSubtitles = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.Bookmark,
                title = stringResource(R.string.settings_embed_chapters),
                subtitle = stringResource(R.string.settings_embed_chapters_desc),
                checked = embedChapters,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                embedChapters = it
                AppSettings.embedChapters = it
            }
        }
    }

    // 3. Filtros y Restricciones
    SettingsHeader(stringResource(R.string.settings_header_filters), C_gray2)
    Surface(
        color = C_card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, C_border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Column {
            SettingsToggleRow(
                icon = AppIcons.Block,
                title = stringResource(R.string.settings_sponsorblock),
                subtitle = stringResource(R.string.settings_sponsorblock_desc),
                checked = sponsorBlock,
                colorAccent = C_amber,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                sponsorBlock = it
                AppSettings.sponsorBlockEnabled = it
            }
            HorizontalDivider(color = C_border, thickness = 1.dp)
            SettingsToggleRow(
                icon = AppIcons.Public,
                title = stringResource(R.string.settings_geo_bypass),
                subtitle = stringResource(R.string.settings_geo_bypass_desc),
                checked = bypassGeo,
                colorAccent = C_accent,
                textColor = C_white,
                grayColor = C_gray1,
                card2Color = C_card2,
                borderColor = C_border,
                bgColor = C_bg
            ) {
                bypassGeo = it
                AppSettings.bypassGeo = it
            }
        }
    }
}
