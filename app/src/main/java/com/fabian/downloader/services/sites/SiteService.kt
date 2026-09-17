package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.fabian.downloader.services.InfoMedia
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/**
 * Capacidades específicas soportadas por cada sitio o plataforma.
 */
enum class SiteCapability {
    AUDIO_EXTRACTION,
    VIDEO_MULTI_QUALITY,
    PLAYLISTS,
    SUBTITLES,
    EMBED_METADATA,
    FAST_HTTP_STREAM,
    CUSTOM_USER_AGENT,
    WATERMARK_REMOVAL
}

/**
 * Interfaz unificada de servicio de sitio para el nuevo sistema de descarga.
 * Define la identidad, capacidades, reglas de enlace y personalización
 * para extracción y descarga por plataforma.
 */
interface SiteService {
    val siteId: String
    val displayName: String
    val brandColorHex: String
    val iconName: String
    val supportedUrlPatterns: List<String>
    
    val capabilities: Set<SiteCapability>
        get() = setOf(
            SiteCapability.AUDIO_EXTRACTION,
            SiteCapability.VIDEO_MULTI_QUALITY,
            SiteCapability.EMBED_METADATA
        )

    val supportedFormats: List<String>
        get() = listOf(Config.FORMAT_MP4, Config.FORMAT_MP3, Config.FORMAT_M4A)

    val supportedQualities: List<String>
        get() = listOf("1080p", "720p", "480p", "360p", Config.QUALITY_BEST)

    val defaultQuality: String
        get() = "720p"

    val customUserAgent: String?
        get() = null

    val customReferer: String?
        get() = null

    fun cleanUrl(url: String): String = url

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

    fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {}

    fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {}

    suspend fun extractMetadata(url: String): InfoMedia?

    suspend fun download(
        url: String,
        quality: String,
        format: String,
        destFolder: File,
        fileNameWithoutExt: String,
        processId: String?,
        onProgress: (progress: Float, sizeText: String, speedText: String) -> Unit
    ): Boolean
}

