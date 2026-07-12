package com.fabian.downloader.services

import com.fabian.downloader.utils.Config
import android.util.Log
import com.fabian.downloader.services.sites.SiteServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ExtractionService {
    
    private val client = com.fabian.downloader.network.NetworkClient.okHttpClient

    data class ExtractedVideo(
        val title: String,
        val availableFormats: List<String>,
        val size: String,
        val thumbnailUrl: String? = null,
        val formatSizes: Map<String, Double> = emptyMap(),
        val platformId: String = "generic",
        val platformName: String = "Enlace Directo",
        val brandColorHex: String = "#607D8B"
    )
    
    private fun extractYoutubeVideoId(url: String): String? {
        val pattern = "(?:youtube\\.com/(?:[^/]+/\\S+/|(?:v|e(?:mbed)?)/|shorts/|watch\\?v=|\\S*?[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})"
        val regex = Regex(pattern)
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.getOrNull(1)
    }

    private fun getOEmbedInfo(videoUrl: String, downloadId: Long? = null): Pair<String, String?>? {
        try {
            val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
            val oEmbedUrl = when {
                videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be") -> 
                    Config.YT_OEMBED_URL.format(encodedUrl)
                videoUrl.contains("tiktok.com") -> 
                    Config.TIKTOK_OEMBED_URL.format(encodedUrl)
                else -> null
            } ?: return null

            val request = Request.Builder()
                .url(oEmbedUrl)
                .addHeader("User-Agent", Config.UA_DESKTOP)
                .build()
            
            val call = client.newCall(request)
            if (downloadId != null) {
                com.fabian.downloader.services.DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).registerActiveCall(downloadId, call)
            }
            
            try {
                call.execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        if (body.isNotEmpty()) {
                            val json = JSONObject(body)
                            val title = json.optString("title", "")
                            val thumbnail = json.optString("thumbnail_url", "")
                            if (title.isNotEmpty()) {
                                return Pair(title, thumbnail.takeIf { it.isNotEmpty() })
                            }
                        }
                    }
                }
            } finally {
                if (downloadId != null) {
                    com.fabian.downloader.services.DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).unregisterActiveCall(downloadId)
                }
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_EXTRACTION_SERVICE, "oEmbed failed for $videoUrl", e)
        }
        return null
    }

    private fun scrapeHtmlMetadata(videoUrl: String, downloadId: Long? = null): Pair<String, String?>? {
        val userAgents = listOf(
            Config.UA_FACEBOOK,
            Config.UA_GOOGLEBOT,
            Config.UA_DESKTOP
        )

        for (userAgent in userAgents) {
            try {
                val request = Request.Builder()
                    .url(videoUrl)
                    .addHeader("User-Agent", userAgent)
                    .addHeader("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                    .build()
                
                val call = client.newCall(request)
                if (downloadId != null) {
                    com.fabian.downloader.services.DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).registerActiveCall(downloadId, call)
                }
                
                try {
                    call.execute().use { response ->
                        if (response.isSuccessful) {
                            val html = response.body?.string() ?: ""
                            if (html.isNotEmpty()) {
                                // Intentar buscar og:title o twitter:title o name="title"
                                var title = extractMetaTagContent(html, "property=\"og:title\"")
                                if (title.isNullOrEmpty()) {
                                    title = extractMetaTagContent(html, "property='og:title'")
                                }
                                if (title.isNullOrEmpty()) {
                                    title = extractMetaTagContent(html, "name=\"twitter:title\"")
                                }
                                if (title.isNullOrEmpty()) {
                                    title = extractMetaTagContent(html, "property=\"twitter:title\"")
                                }
                                if (title.isNullOrEmpty()) {
                                    title = extractMetaTagContent(html, "name=\"title\"")
                                }
                                // Fallback al tag <title>
                                if (title.isNullOrEmpty()) {
                                    val titleRegex = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE)
                                    val match = titleRegex.find(html)
                                    title = match?.groupValues?.getOrNull(1)?.trim()
                                }
                                
                                // Decodificar entidades HTML comunes
                                title = title?.let {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                        android.text.Html.fromHtml(it, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                                    } else {
                                        @Suppress("DEPRECATION")
                                        android.text.Html.fromHtml(it).toString().trim()
                                    }
                                }

                                // Intentar buscar og:image para la miniatura
                                var thumbnail = extractMetaTagContent(html, "property=\"og:image\"")
                                if (thumbnail.isNullOrEmpty()) {
                                    thumbnail = extractMetaTagContent(html, "property='og:image'")
                                }
                                if (thumbnail.isNullOrEmpty()) {
                                    thumbnail = extractMetaTagContent(html, "name=\"twitter:image\"")
                                }
                                if (thumbnail.isNullOrEmpty()) {
                                    thumbnail = extractMetaTagContent(html, "property=\"twitter:image\"")
                                }

                                val lowerTitle = title?.lowercase() ?: ""
                                val isInvalid = lowerTitle.isEmpty() || 
                                                lowerTitle == "instagram" || 
                                                lowerTitle.contains("inicia sesión") || 
                                                lowerTitle.contains("log in") || 
                                                lowerTitle.contains("login") || 
                                                lowerTitle.contains("redirecting")

                                if (!isInvalid) {
                                    return Pair(title!!, thumbnail?.takeIf { it.isNotEmpty() })
                                }
                            }
                        }
                    }
                } finally {
                    if (downloadId != null) {
                        com.fabian.downloader.services.DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).unregisterActiveCall(downloadId)
                    }
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_EXTRACTION_SERVICE, "HTML scraping with $userAgent failed for $videoUrl", e)
            }
        }
        return null
    }

    private fun extractMetaTagContent(html: String, identifier: String): String? {
        try {
            // Buscamos todas las etiquetas <meta ...>
            val metaRegex = Regex("<meta\\s+[^>]*>", RegexOption.IGNORE_CASE)
            val matches = metaRegex.findAll(html)
            for (match in matches) {
                val metaTag = match.value
                // Limpiar comillas para buscar el identificador sin importar si tiene simples o dobles
                val cleanIdentifier = identifier.replace("\"", "").replace("'", "")
                val cleanMetaTag = metaTag.replace("\"", "").replace("'", "")
                if (cleanMetaTag.contains(cleanIdentifier, ignoreCase = true)) {
                    // Ahora extraigamos el valor de 'content' con soporte para comillas simples y dobles
                    val contentRegex = Regex("content\\s*=\\s*\"([^\"]*)\"|content\\s*=\\s*'([^']*)'", RegexOption.IGNORE_CASE)
                    val contentMatch = contentRegex.find(metaTag)
                    val contentValue = contentMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: contentMatch?.groupValues?.getOrNull(2)
                    if (contentValue != null) {
                        return contentValue.trim()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_EXTRACTION_SERVICE, "Error extracting meta tag $identifier", e)
        }
        return null
    }

    companion object {
        // Usamos un caché estático para que se comparta globalmente entre llamadas y pantallas,
        // ahorrando la ejecución de yt-dlp que es sumamente pesada.
        private val metadataCache = java.util.concurrent.ConcurrentHashMap<String, InfoMedia>()
        private val videoCache = java.util.concurrent.ConcurrentHashMap<String, ExtractedVideo>()

        // Cachés individuales para optimización y rapidez
        private val titleCache = java.util.concurrent.ConcurrentHashMap<String, String>()
        private val thumbnailCache = java.util.concurrent.ConcurrentHashMap<String, String?>()
        private val sizeCache = java.util.concurrent.ConcurrentHashMap<String, Map<String, Double>>()
    }

    suspend fun extractTitle(url: String, downloadId: Long? = null): String = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()

        // 1. Check title cache
        titleCache[cleanUrl]?.let { return@withContext it }

        // 2. Check full video or metadata caches
        videoCache[cleanUrl]?.title?.let {
            titleCache[cleanUrl] = it
            return@withContext it
        }
        metadataCache[cleanUrl]?.titulo?.let {
            if (it != Config.STATUS_UNKNOWN && it.isNotEmpty()) {
                titleCache[cleanUrl] = it
                return@withContext it
            }
        }

        // 3. Try super fast oEmbed
        val oEmbed = getOEmbedInfo(cleanUrl, downloadId)
        if (oEmbed != null) {
            titleCache[cleanUrl] = oEmbed.first
            oEmbed.second?.let { thumbnailCache[cleanUrl] = it }
            return@withContext oEmbed.first
        }

        // 4. Try HTML Scraping
        val scraped = scrapeHtmlMetadata(cleanUrl, downloadId)
        if (scraped != null) {
            titleCache[cleanUrl] = scraped.first
            scraped.second?.let { thumbnailCache[cleanUrl] = it }
            return@withContext scraped.first
        }

        // 5. Run yt-dlp (metadata extraction) - only as fallback since it's slower
        val service = SiteServiceProvider.getServiceForUrl(cleanUrl)
        try {
            val info = service.extractMetadata(cleanUrl)
            if (info != null) {
                metadataCache[cleanUrl] = info
                if (info.titulo != Config.STATUS_UNKNOWN && info.titulo.isNotEmpty()) {
                    titleCache[cleanUrl] = info.titulo
                    return@withContext info.titulo
                }
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_EXTRACTION_SERVICE, "Failed to extract title with yt-dlp for $cleanUrl", e)
        }

        // 6. Generic Fallback
        val fallback = if (service.siteId == "generic") "Enlace Directo" else "Video de ${service.displayName}"
        titleCache[cleanUrl] = fallback
        return@withContext fallback
    }

    suspend fun extractThumbnail(url: String, downloadId: Long? = null): String? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()

        // 1. Check thumbnail cache
        if (thumbnailCache.containsKey(cleanUrl)) {
            return@withContext thumbnailCache[cleanUrl]
        }

        // 2. Check full video or metadata caches
        videoCache[cleanUrl]?.thumbnailUrl?.let {
            thumbnailCache[cleanUrl] = it
            return@withContext it
        }
        metadataCache[cleanUrl]?.miniaturaUrl?.let {
            if (it.isNotEmpty()) {
                thumbnailCache[cleanUrl] = it
                return@withContext it
            }
        }

        val ytId = extractYoutubeVideoId(cleanUrl)
        val fallbackThumbnail = if (ytId != null) Config.YT_THUMBNAIL_URL.format(ytId) else null

        // 3. Try super fast oEmbed
        val oEmbed = getOEmbedInfo(cleanUrl, downloadId)
        if (oEmbed != null) {
            titleCache[cleanUrl] = oEmbed.first
            val thumb = oEmbed.second ?: fallbackThumbnail
            thumbnailCache[cleanUrl] = thumb
            return@withContext thumb
        }

        // 4. Try HTML Scraping
        val scraped = scrapeHtmlMetadata(cleanUrl, downloadId)
        if (scraped != null) {
            titleCache[cleanUrl] = scraped.first
            val thumb = scraped.second ?: fallbackThumbnail
            thumbnailCache[cleanUrl] = thumb
            return@withContext thumb
        }

        // 5. Run yt-dlp (metadata extraction) - only as fallback
        val service = SiteServiceProvider.getServiceForUrl(cleanUrl)
        try {
            val info = service.extractMetadata(cleanUrl)
            if (info != null) {
                metadataCache[cleanUrl] = info
                if (info.miniaturaUrl.isNotEmpty()) {
                    thumbnailCache[cleanUrl] = info.miniaturaUrl
                    return@withContext info.miniaturaUrl
                }
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_EXTRACTION_SERVICE, "Failed to extract thumbnail with yt-dlp for $cleanUrl", e)
        }

        thumbnailCache[cleanUrl] = fallbackThumbnail
        return@withContext fallbackThumbnail
    }

    suspend fun extractFormatSizes(url: String, downloadId: Long? = null): Map<String, Double> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()

        // 1. Check size cache
        sizeCache[cleanUrl]?.let { return@withContext it }

        // 2. Check metadata or video cache
        metadataCache[cleanUrl]?.formatSizes?.let {
            if (it.isNotEmpty()) {
                sizeCache[cleanUrl] = it
                return@withContext it
            }
        }
        videoCache[cleanUrl]?.formatSizes?.let {
            if (it.isNotEmpty()) {
                sizeCache[cleanUrl] = it
                return@withContext it
            }
        }

        // 3. Extract metadata using yt-dlp (required for exact sizes)
        val service = SiteServiceProvider.getServiceForUrl(cleanUrl)
        try {
            val info = service.extractMetadata(cleanUrl)
            if (info != null) {
                metadataCache[cleanUrl] = info
                val sizes = info.formatSizes
                sizeCache[cleanUrl] = sizes
                return@withContext sizes
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_EXTRACTION_SERVICE, "Failed to extract format sizes with yt-dlp for $cleanUrl", e)
        }

        return@withContext emptyMap()
    }

    suspend fun extractVideoInfo(url: String, downloadId: Long? = null): ExtractedVideo = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()

        // Check videoCache first
        videoCache[cleanUrl]?.let { return@withContext it }

        val service = SiteServiceProvider.getServiceForUrl(cleanUrl)

        val title = extractTitle(cleanUrl, downloadId)
        val thumbnailUrl = extractThumbnail(cleanUrl, downloadId)
        val formatSizes = extractFormatSizes(cleanUrl, downloadId)

        val maxMb = formatSizes.values.maxOrNull() ?: 0.0
        val sizeStr = if (maxMb > 0.0) String.format(java.util.Locale.US, "%.1f MB", maxMb) else "Auto"

        val res = ExtractedVideo(
            title = title,
            availableFormats = listOf(Config.FORMAT_MP4, Config.FORMAT_MP3, Config.FORMAT_M4A),
            size = sizeStr,
            thumbnailUrl = thumbnailUrl,
            formatSizes = formatSizes,
            platformId = service.siteId,
            platformName = service.displayName,
            brandColorHex = service.brandColorHex
        )
        videoCache[cleanUrl] = res
        return@withContext res
    }

    suspend fun getRealSizeAndUrl(url: String, quality: String, format: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        val service = SiteServiceProvider.getServiceForUrl(cleanUrl)
        
        // 1. Intentar buscar en el caché de InfoMedia primero para evitar llamadas a la red redundantes
        var info = metadataCache[cleanUrl]
        if (info == null) {
            val cachedVideo = videoCache[cleanUrl]
            if (cachedVideo != null && cachedVideo.formatSizes.isNotEmpty()) {
                val sizeForQuality = cachedVideo.formatSizes[quality] ?: cachedVideo.formatSizes[quality.lowercase()]
                if (sizeForQuality != null && sizeForQuality > 0) {
                    return@withContext Pair(String.format(java.util.Locale.US, "%.1f MB", sizeForQuality), cleanUrl)
                }
            }
            
            // Si por alguna razón no está en caché, hacemos la extracción
            info = service.extractMetadata(cleanUrl)
            if (info != null) {
                metadataCache[cleanUrl] = info
            }
        }

        if (info != null) {
            val formatSizes = info.formatSizes
            val sizeForQuality = formatSizes[quality] ?: formatSizes[quality.lowercase()]
            if (sizeForQuality != null && sizeForQuality > 0) {
                return@withContext Pair(String.format(java.util.Locale.US, "%.1f MB", sizeForQuality), cleanUrl)
            }
            if (info.pesoEstimadoMB > 0) {
                return@withContext Pair(String.format(java.util.Locale.US, "%.1f MB", info.pesoEstimadoMB), cleanUrl)
            }
        }
        return@withContext Pair("Auto", cleanUrl)
    }
}
