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
                addOption("--extractor-args", "youtube:player-client=ios,web,android")
                addOption("--user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1")
            }
            
            addOption("--referer", "https://www.google.com/")
            addOption("--force-ipv4")
            addOption("--no-check-certificate")
            addOption("--geo-bypass")
            addOption("--quiet")
            addOption("--no-warnings")
            addOption("--ignore-errors")
            addOption("--no-mtime")
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
