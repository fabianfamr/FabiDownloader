package com.fabian.downloader.services.sites

import android.util.Log
import com.fabian.downloader.configs.Config
import com.fabian.downloader.services.InfoMedia
import com.fabian.downloader.services.YtdlpDownloader
import com.fabian.downloader.services.YtdlpExtractor
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

abstract class BaseSiteService : SiteService {

    companion object {
        private val activeExtractions = java.util.concurrent.ConcurrentHashMap<String, Deferred<InfoMedia?>>()
        private val extractionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private val downloader = YtdlpDownloader()

    open fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        // Shared options between extractor and downloader
        request.addOption("--geo-bypass")
        request.addOption("--quiet")
        request.addOption("--no-warnings")
        request.addOption("--socket-timeout", "10")
        request.addOption("--retries", "5")
        if (com.fabian.downloader.ui.AppSettings.bypassSslVerification) {
            request.addOption("--no-check-certificate")
        }
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
        if (com.fabian.downloader.ui.AppSettings.bypassSslVerification) {
            request.addOption("--no-check-certificate")
        }
        request.addOption("--no-call-home")
        request.addOption("--no-check-formats")
    }

    override suspend fun extractMetadata(url: String): InfoMedia? {
        val cleanUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(url)
        
        val deferred = activeExtractions.computeIfAbsent(cleanUrl) { _ ->
            extractionScope.async {
                val request = YoutubeDLRequest(cleanUrl).apply {
                    addOption("--dump-json")
                    
                    val cookiesFile = File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
                    if (cookiesFile.exists() && cookiesFile.length() > 0) {
                        addOption("--cookies", cookiesFile.absolutePath)
                    }
                    
                    if (!com.fabian.downloader.ui.AppSettings.playlistEnabled) {
                        addOption("--no-playlist")
                    }
                    addOption("--no-cache-dir")
                    
                    customizeExtractorRequest(this, cleanUrl)
                }

                try {
                    val response = YoutubeDL.getInstance().execute(request)
                    val jsonRaw = response.out ?: return@async null
                    val json = JSONObject(jsonRaw)

                    com.fabian.downloader.utils.YtdlpParser.parseMetadata(json, Config.STATUS_UNKNOWN, "Video de $displayName")
                } catch (e: Exception) {
                    Log.e(Config.TAG_BASE_SITE_SERVICE, "Error extracting info for $cleanUrl in service $siteId: ${e.message}", e)
                    val lowerMsg = (e.message ?: "").lowercase()
                    if (lowerMsg.contains("zipimport") || lowerMsg.contains("bad local file header") ||
                        lowerMsg.contains("player api") || lowerMsg.contains("player-client")) {
                        Log.w(Config.TAG_BASE_SITE_SERVICE, "Detectada corrupción de binario o error de API player. Re-inicializando binario limpio y reintentando...")
                        val appCtx = com.fabian.downloader.MyApplication.getInstance()
                        appCtx.resetAndReinitYtdlp(appCtx)
                        try {
                            val retryResponse = YoutubeDL.getInstance().execute(request)
                            val retryJsonRaw = retryResponse.out ?: return@async null
                            val retryJson = JSONObject(retryJsonRaw)
                            return@async com.fabian.downloader.utils.YtdlpParser.parseMetadata(retryJson, Config.STATUS_UNKNOWN, "Video de $displayName")
                        } catch (retryException: Exception) {
                            Log.e(Config.TAG_BASE_SITE_SERVICE, "Reintento de extracción falló tras re-inicializar: ${retryException.message}", retryException)
                        }
                    }
                    null
                }
            }
        }

        try {
            return deferred.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                deferred.cancel()
                throw e
            }
            return null
        } finally {
            activeExtractions.remove(cleanUrl, deferred)
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
            rawVideoUrl = url,
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
