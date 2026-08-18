package com.fabian.downloader.services

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
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

    suspend fun obtenerDetallesVideo(rawVideoUrl: String, quality: String? = null, format: String? = null): InfoMedia? = withContext(Dispatchers.IO) {
        com.fabian.downloader.MyApplication.getInstance().waitForInitialization()
        val videoUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(rawVideoUrl)
        val lowerUrl = videoUrl.lowercase()
        val isYoutube = com.fabian.downloader.utils.UrlUtils.isYoutubeUrl(videoUrl)
        val isInstagram = com.fabian.downloader.utils.UrlUtils.isInstagramUrl(videoUrl)

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
                        addOption("--extractor-args", "youtube:player_client=$playerClient")
                    }
                    addOption("--user-agent", Config.UA_DESKTOP)
                }
                
                addOption("--no-check-formats")
                addOption("--referer", Config.REFERER_DEFAULT)
                if (com.fabian.downloader.ui.AppSettings.bypassSslVerification) {
                    addOption("--no-check-certificate")
                }
                addOption("--geo-bypass")
                addOption("--quiet")
                addOption("--no-warnings")
                addOption("--ignore-errors")
                addOption("--no-mtime")
            }
        }

        val clientOptions: List<String?> = listOf("android,mweb,ios", "ios,mweb", "tv,android_creator,mweb", "android_creator,ios,tv,web", null)

        for (client in clientOptions) {
            val processId = java.util.UUID.randomUUID().toString()
            try {
                val request = createExtractorRequest(client)
                val response = YoutubeDL.getInstance().execute(request, processId)
                val jsonRaw = response.out ?: continue
                val json = JSONObject(jsonRaw)

                val defaultAuthor = if (isInstagram) Config.DEFAULT_AUTHOR_INSTAGRAM else Config.STATUS_UNKNOWN
                return@withContext com.fabian.downloader.utils.YtdlpParser.parseMetadata(json, defaultAuthor)
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_EXTRACTOR, "Error extrayendo con client=$client: ${e.message}")
                val msg = (e.message ?: "").lowercase()
                if (msg.contains("zipimport") || msg.contains("bad local file header") ||
                    msg.contains("cannot link") || msg.contains("libandroid-support") ||
                    msg.contains("libpython") || msg.contains("not found")) {
                    Log.w(Config.TAG_YTDLP_EXTRACTOR, "Binario de yt-dlp corrupto. Reseteando desde APK assets...")
                    val appCtx = com.fabian.downloader.MyApplication.getInstance()
                    appCtx.resetAndReinitYtdlp(appCtx)
                } else if (msg.contains("player api") || msg.contains("web player api") || msg.contains("player client") ||
                    msg.contains("requested format") || msg.contains("format is not available") ||
                    msg.contains("no video formats found") || msg.contains("quickjs") || msg.contains("bot")) {
                    com.fabian.downloader.MyApplication.getInstance().forceUpdateYtdlpBinary(com.fabian.downloader.MyApplication.getInstance())
                }
            } finally {
                if (!isActive) {
                    try { YoutubeDL.getInstance().destroyProcessById(processId) } catch (_: Exception) {}
                }
            }
        }
        Log.w(Config.TAG_YTDLP_EXTRACTOR, "No se pudo extraer metadatos para: $videoUrl")
        return@withContext null
    }

    suspend fun obtenerDetallesPlaylist(rawPlaylistUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        com.fabian.downloader.MyApplication.getInstance().waitForInitialization()
        val playlistUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(rawPlaylistUrl, keepPlaylistParams = true)
        val isYoutube = com.fabian.downloader.utils.UrlUtils.isYoutubeUrl(playlistUrl)
        
        val request = YoutubeDLRequest(playlistUrl).apply {
            addOption("--dump-single-json")
            addOption("--flat-playlist")
            
            val cookiesFile = java.io.File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }
            
            addOption("--no-cache-dir")
            
            if (isYoutube) {
                addOption("--extractor-args", "youtube:player_client=android,mweb,ios")
                addOption("--user-agent", Config.UA_DESKTOP)
            }
            
            addOption("--referer", Config.REFERER_DEFAULT)
            if (com.fabian.downloader.ui.AppSettings.bypassSslVerification) {
                addOption("--no-check-certificate")
            }
            addOption("--geo-bypass")
            addOption("--quiet")
            addOption("--no-warnings")
            addOption("--ignore-errors")
            addOption("--no-mtime")
        }

        val processId = java.util.UUID.randomUUID().toString()
        try {
            val response = YoutubeDL.getInstance().execute(request, processId)
            val jsonRaw = response.out ?: return@withContext null
            JSONObject(jsonRaw)
        } catch (e: Exception) {
            Log.e(Config.TAG_YTDLP_EXTRACTOR, "Error extracting playlist info: ${e.message}", e)
            null
        } finally {
            if (!isActive) {
                try { YoutubeDL.getInstance().destroyProcessById(processId) } catch (_: Exception) {}
            }
        }
    }
}
