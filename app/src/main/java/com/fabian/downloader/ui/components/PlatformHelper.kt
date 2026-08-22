package com.fabian.downloader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fabian.downloader.configs.Config

fun getPlatformIconAndColor(url: String, format: String): Pair<ImageVector, Color> {
    val lowerUrl = url.lowercase()
    val upperFormat = format.uppercase()
    val isAudio = upperFormat.contains(Config.FORMAT_MP3) || upperFormat.contains(Config.FORMAT_M4A) || upperFormat.contains(Config.FORMAT_OGG) || upperFormat.contains(Config.FORMAT_WAV)
    
    return when {
        lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") || lowerUrl.contains("shorts") -> 
            Pair(PlatformIcons.YouTube, Color(0xFFFF0000))
        lowerUrl.contains("instagram.com") -> 
            Pair(PlatformIcons.Instagram, Color(0xFFE1306C))
        lowerUrl.contains("facebook.com") || lowerUrl.contains("fb.watch") -> 
            Pair(PlatformIcons.Facebook, Color(0xFF1877F2))
        lowerUrl.contains("tiktok.com") -> 
            Pair(PlatformIcons.TikTok, Color(0xFF010101))
        lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") -> 
            Pair(PlatformIcons.X, Color(0xFF000000))
        lowerUrl.contains("twitch.tv") -> 
            Pair(PlatformIcons.Twitch, Color(0xFF9146FF))
        lowerUrl.contains("kick.com") -> 
            Pair(PlatformIcons.Kick, Color(0xFF53FC18))
        lowerUrl.contains("reddit.com") || lowerUrl.contains("v.redd.it") -> 
            Pair(PlatformIcons.Reddit, Color(0xFFFF4500))
        lowerUrl.contains("pinterest.com") || lowerUrl.contains("pin.it") -> 
            Pair(PlatformIcons.Pinterest, Color(0xFFE60023))
        lowerUrl.contains("vimeo.com") -> 
            Pair(PlatformIcons.Vimeo, Color(0xFF1AB7EA))
        lowerUrl.contains("soundcloud.com") -> 
            Pair(PlatformIcons.SoundCloud, Color(0xFFFF5500))
        else -> 
            Pair(if (isAudio) Icons.Default.Audiotrack else Icons.Default.OndemandVideo, Color(0xFF2979FF))
    }
}
