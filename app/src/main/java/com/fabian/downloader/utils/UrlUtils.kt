package com.fabian.downloader.utils

import android.net.Uri

object UrlUtils {
    fun isYoutubeUrl(url: String): Boolean {
        try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            return host == "youtube.com" || host.endsWith(".youtube.com") || 
                   host == "youtu.be" || host.endsWith(".youtu.be")
        } catch (e: Exception) {
            return false
        }
    }
    
    fun isInstagramUrl(url: String): Boolean {
        try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            return host == "instagram.com" || host.endsWith(".instagram.com")
        } catch (e: Exception) {
            return false
        }
    }

    fun isTiktokUrl(url: String): Boolean {
        try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            return host == "tiktok.com" || host.endsWith(".tiktok.com") || host == "vm.tiktok.com" || host == "vt.tiktok.com"
        } catch (e: Exception) {
            return false
        }
    }
}
