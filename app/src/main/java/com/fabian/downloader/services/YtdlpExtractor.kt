package com.fabian.downloader.services

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class InfoMedia(
    val titulo: String,
    val autor: String,
    val miniaturaUrl: String,
    val duracionTexto: String,
    val vistas: String,
    val pesoEstimadoMB: Double,
    val videoId: String = "",
    val formatSizes: Map<String, Double> = emptyMap()
)

class YtdlpExtractor {

    suspend fun obtenerDetallesVideo(videoUrl: String, quality: String? = null, format: String? = null): InfoMedia? = withContext(Dispatchers.IO) {
        com.fabian.downloader.MyApplication.getInstance().waitForInitialization()
        val isYoutube = videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be") || videoUrl.contains("shorts") || videoUrl.contains("music.youtube.com")
        val isInstagram = videoUrl.contains("instagram.com")

        fun createExtractorRequest(playerClient: String?): YoutubeDLRequest {
            return YoutubeDLRequest(videoUrl).apply {
                addOption("--dump-json")
                
                val cookiesFile = java.io.File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
                if (cookiesFile.exists() && cookiesFile.length() > 0) {
                    addOption("--cookies", cookiesFile.absolutePath)
                }
                
                if (!com.fabian.downloader.ui.AppSettings.playlistEnabled) {
                    addOption("--no-playlist")
                }
                addOption("--no-cache-dir")
                
                if (isYoutube) {
                    if (!playerClient.isNullOrEmpty()) {
                        addOption("--extractor-args", "youtube:player-client=$playerClient")
                    }
                    addOption("--user-agent", Config.UA_DESKTOP)
                }
                
                addOption("--no-check-formats")
                addOption("--referer", Config.REFERER_DEFAULT)
                addOption("--force-ipv4")
                addOption("--no-check-certificate")
                addOption("--no-call-home")
                addOption("--geo-bypass")
                addOption("--quiet")
                addOption("--no-warnings")
                addOption("--ignore-errors")
                addOption("--no-mtime")
            }
        }

        val clientOptions: List<String?> = listOf("android,web", "android", "tv,android", "web", null)

        for (client in clientOptions) {
            try {
                val request = createExtractorRequest(client)
                val response = YoutubeDL.getInstance().execute(request)
                val jsonRaw = response.out ?: continue
                val json = JSONObject(jsonRaw)

                val defaultAuthor = if (isInstagram) Config.DEFAULT_AUTHOR_INSTAGRAM else Config.STATUS_UNKNOWN
                return@withContext com.fabian.downloader.utils.YtdlpParser.parseMetadata(json, defaultAuthor)
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_EXTRACTOR, "Error extrayendo con client=$client: ${e.message}")
                val msg = (e.message ?: "").lowercase()
                if (msg.contains("player api") || msg.contains("web player api") || msg.contains("ios") || msg.contains("format") || msg.contains("bot")) {
                    com.fabian.downloader.MyApplication.getInstance().forceUpdateYtdlpBinary(com.fabian.downloader.MyApplication.getInstance())
                }
            }
        }
        return@withContext null
    }

    suspend fun obtenerDetallesPlaylist(playlistUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        com.fabian.downloader.MyApplication.getInstance().waitForInitialization()
        val isYoutube = playlistUrl.contains("youtube.com") || playlistUrl.contains("youtu.be")
        
        val request = YoutubeDLRequest(playlistUrl).apply {
            addOption("--dump-single-json")
            addOption("--flat-playlist")
            
            val cookiesFile = java.io.File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }
            
            addOption("--no-cache-dir")
            
            if (isYoutube) {
                addOption("--extractor-args", "youtube:player-client=android,web")
                addOption("--user-agent", Config.UA_DESKTOP)
            }
            
            addOption("--referer", Config.REFERER_DEFAULT)
            addOption("--force-ipv4")
            addOption("--no-check-certificate")
            addOption("--no-call-home")
            addOption("--geo-bypass")
            addOption("--quiet")
            addOption("--no-warnings")
            addOption("--ignore-errors")
            addOption("--no-mtime")
        }

        try {
            val response = YoutubeDL.getInstance().execute(request)
            val jsonRaw = response.out ?: return@withContext null
            JSONObject(jsonRaw)
        } catch (e: Exception) {
            Log.e(Config.TAG_YTDLP_EXTRACTOR, "Error extracting playlist info: ${e.message}", e)
            null
        }
    }
}
