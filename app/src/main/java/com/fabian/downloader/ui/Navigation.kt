package com.fabian.downloader.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.fabian.downloader.R
import com.fabian.downloader.ui.components.AppIcons

sealed class Screen(val route: String, @param:StringRes val titleRes: Int, val icon: ImageVector) {
    object Main : Screen("main", R.string.main_tab_home, AppIcons.Home)
    object Downloads : Screen("downloads", R.string.main_tab_library, AppIcons.Library)
    object Settings : Screen("settings", R.string.main_tab_settings, AppIcons.Settings)
    object DownloadSettings : Screen("download_settings", R.string.main_tab_download_settings, AppIcons.DownloadSettings)
}
