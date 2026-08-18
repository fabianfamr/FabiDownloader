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
        private var extractionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun cancelAllExtractions() {
            extractionScope.cancel()
            extractionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            activeExtractions.clear()
        }
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
        request.addOption("--no-check-formats")
    }

    open fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        // Downloader-only options (not needed for extraction)
        // Site-specific overrides can be added here by subclasses.
    }

    override suspend fun extractMetadata(url: String): InfoMedia? {
        com.fabian.downloader.MyApplication.getInstance().waitForInitialization()
        val cleanUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(url)
        val isYoutube = com.fabian.downloader.utils.UrlUtils.isYoutubeUrl(cleanUrl)
        
        val deferred = activeExtractions.computeIfAbsent(cleanUrl) { _ ->
            extractionScope.async {
                val clientOptions: List<String?> = if (isYoutube) {
                    listOf("android,mweb,ios", "ios,mweb", "tv,android_creator,mweb", "android_creator,ios,tv,web", null)
                } else {
                    listOf(null)
                }

                fun createRequest(playerClient: String?): YoutubeDLRequest {
                    return YoutubeDLRequest(cleanUrl).apply {
                        addOption("--dump-json")
                        
                        val cookiesFile = File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
                        if (cookiesFile.exists() && cookiesFile.length() > 0) {
                            addOption("--cookies", cookiesFile.absolutePath)
                        }
                        
                        if (!com.fabian.downloader.ui.AppSettings.playlistEnabled) {
                            addOption("--no-playlist")
                        }
                        addOption("--no-cache-dir")
                        
                        if (isYoutube && !playerClient.isNullOrEmpty()) {
                            addOption("--extractor-args", "youtube:player_client=$playerClient")
                        }
                        
                        customizeExtractorRequest(this, cleanUrl)
                    }
                }

                for (client in clientOptions) {
                    val request = createRequest(client)
                    try {
                        val response = YoutubeDL.getInstance().execute(request)
                        val jsonRaw = response.out ?: continue
                        val json = JSONObject(jsonRaw)

                        val parsed = com.fabian.downloader.utils.YtdlpParser.parseMetadata(json, Config.STATUS_UNKNOWN, "Video de $displayName")
                        if (parsed != null) {
                            return@async parsed
                        }
                    } catch (e: Exception) {
                        Log.e(Config.TAG_BASE_SITE_SERVICE, "Error extracting info for $cleanUrl (client=$client) in service $siteId: ${e.message}", e)
                        val lowerMsg = (e.message ?: "").lowercase()
                        if (lowerMsg.contains("zipimport") || lowerMsg.contains("bad local file header") ||
                            lowerMsg.contains("cannot link") || lowerMsg.contains("libandroid-support") ||
                            lowerMsg.contains("libpython") || lowerMsg.contains("not found")) {
                            Log.w(Config.TAG_BASE_SITE_SERVICE, "Detectada corrupción de binario. Re-inicializando binario limpio y reintentando...")
                            val appCtx = com.fabian.downloader.MyApplication.getInstance()
                            appCtx.resetAndReinitYtdlp(appCtx)
                        } else if (lowerMsg.contains("player api") || lowerMsg.contains("bot") || lowerMsg.contains("sign in") || lowerMsg.contains("confirm you're not a bot")) {
                            Log.w(Config.TAG_BASE_SITE_SERVICE, "Error de autenticación/player api en YouTube. Intentando siguiente cliente o actualizando motor...")
                            val appCtx = com.fabian.downloader.MyApplication.getInstance()
                            appCtx.forceUpdateYtdlpBinary(appCtx)
                        }
                    }
                }
                null
            }
        }

        try {
            return deferred.await()
        } catch (e: Exception) {
            deferred.cancel()
            if (e is kotlinx.coroutines.CancellationException) {
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
