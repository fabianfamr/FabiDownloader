package com.fabian.downloader.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.fabian.downloader.R

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Main : Screen("main", R.string.main_tab_home, Icons.Default.Home)
    object Downloads : Screen("downloads", R.string.main_tab_library, Icons.Default.PlayCircleFilled)
    object Settings : Screen("settings", R.string.main_tab_settings, Icons.Default.Settings)
    object DownloadSettings : Screen("download_settings", R.string.main_tab_download_settings, Icons.Default.Settings)
}
