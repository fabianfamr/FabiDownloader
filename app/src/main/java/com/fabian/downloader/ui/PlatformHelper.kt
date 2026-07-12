package com.fabian.downloader.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun getPlatformIconAndColor(url: String, format: String): Pair<ImageVector, Color> {
    val lowerUrl = url.lowercase()
    val upperFormat = format.uppercase()
    val isAudio = upperFormat.contains(com.fabian.downloader.utils.Config.FORMAT_MP3) || upperFormat.contains(com.fabian.downloader.utils.Config.FORMAT_M4A) || upperFormat.contains(com.fabian.downloader.utils.Config.FORMAT_OGG) || upperFormat.contains(com.fabian.downloader.utils.Config.FORMAT_WAV)
    
    return when {
        lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") || lowerUrl.contains("shorts") -> 
            Pair(Icons.Default.PlayCircle, Color(0xFFFF0000))
        lowerUrl.contains("instagram.com") -> 
            Pair(Icons.Default.CameraAlt, Color(0xFFE1306C))
        lowerUrl.contains("facebook.com") || lowerUrl.contains("fb.watch") -> 
            Pair(Icons.Default.Facebook, Color(0xFF1877F2))
        lowerUrl.contains("tiktok.com") -> 
            Pair(Icons.Default.Album, Color(0xFF010101))
        lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") -> 
            Pair(Icons.Default.Share, Color(0xFF000000))
        lowerUrl.contains("twitch.tv") -> 
            Pair(Icons.Default.VideogameAsset, Color(0xFF9146FF))
        lowerUrl.contains("kick.com") -> 
            Pair(Icons.Default.LiveTv, Color(0xFF53FC18))
        else -> 
            Pair(if (isAudio) Icons.Default.Audiotrack else Icons.Default.OndemandVideo, Color(0xFF2979FF))
    }
}
