package com.fabian.downloader.services.sites

import android.util.Log
import com.fabian.downloader.utils.Config
import com.fabian.downloader.services.InfoMedia
import com.fabian.downloader.services.YtdlpDownloader
import com.fabian.downloader.services.YtdlpExtractor
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

abstract class BaseSiteService : SiteService {

    private val downloader = YtdlpDownloader()

    open fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        // Shared options between extractor and downloader
        request.addOption("--geo-bypass")
        request.addOption("--quiet")
        request.addOption("--no-warnings")
        request.addOption("--socket-timeout", "10")
        request.addOption("--retries", "5")
        request.addOption("--no-check-certificate")
        request.addOption("--no-call-home")
        request.addOption("--no-check-formats")
    }

    open fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        // Downloader-only options (not needed for extraction)
        // Note: socket-timeout, retries, fragment-retries are already set by YtdlpDownloader.createRequest()
        // We only add site-specific overrides here to avoid duplicate options
        request.addOption("--no-overwrites")
        request.addOption("--no-mtime")
        request.addOption("--referer", Config.REFERER_DEFAULT)
        request.addOption("--no-check-certificate")
        request.addOption("--no-call-home")
        request.addOption("--no-check-formats")
    }

    override suspend fun extractMetadata(url: String): InfoMedia? = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--dump-json")
            
            val cookiesFile = File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }
            
            if (!com.fabian.downloader.ui.AppSettings.playlistEnabled) {
                addOption("--no-playlist")
            }
            addOption("--no-cache-dir")
            
            customizeExtractorRequest(this, url)
        }

        try {
            val response = YoutubeDL.getInstance().execute(request)
            val jsonRaw = response.out ?: return@withContext null
            val json = JSONObject(jsonRaw)

            return@withContext com.fabian.downloader.utils.YtdlpParser.parseMetadata(json, Config.STATUS_UNKNOWN, "Video de $displayName")
        } catch (e: Exception) {
            Log.e(Config.TAG_BASE_SITE_SERVICE, "Error extracting info for $url in service $siteId: ${e.message}", e)
            return@withContext null
        }
    }

    override suspend fun download(
        url: String,
        quality: String,
        format: String,
        destFolder: File,
        fileNameWithoutExt: String, processId: String?,
        onProgress: (progress: Float, sizeText: String, speedText: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        downloader.descargar(
            videoUrl = url,
            quality = quality,
            format = format,
            destFolder = destFolder,
            fileNameWithoutExt = fileNameWithoutExt,
            processId = processId ?: java.util.UUID.randomUUID().toString(),
            customizeRequest = { request ->
                customizeDownloaderRequest(request, url)
            },
            alProgresar = onProgress
        )
    }
}
