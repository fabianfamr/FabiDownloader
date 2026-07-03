package com.fabian.downloader.services

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

        val request = YoutubeDLRequest(videoUrl).apply {
            addOption("--dump-json")
            
            val cookiesFile = java.io.File(com.fabian.downloader.MyApplication.getInstance().filesDir, "cookies.txt")
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }
            
            if (!com.fabian.downloader.ui.AppSettings.playlistEnabled) {
                // Only use --flat-playlist if not dumping json, but here we ARE dumping json.
                // However, the user request says: "Quitar --flat-playlist" in BaseSiteService.
                // Let's remove it if we want full format info.
                addOption("--no-playlist")
            }
            addOption("--no-cache-dir")
            
            if (isYoutube) {
                // Configuración específica para YouTube para evadir bot detection
                addOption("--extractor-args", "youtube:player-client=android,web,ios")
                addOption("--user-agent", "com.google.android.youtube/19.29.37 (Linux; U; Android 14; en_US) gzip")
                addOption("--add-header", "X-Youtube-Client-Name: 3")
                addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
            }
            
            addOption("--referer", "https://www.google.com/")
            addOption("--force-ipv4")
            // addOption("--no-check-certificate") // Removed for security as per user request
            addOption("--geo-bypass")
            addOption("--quiet")
            addOption("--no-warnings")
        }

        try {
            val response = YoutubeDL.getInstance().execute(request)
            val jsonRaw = response.out ?: return@withContext null
            val json = JSONObject(jsonRaw)

            val defaultAuthor = if (isInstagram) "Instagram User" else "Desconocido"
            return@withContext com.fabian.downloader.utils.YtdlpParser.parseMetadata(json, defaultAuthor)
        } catch (e: Exception) {
            Log.e("YtdlpExtractor", "Error extracting video info: ${e.message}", e)
            return@withContext null
        }
    }
}
