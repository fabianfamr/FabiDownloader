package com.fabian.downloader.services.sites

import com.fabian.downloader.services.InfoMedia
import java.io.File

interface SiteService {
    val siteId: String
    val displayName: String
    val brandColorHex: String
    val iconName: String
    val supportedUrlPatterns: List<String>
    val supportedFormats: List<String> 
        get() = listOf(com.fabian.downloader.configs.Config.FORMAT_MP4, com.fabian.downloader.configs.Config.FORMAT_MP3, com.fabian.downloader.configs.Config.FORMAT_M4A)

    fun canHandle(url: String): Boolean {
        try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            return supportedUrlPatterns.any { pattern ->
                val lowerPattern = pattern.lowercase()
                host == lowerPattern || host.endsWith(".$lowerPattern")
            }
        } catch (e: Exception) {
            return false
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
