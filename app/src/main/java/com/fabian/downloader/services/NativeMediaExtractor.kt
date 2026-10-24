package com.fabian.downloader.services

import android.util.Log
import com.fabian.downloader.configs.Config
import com.fabian.downloader.network.NetworkClient
import com.fabian.downloader.utils.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object NativeMediaExtractor {
    private const val TAG = "NativeMediaExtractor"
    private val client = NetworkClient.okHttpClient

    data class StreamFormat(
        val itag: Int = 0,
        val quality: String,
        val format: String,
        val url: String?,
        val height: Int = 0,
        val approximateSizeMb: Double = 0.0,
        val isVideo: Boolean = true,
        val isAudio: Boolean = true
    )

    data class NativeVideoDetails(
        val title: String,
        val author: String,
        val thumbnailUrl: String?,
        val durationSeconds: Long = 0,
        val streamFormats: List<StreamFormat> = emptyList(),
        val formatSizes: Map<String, Double> = emptyMap()
    )

    /**
     * Intenta extraer metadatos y streams directamente usando nuestros extractores nativos HTTP (sin Python).
     */
    suspend fun extractNatively(url: String): NativeVideoDetails? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        try {
            when {
                UrlUtils.isYoutubeUrl(cleanUrl) -> extractYoutubeInnerTube(cleanUrl)
                UrlUtils.isTwitterUrl(cleanUrl) -> extractTwitterSyndication(cleanUrl)
                UrlUtils.isTikTokUrl(cleanUrl) -> extractTikTokDirectOrOembed(cleanUrl)
                UrlUtils.isInstagramUrl(cleanUrl) -> extractInstagramDirect(cleanUrl)
                UrlUtils.isRedditUrl(cleanUrl) -> extractRedditOembed(cleanUrl)
                isDirectMediaLink(cleanUrl) -> extractDirectMediaLink(cleanUrl)
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en extracción nativa para $cleanUrl: ${e.message}")
            null
        }
    }

    private fun extractInstagramDirect(url: String): NativeVideoDetails? {
        val shortcodeRegex = Regex("""(?:p|reel|tv)/([a-zA-Z0-9_-]+)""")
        val shortcode = shortcodeRegex.find(url)?.groupValues?.getOrNull(1) ?: return null
        val endpoints = listOf(
            "https://www.instagram.com/p/$shortcode/?__a=1&__d=dis",
            "https://www.instagram.com/reel/$shortcode/?__a=1&__d=dis"
        )

        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("User-Agent", Config.UA_INSTAGRAM)
                    .addHeader("Accept", "*/*")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)

                    // Estructura 1: items[0]
                    val items = json.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        val firstItem = items.getJSONObject(0)
                        val caption = firstItem.optJSONObject("caption")?.optString("text", "Instagram Video") ?: "Instagram Video"
                        val user = firstItem.optJSONObject("user")?.optString("username", "Instagram") ?: "Instagram"
                        val videoVersions = firstItem.optJSONArray("video_versions")
                        val thumb = firstItem.optJSONObject("image_versions2")?.optJSONArray("candidates")?.optJSONObject(0)?.optString("url")

                        if (videoVersions != null && videoVersions.length() > 0) {
                            val bestVideo = videoVersions.getJSONObject(0)
                            val directUrl = bestVideo.optString("url")
                            val width = bestVideo.optInt("width", 720)
                            val height = bestVideo.optInt("height", 1280)
                            val qualityLabel = if (height >= 1080 || width >= 1080) "1080p" else "720p"

                            val stream = StreamFormat(
                                quality = qualityLabel,
                                format = "MP4",
                                url = directUrl,
                                height = height,
                                approximateSizeMb = 15.0,
                                isVideo = true,
                                isAudio = true
                            )
                            return NativeVideoDetails(
                                title = caption.lines().firstOrNull()?.take(60) ?: "Instagram Video",
                                author = user,
                                thumbnailUrl = thumb,
                                streamFormats = listOf(stream),
                                formatSizes = mapOf(qualityLabel to 15.0, "Original" to 15.0)
                            )
                        }
                    }

                    // Estructura 2: graphql.shortcode_media
                    val media = json.optJSONObject("graphql")?.optJSONObject("shortcode_media")
                    if (media != null && media.optBoolean("is_video", false)) {
                        val directUrl = media.optString("video_url")
                        val thumb = media.optString("display_url")
                        val owner = media.optJSONObject("owner")?.optString("username", "Instagram") ?: "Instagram"
                        if (directUrl.isNotBlank()) {
                            val stream = StreamFormat(
                                quality = "720p",
                                format = "MP4",
                                url = directUrl,
                                approximateSizeMb = 12.0,
                                isVideo = true,
                                isAudio = true
                            )
                            return NativeVideoDetails(
                                title = "Video de $owner",
                                author = owner,
                                thumbnailUrl = thumb,
                                streamFormats = listOf(stream),
                                formatSizes = mapOf("720p" to 12.0)
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun extractTikTokDirectOrOembed(url: String): NativeVideoDetails? {
        // Intento 1: Extractor directo rápido (TikWM / endpoint público)
        try {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val tikwmEndpoint = "https://www.tikwm.com/api/?url=$encodedUrl"
            val request = Request.Builder()
                .url(tikwmEndpoint)
                .addHeader("User-Agent", Config.UA_TIKTOK_MOBILE)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val data = json.optJSONObject("data")
                        if (data != null) {
                            val title = data.optString("title", "TikTok Video").take(70)
                            val author = data.optJSONObject("author")?.optString("nickname", "TikTok") ?: "TikTok"
                            val thumb = data.optString("cover")
                            val playUrl = data.optString("play")
                            val hdPlayUrl = data.optString("hdplay").ifEmpty { playUrl }
                            val sizeBytes = data.optLong("size", 0L)
                            val sizeMb = if (sizeBytes > 0) sizeBytes / (1024.0 * 1024.0) else 10.0

                            val streams = mutableListOf<StreamFormat>()
                            if (hdPlayUrl.isNotBlank()) {
                                streams.add(
                                    StreamFormat(
                                        quality = "HD (Sin Marca)",
                                        format = "MP4",
                                        url = hdPlayUrl,
                                        approximateSizeMb = sizeMb,
                                        isVideo = true,
                                        isAudio = true
                                    )
                                )
                            }
                            if (playUrl.isNotBlank() && playUrl != hdPlayUrl) {
                                streams.add(
                                    StreamFormat(
                                        quality = "720p",
                                        format = "MP4",
                                        url = playUrl,
                                        approximateSizeMb = sizeMb * 0.8,
                                        isVideo = true,
                                        isAudio = true
                                    )
                                )
                            }

                            if (streams.isNotEmpty()) {
                                return NativeVideoDetails(
                                    title = title,
                                    author = author,
                                    thumbnailUrl = thumb,
                                    durationSeconds = data.optLong("duration", 0L),
                                    streamFormats = streams,
                                    formatSizes = mapOf("HD (Sin Marca)" to sizeMb, "720p" to sizeMb)
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: oEmbed estándar
        return extractTikTokOembed(url)
    }

    private fun extractTikTokOembed(url: String): NativeVideoDetails? {
        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
        val endpoint = "https://www.tiktok.com/oembed?url=$encodedUrl"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("User-Agent", Config.UA_TIKTOK_MOBILE)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val title = json.optString("title", "Video de TikTok")
            val author = json.optString("author_name", "TikTok")
            val thumb = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
            return NativeVideoDetails(
                title = title.take(80),
                author = author,
                thumbnailUrl = thumb,
                durationSeconds = 0,
                streamFormats = emptyList(),
                formatSizes = emptyMap()
            )
        }
    }

    private fun extractRedditOembed(url: String): NativeVideoDetails? {
        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
        val endpoint = "https://www.reddit.com/oembed?url=$encodedUrl"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("User-Agent", Config.UA_DEFAULT_CHROME_WINDOWS)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val title = json.optString("title", "Publicación de Reddit")
            val author = json.optString("author_name", "Reddit")
            val thumb = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
            return NativeVideoDetails(
                title = title.take(80),
                author = author,
                thumbnailUrl = thumb,
                durationSeconds = 0,
                streamFormats = emptyList(),
                formatSizes = emptyMap()
            )
        }
    }

    private fun isDirectMediaLink(url: String): Boolean {
        val path = url.substringBefore('?').lowercase()
        return Config.VALID_EXTENSIONS.any { ext -> path.endsWith(".$ext") }
    }

    /**
     * Extracción nativa directa para enlaces de medios directos (.mp4, .mp3, etc.)
     */
    private fun extractDirectMediaLink(url: String): NativeVideoDetails? {
        val cleanPath = url.substringBefore('?')
        val fileName = cleanPath.substringAfterLast('/').ifEmpty { "media" }
        val ext = fileName.substringAfterLast('.', "mp4").uppercase()
        val isAudio = Config.AUDIO_EXTENSIONS.any { it.equals(ext, ignoreCase = true) }

        var sizeMb = 0.0
        try {
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .addHeader("User-Agent", Config.UA_DESKTOP)
                .build()
            client.newCall(headRequest).execute().use { response ->
                val len = response.header("Content-Length")?.toLongOrNull() ?: 0L
                if (len > 0L) {
                    sizeMb = len / (1024.0 * 1024.0)
                }
            }
        } catch (_: Exception) {}

        val qualityLabel = if (isAudio) "Audio" else "Original"
        val formatStream = StreamFormat(
            quality = qualityLabel,
            format = ext,
            url = url,
            approximateSizeMb = sizeMb,
            isVideo = !isAudio,
            isAudio = true
        )

        val formatSizes = if (sizeMb > 0) mapOf(qualityLabel to sizeMb, ext to sizeMb) else emptyMap()

        return NativeVideoDetails(
            title = fileName.substringBeforeLast('.'),
            author = "Direct Link",
            thumbnailUrl = null,
            streamFormats = listOf(formatStream),
            formatSizes = formatSizes
        )
    }

    /**
     * Extracción nativa de Twitter/X usando la API de sindicación pública.
     */
    private fun extractTwitterSyndication(url: String): NativeVideoDetails? {
        val tweetIdRegex = Regex("""status/(\d+)""")
        val tweetId = tweetIdRegex.find(url)?.groupValues?.getOrNull(1) ?: return null
        val syndicationUrl = "https://cdn.syndication.twimg.com/tweet-result?id=$tweetId&lang=en"

        val request = Request.Builder()
            .url(syndicationUrl)
            .addHeader("User-Agent", Config.UA_DESKTOP)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            val text = json.optString("text", "Video de X / Twitter")
            val user = json.optJSONObject("user")?.optString("name", "X") ?: "X"
            val mediaEntities = json.optJSONArray("mediaDetails") ?: return null

            for (i in 0 until mediaEntities.length()) {
                val media = mediaEntities.getJSONObject(i)
                val type = media.optString("type")
                if (type == "video" || type == "animated_gif") {
                    val videoInfo = media.optJSONObject("video_info")
                    val variants = videoInfo?.optJSONArray("variants") ?: continue
                    val thumb = media.optString("media_url_https").takeIf { it.isNotBlank() }

                    val streamList = mutableListOf<StreamFormat>()
                    val sizesMap = mutableMapOf<String, Double>()

                    for (j in 0 until variants.length()) {
                        val variant = variants.getJSONObject(j)
                        val contentType = variant.optString("content_type")
                        if (contentType == "video/mp4") {
                            val bitrate = variant.optLong("bitrate", 0L)
                            val videoUrl = variant.optString("url")
                            val qualityLabel = when {
                                bitrate > 1_500_000 -> "720p"
                                bitrate > 800_000 -> "480p"
                                else -> "360p"
                            }
                            streamList.add(
                                StreamFormat(
                                    quality = qualityLabel,
                                    format = "MP4",
                                    url = videoUrl,
                                    isVideo = true,
                                    isAudio = true
                                )
                            )
                            sizesMap[qualityLabel] = 12.0 // Estimado
                        }
                    }

                    if (streamList.isNotEmpty()) {
                        return NativeVideoDetails(
                            title = text.take(60),
                            author = user,
                            thumbnailUrl = thumb,
                            streamFormats = streamList,
                            formatSizes = sizesMap
                        )
                    }
                }
            }
        }
        return null
    }

    /**
     * Extracción nativa de YouTube usando el endpoint InnerTube Player de YouTube.
     */
    private fun extractYoutubeInnerTube(url: String): NativeVideoDetails? {
        val videoIdRegex = Regex("""(?:v=|youtu\.be/|shorts/)([a-zA-Z0-9_-]{11})""")
        val videoId = videoIdRegex.find(url)?.groupValues?.getOrNull(1) ?: return null

        val endpoint = "https://www.youtube.com/youtubei/v1/player"
        val payload = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID")
                    put("clientVersion", "19.29.37")
                    put("androidSdkVersion", 30)
                    put("hl", "es")
                    put("gl", "ES")
                })
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "com.google.android.youtube/19.29.37 (Linux; U; Android 11) gzip")
            .addHeader("X-YouTube-Client-Name", "3")
            .addHeader("X-YouTube-Client-Version", "19.29.37")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            val videoDetails = json.optJSONObject("videoDetails") ?: return null
            val title = videoDetails.optString("title", "Video de YouTube")
            val author = videoDetails.optString("author", "YouTube")
            val durationSeconds = videoDetails.optLong("lengthSeconds", 0L)
            val thumbnails = videoDetails.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            val bestThumbnail = if (thumbnails != null && thumbnails.length() > 0) {
                thumbnails.getJSONObject(thumbnails.length() - 1).optString("url")
            } else {
                Config.YT_THUMBNAIL_URL.replace("{ytId}", videoId)
            }

            val streamingData = json.optJSONObject("streamingData")
            val formats = streamingData?.optJSONArray("formats") ?: JSONArray()
            val adaptiveFormats = streamingData?.optJSONArray("adaptiveFormats") ?: JSONArray()

            val streamList = mutableListOf<StreamFormat>()
            val formatSizes = mutableMapOf<String, Double>()

            // Procesar formatos combinados
            for (i in 0 until formats.length()) {
                val fmt = formats.getJSONObject(i)
                val itag = fmt.optInt("itag")
                val qualityLabel = fmt.optString("qualityLabel", "360p")
                val height = fmt.optInt("height", 360)
                val directUrl = fmt.optString("url").takeIf { it.isNotBlank() }
                val contentLength = fmt.optLong("contentLength", 0L)
                val sizeMb = if (contentLength > 0) contentLength / (1024.0 * 1024.0) else 0.0

                if (sizeMb > 0) {
                    formatSizes[qualityLabel] = sizeMb
                }

                streamList.add(
                    StreamFormat(
                        itag = itag,
                        quality = qualityLabel,
                        format = "MP4",
                        url = directUrl,
                        height = height,
                        approximateSizeMb = sizeMb,
                        isVideo = true,
                        isAudio = true
                    )
                )
            }

            // Procesar formatos adaptativos para tamaños de 720p, 1080p, audio
            for (i in 0 until adaptiveFormats.length()) {
                val fmt = adaptiveFormats.getJSONObject(i)
                val qualityLabel = fmt.optString("qualityLabel", "")
                val mimeType = fmt.optString("mimeType", "")
                val contentLength = fmt.optLong("contentLength", 0L)
                val sizeMb = if (contentLength > 0) contentLength / (1024.0 * 1024.0) else 0.0

                if (qualityLabel.isNotEmpty() && sizeMb > 0) {
                    if (!formatSizes.containsKey(qualityLabel) || (formatSizes[qualityLabel] ?: 0.0) < sizeMb) {
                        formatSizes[qualityLabel] = sizeMb
                    }
                } else if (mimeType.startsWith("audio/") && sizeMb > 0) {
                    formatSizes["MP3"] = sizeMb
                    formatSizes["M4A"] = sizeMb
                }
            }

            return NativeVideoDetails(
                title = title,
                author = author,
                thumbnailUrl = bestThumbnail,
                durationSeconds = durationSeconds,
                streamFormats = streamList,
                formatSizes = formatSizes
            )
        }
    }
}
