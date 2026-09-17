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

open class BaseSiteService(
    override val siteId: String = "generic",
    override val displayName: String = "Enlace Directo",
    override val brandColorHex: String = "#607D8B",
    override val iconName: String = "generic",
    override val supportedUrlPatterns: List<String> = emptyList()
) : SiteService {

    companion object {
        private val activeExtractions = java.util.concurrent.ConcurrentHashMap<String, Deferred<InfoMedia?>>()
        private var extractionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        // Época (generación) del scope: cada cancelAllExtractions la incrementa.
        // Permite descartar Deferred viejos pertenecientes a un scope ya cancelado.
        private val extractionEpoch = java.util.concurrent.atomic.AtomicLong(0L)

        fun cancelAllExtractions() {
            extractionEpoch.incrementAndGet()
            extractionScope.cancel()
            extractionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            activeExtractions.clear()
        }
    }

    private val downloader = YtdlpDownloader()

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        // Shared options between extractor and downloader
        request.addOption("--geo-bypass")
        request.addOption("--quiet")
        request.addOption("--no-warnings")
        request.addOption("--socket-timeout", "8")
        request.addOption("--retries", "2")
        request.addOption("--extractor-retries", "1")
        if (com.fabian.downloader.ui.AppSettings.bypassSslVerification) {
            request.addOption("--no-check-certificate")
        }
        request.addOption("--no-check-formats")

        customUserAgent?.let { ua ->
            if (ua.isNotEmpty()) request.addOption("--user-agent", ua)
        }
        customReferer?.let { ref ->
            if (ref.isNotEmpty()) request.addOption("--referer", ref)
        }
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        // Downloader-only options
        customUserAgent?.let { ua ->
            if (ua.isNotEmpty()) request.addOption("--user-agent", ua)
        }
        customReferer?.let { ref ->
            if (ref.isNotEmpty()) request.addOption("--referer", ref)
        }
    }

    override suspend fun extractMetadata(url: String): InfoMedia? {
        val cleanUrl = cleanUrl(com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(url))
        
        // Vía rápida: Extracción nativa HTTP directa sin sobrecarga de Python si es soportado
        try {
            val native = com.fabian.downloader.services.NativeMediaExtractor.extractNatively(cleanUrl)
            if (native != null && native.title.isNotEmpty() && native.title != Config.STATUS_UNKNOWN) {
                return InfoMedia(
                    titulo = native.title,
                    autor = native.author.ifEmpty { displayName },
                    miniaturaUrl = native.thumbnailUrl ?: "",
                    duracionTexto = if (native.durationSeconds > 0) {
                        val mins = native.durationSeconds / 60
                        val secs = native.durationSeconds % 60
                        String.format(java.util.Locale.US, "%d:%02d", mins, secs)
                    } else "",
                    vistas = "",
                    pesoEstimadoMB = native.formatSizes.values.maxOrNull() ?: 0.0,
                    videoId = "",
                    formatSizes = native.formatSizes
                )
            }
        } catch (_: Exception) {}

        com.fabian.downloader.MyApplication.getInstance().ensureInitialized()
        val isYoutube = com.fabian.downloader.utils.UrlUtils.isYoutubeUrl(cleanUrl)

        // Reintentar en bucle si el Deferred obtenido pertenece a un scope
        // cancelado por cancelAllExtractions (época distinta), para no devolver
        // un resultado "stale" ni reintentar sobre un Deferred muerto.
        while (true) {
            val epochAtCall = extractionEpoch.get()
            val deferred = activeExtractions.computeIfAbsent(cleanUrl) { _ ->
                extractionScope.async {
                    val clientOptions: List<String?> = if (isYoutube) {
                        // Primero los clientes por defecto de yt-dlp (mantenidos por el
                        // proyecto y compatibles con QuickJS); los clientes explícitos
                        // quedan como fallback en caso de bot-detection o firmas rotas.
                        listOf(null, "ios,mweb", "ios,web", "android_creator,mweb")
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
                            addOption("--no-update")

                            if (isYoutube && !playerClient.isNullOrEmpty()) {
                                addOption("--extractor-args", "youtube:player_client=$playerClient")
                            }

                            customizeExtractorRequest(this, cleanUrl)
                        }
                    }

                    for (client in clientOptions) {
                        val request = createRequest(client)
                        val processId = java.util.UUID.randomUUID().toString()
                        try {
                            val response = YoutubeDL.getInstance().execute(request, processId)
                            val jsonRaw = response.out ?: continue
                            val json = JSONObject(jsonRaw)

                            val parsed = com.fabian.downloader.utils.YtdlpParser.parseMetadata(
                                json,
                                defaultAuthor = Config.STATUS_UNKNOWN,
                                defaultTitle = "Video de $displayName"
                            )
                            return@async parsed
                        } catch (e: Exception) {
                            Log.e(Config.TAG_BASE_SITE_SERVICE, "Error extracting info for $cleanUrl (client=$client) in service $siteId: ${e.message}", e)
                            val lowerMsg = (e.message ?: "").lowercase()
                            val appCtx = com.fabian.downloader.MyApplication.getInstance()
                            if (lowerMsg.contains("not initialized") || (e is IllegalStateException && lowerMsg.contains("initialized"))) {
                                Log.w(Config.TAG_BASE_SITE_SERVICE, "YoutubeDL no inicializado en extracción. Auto-recuperando y reintentando...")
                                val recovered = appCtx.ensureInitialized(appCtx)
                                if (recovered) {
                                    try {
                                        val retryResponse = YoutubeDL.getInstance().execute(request, processId)
                                        val jsonRaw = retryResponse.out
                                        if (jsonRaw != null) {
                                            val json = JSONObject(jsonRaw)
                                            return@async com.fabian.downloader.utils.YtdlpParser.parseMetadata(
                                                json,
                                                defaultAuthor = Config.STATUS_UNKNOWN,
                                                defaultTitle = "Video de $displayName"
                                            )
                                        }
                                    } catch (retryEx: Exception) {
                                        Log.e(Config.TAG_BASE_SITE_SERVICE, "Fallo reintento tras inicialización: ${retryEx.message}", retryEx)
                                    }
                                }
                            } else if (lowerMsg.contains("zipimport") || lowerMsg.contains("bad local file header") ||
                                lowerMsg.contains("cannot link") || lowerMsg.contains("libandroid-support") ||
                                lowerMsg.contains("libpython") || lowerMsg.contains("exec format error")) {
                                Log.w(Config.TAG_BASE_SITE_SERVICE, "Detectada corrupción de binario. Re-inicializando binario limpio y reintentando...")
                                appCtx.resetAndReinitYtdlp(appCtx)
                            } else if (com.fabian.downloader.services.YtdlpErrorResolver.isExtractorOrCipherError(e, lowerMsg)) {
                                Log.w(Config.TAG_BASE_SITE_SERVICE, "Posible extractor/firma desactualizada. Solicitando verificación silenciosa con ahorro de Wi-Fi...")
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    com.fabian.downloader.managers.YtdlpUpdateManager.autoUpdateSilentlyOnFailure(appCtx)
                                }
                            }
                        } finally {
                            try {
                                YoutubeDL.getInstance().destroyProcessById(processId)
                            } catch (_: Exception) {}
                        }
                    }
                    Log.w(Config.TAG_BASE_SITE_SERVICE, "Todas las extracciones fallaron para $cleanUrl (servicio $siteId)")
                    null
                }
            }

            if (epochAtCall != extractionEpoch.get()) {
                // El scope fue reiniciado mientras se creaba/reutilizaba el Deferred:
                // descartarlo y reintentar con el scope nuevo.
                activeExtractions.remove(cleanUrl, deferred)
                deferred.cancel()
                continue
            }

            try {
                return deferred.await()
            } catch (e: Exception) {
                deferred.cancel()
                if (e is kotlinx.coroutines.CancellationException && epochAtCall == extractionEpoch.get()) {
                    throw e
                }
                if (e is kotlinx.coroutines.CancellationException) {
                    // Deferred cancelado por reinicio del scope: reintentar.
                    continue
                }
                return null
            } finally {
                activeExtractions.remove(cleanUrl, deferred)
            }
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
