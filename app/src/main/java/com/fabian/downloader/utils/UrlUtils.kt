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

    fun isTikTokUrl(url: String): Boolean = isTiktokUrl(url)

    fun isRedditUrl(url: String): Boolean {
        try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            return host == "reddit.com" || host.endsWith(".reddit.com") || host == "redd.it"
        } catch (e: Exception) {
            return false
        }
    }

    fun cleanTrackingParams(url: String): String {
        return com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(url)
    }

    fun isTwitterUrl(url: String): Boolean {
        try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            return host == "twitter.com" || host.endsWith(".twitter.com") ||
                   host == "x.com" || host.endsWith(".x.com")
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Extrae una URL válida de cualquier texto (incluso si contiene saltos de línea,
     * encabezados como '========', o texto adicional al compartir desde redes sociales).
     */
    fun extractUrlFromText(text: String): String {
        val trimmed = text.trim()
        val regex = Regex("""https?://[^\s<>"']+""")
        val match = regex.find(trimmed)?.value
        return if (match != null) {
            com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(match)
        } else {
            // Si no contiene http/https pero es texto multilinea, limpiar saltos de linea
            trimmed.lines().firstOrNull { it.isNotBlank() && !it.startsWith("===") }?.trim() ?: trimmed
        }
    }
}
