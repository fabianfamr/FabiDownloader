package com.fabian.downloader.services.sites

import com.fabian.downloader.services.InfoMedia
import java.io.File

interface SiteService {
    val siteId: String
    val displayName: String
    val brandColorHex: String
    val iconName: String
    val supportedUrlPatterns: List<String>

    fun canHandle(url: String): Boolean {
        try {
            val host = java.net.URI(url).host?.lowercase() ?: return false
            return supportedUrlPatterns.any { pattern ->
                host == pattern.lowercase() || host.endsWith(".$pattern", ignoreCase = true)
            }
        } catch (e: Exception) {
            return supportedUrlPatterns.any { pattern ->
                url.contains(pattern, ignoreCase = true)
            }
        }
    }

    suspend fun extractMetadata(url: String): InfoMedia?

    suspend fun download(
        url: String,
        quality: String,
        format: String,
        destFolder: File,
        fileNameWithoutExt: String, processId: String?,
        onProgress: (progress: Float, sizeText: String, speedText: String) -> Unit
    ): Boolean
}
