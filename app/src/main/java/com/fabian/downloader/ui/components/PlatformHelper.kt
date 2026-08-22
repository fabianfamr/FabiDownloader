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
        lowerUrl.contains("dailymotion.com") -> 
            Pair(PlatformIcons.Dailymotion, Color(0xFF0066DC))
        lowerUrl.contains("bilibili.com") -> 
            Pair(PlatformIcons.Bilibili, Color(0xFF00A1D6))
        lowerUrl.contains("tumblr.com") -> 
            Pair(PlatformIcons.Tumblr, Color(0xFF36465D))
        lowerUrl.contains("vk.com") -> 
            Pair(PlatformIcons.VK, Color(0xFF0077FF))
        lowerUrl.contains("rumble.com") -> 
            Pair(PlatformIcons.Rumble, Color(0xFF85C742))
        lowerUrl.contains("snapchat.com") -> 
            Pair(PlatformIcons.Snapchat, Color(0xFFFFFC00))
        lowerUrl.contains("threads.net") -> 
            Pair(PlatformIcons.Threads, Color(0xFF000000))
        lowerUrl.contains("patreon.com") -> 
            Pair(PlatformIcons.Patreon, Color(0xFFFF424D))
        lowerUrl.contains("bandcamp.com") -> 
            Pair(PlatformIcons.Bandcamp, Color(0xFF629AA9))
        lowerUrl.contains("mixcloud.com") -> 
            Pair(PlatformIcons.Mixcloud, Color(0xFF5000FF))
        lowerUrl.contains("dropbox.com") -> 
            Pair(PlatformIcons.Dropbox, Color(0xFF0061FF))
        lowerUrl.contains("drive.google.com") -> 
            Pair(PlatformIcons.GoogleDrive, Color(0xFF4285F4))
        lowerUrl.contains("t.me") || lowerUrl.contains("telegram.org") -> 
            Pair(PlatformIcons.Telegram, Color(0xFF26A5E4))
        lowerUrl.contains("whatsapp.com") -> 
            Pair(PlatformIcons.WhatsApp, Color(0xFF25D366))
        lowerUrl.contains("discord.com") || lowerUrl.contains("discordapp.com") -> 
            Pair(PlatformIcons.Discord, Color(0xFF5865F2))
        lowerUrl.contains("imgur.com") -> 
            Pair(PlatformIcons.Imgur, Color(0xFF1BB76E))
        lowerUrl.contains("flickr.com") -> 
            Pair(PlatformIcons.Flickr, Color(0xFF0063DC))
        lowerUrl.contains("giphy.com") -> 
            Pair(PlatformIcons.Giphy, Color(0xFF000000))
        lowerUrl.contains("music.apple.com") -> 
            Pair(PlatformIcons.AppleMusic, Color(0xFFFA243C))
        lowerUrl.contains("odysee.com") -> 
            Pair(PlatformIcons.Odysee, Color(0xFFE21B4D))
        lowerUrl.contains("mastodon.social") || lowerUrl.contains("joinmastodon.org") -> 
            Pair(PlatformIcons.Mastodon, Color(0xFF6364FF))
        lowerUrl.contains("spotify.com") -> 
            Pair(PlatformIcons.Spotify, Color(0xFF1DB954))
        else -> 
            Pair(if (isAudio) Icons.Default.Audiotrack else Icons.Default.OndemandVideo, Color(0xFF2979FF))
    }
}
