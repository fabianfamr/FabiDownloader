package com.fabian.downloader.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Main : Screen("main", "Inicio", Icons.Default.Home)
    object Downloads : Screen("downloads", "Biblioteca", Icons.Default.PlayCircleFilled)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
    object DownloadSettings : Screen("download_settings", "Ajustes de descarga", Icons.Default.Settings)
}
