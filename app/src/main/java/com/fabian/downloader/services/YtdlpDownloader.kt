package com.fabian.downloader.services

import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.BatteryOptimizerManager
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

class YtdlpDownloader {

    companion object {
        private val SPEED_REGEX = Regex("""at\s+([0-9.]+[a-zA-Z]+/s)""")
        private val SIZE_REGEX = Regex("""of\s+([~]?[0-9.]+[a-zA-Z]+)""")
        // Garantizar que el post-procesado / conversión FFmpeg de varias descargas ocurra de uno en uno
        private val postProcessingSemaphore = java.util.concurrent.Semaphore(1, true)

        fun resolveUserFacingError(e: Throwable, lastLine: String): String {
            val msg = e.message ?: ""
            val lowerMsg = msg.lowercase()
            val lowerClass = e.javaClass.name.lowercase()
            val lowerLine = lastLine.lowercase()

            if (lowerClass.contains("youtubedlexception") || lowerClass.contains("youtubedl")) {
                if (lowerMsg.contains("process id already exists")) {
                    return "Proceso atascado. Por favor, reintente la descarga."
                }
                if (lowerMsg.contains("sign in") || lowerMsg.contains("private video") || lowerMsg.contains("login")) {
                    return "Requiere inicio de sesión (Video privado o con restricciones)."
                }
                if (lowerMsg.contains("unavailable") || lowerLine.contains("unavailable")) {
                    return "Video o formato no disponible."
                }
                if (lowerMsg.contains("canceled") || lowerClass.contains("canceled")) {
                    return "Descarga interrumpida de forma inesperada. Pulse reintentar."
                }
                if (lowerMsg.contains("http error 403")) {
                    return "Error de acceso (403). Intente reiniciar la descarga."
                }
                return "Error de YT-DLP: ${msg.take(40)}..."
            }
            
            if (e is java.io.InterruptedIOException || lowerClass.contains("interruptedioexception")) {
                return "Conexión interrumpida inesperadamente. Pulse reintentar."
            }
            
            if (lowerMsg.contains("no space left") || lowerMsg.contains("enospc") || lowerMsg.contains("disk full")) {
                return "Espacio en disco insuficiente. Libere espacio."
            }
            if (lowerMsg.contains("timeout") || lowerMsg.contains("connection") || lowerMsg.contains("network")) {
                return "Error de conexión o tiempo de espera agotado."
            }
            
            val fallBackMsg = lastLine.ifEmpty { msg }
            return fallBackMsg.ifEmpty { com.fabian.downloader.MyApplication.getInstance().getString(com.fabian.downloader.R.string.downloads_error_unknown) }
        }
    }

    private fun createRequest(
        videoUrl: String,
        quality: String,
        format: String,
        destFolder: java.io.File,
        fileNameWithoutExt: String,
        fallbackLevel: Int,
        customizeRequest: ((YoutubeDLRequest) -> Unit)? = null
    ): YoutubeDLRequest {
        val lowerUrl = videoUrl.lowercase()
        val isYoutube = com.fabian.downloader.utils.UrlUtils.isYoutubeUrl(videoUrl)
        val settings = com.fabian.downloader.ui.AppSettings

        return YoutubeDLRequest(videoUrl).apply {
            val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || 
                          format.equals(Config.FORMAT_PNG, ignoreCase = true) || 
                          format.equals(Config.FORMAT_WEBP, ignoreCase = true) || 
                          format.equals("JPEG", ignoreCase = true)

            if (isImage) {
                addOption("--write-thumbnail")
                addOption("--skip-download")
                addOption("--convert-thumbnails", format.lowercase())
            } else if (format == Config.FORMAT_MP3) {
                when (fallbackLevel) {
                    0 -> addOption("-f", "bestaudio/best")
                    1 -> addOption("-f", "ba/b/best")
                    else -> addOption("-f", "best")
                }
                addOption("--extract-audio")
                addOption("--audio-format", "mp3")
                val defaultBitrateDigits = settings.defaultAudioBitrate.filter { it.isDigit() }.ifEmpty { "320" }
                val selectedBitrateDigits = quality.filter { it.isDigit() }
                val finalAudioQuality = if (selectedBitrateDigits.isNotEmpty()) selectedBitrateDigits else defaultBitrateDigits
                addOption("--audio-quality", finalAudioQuality)
            } else if (format == Config.FORMAT_M4A) {
                when (fallbackLevel) {
                    0 -> addOption("-f", "bestaudio/best")
                    1 -> addOption("-f", "ba/b/best")
                    else -> addOption("-f", "best")
                }
                addOption("--extract-audio")
                addOption("--audio-format", "m4a")
            } else {
                val height = quality.filter { it.isDigit() }.ifEmpty { "720" }
                when (fallbackLevel) {
                    0 -> {
                        addOption("-f", "bv*[height<=$height]+ba/b[height<=$height]/best")
                    }
                    1 -> {
                        addOption("-f", "bv*+ba/b/best")
                    }
                    2 -> {
                        addOption("-f", "bestvideo+bestaudio/best")
                    }
                    else -> {
                        addOption("-f", "best")
                    }
                }
                val targetFormat = format.lowercase()
                val supportedVideoFormats = listOf("mp4", "mkv", "webm")
                val finalVideoFormat = if (supportedVideoFormats.contains(targetFormat)) targetFormat else "mp4"
                addOption("--merge-output-format", finalVideoFormat)
                addOption("--remux-video", finalVideoFormat)
            }

            if (isYoutube) {
                when (fallbackLevel) {
                    0 -> addOption("--extractor-args", "youtube:player_client=android,mweb,ios")
                    1 -> addOption("--extractor-args", "youtube:player_client=ios,mweb")
                    2 -> addOption("--extractor-args", "youtube:player_client=mweb,web")
                    3 -> addOption("--extractor-args", "youtube:player_client=android_creator,ios")
                    else -> { /* omit player_client for raw yt-dlp fallback */ }
                }
            }

            if (settings.embedChapters) {
                addOption("--embed-chapters")
            }

            addOption("-o", "${destFolder.absolutePath}/$fileNameWithoutExt.%(ext)s")

            val cookiesFile = java.io.File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }

            // ============================================================
            // PARALELISMO Y VELOCIDAD (respetar ajustes del usuario o aplicar límites por batería baja)
            // ============================================================
            val appCtx = com.fabian.downloader.MyApplication.getInstance()
            val batteryManager = BatteryOptimizerManager.getInstance(appCtx)
            val isBatteryLowMode = settings.batteryOptimizationEnabled && 
                                   batteryManager.isBatteryLowAndNotCharging() && 
                                   settings.batteryLowAction == "Optimizar recursos"

            // Concurrent fragments: user setting (default 10 for max speed) - restricted to 2 in battery saving mode
            val fragments = if (isBatteryLowMode) "2" else settings.concurrentFragments
            addOption("--concurrent-fragments", fragments)

            // Larger buffer = better throughput on fast connections
            addOption("--buffer-size", "16K")

            // HTTP chunk size: improves speed on large downloads (10MB chunks)
            addOption("--http-chunk-size", "10M")

            // Allow retries on throttled rates or fragments instead of failing immediately
            // addOption("--throttled-rate", "100K")
            // addOption("--abort-on-unavailable-fragment")

            // ============================================================
            // LIMITACIÓN DE VELOCIDAD (respetar maxSpeed o limitar por batería baja)
            // ============================================================
            val maxSpeed = if (isBatteryLowMode) {
                if (settings.maxSpeed == Config.SPEED_UNLIMITED || 
                    settings.maxSpeed == Config.SPEED_5M || 
                    settings.maxSpeed == Config.SPEED_10M) {
                    Config.SPEED_1M
                } else {
                    settings.maxSpeed
                }
            } else {
                settings.maxSpeed
            }

            if (maxSpeed != Config.SPEED_UNLIMITED) {
                val limit = when (maxSpeed) {
                    Config.SPEED_100K -> Config.RATE_LIMIT_100K
                    Config.SPEED_250K -> Config.RATE_LIMIT_250K
                    Config.SPEED_500K -> Config.RATE_LIMIT_500K
                    Config.SPEED_1M -> Config.RATE_LIMIT_1M
                    Config.SPEED_2M -> Config.RATE_LIMIT_2M
                    Config.SPEED_5M -> Config.RATE_LIMIT_5M
                    Config.SPEED_10M -> Config.RATE_LIMIT_10M
                    Config.SPEED_20M -> Config.RATE_LIMIT_20M
                    Config.SPEED_50M -> Config.RATE_LIMIT_50M
                    else -> null
                }
                if (limit != null) {
                    addOption("--limit-rate", limit)
                }
            }

            if (settings.embedSubtitles) {
                addOption("--embed-subs")
                addOption("--write-subs")
                addOption("--sub-langs", "all")
            }

            if (!settings.playlistEnabled) {
                addOption("--no-playlist")
            }

            addOption("--force-overwrites")
            addOption("--no-mtime")
            addOption("--continue")  // Resume partial downloads
            if (settings.bypassGeo) {
                addOption("--geo-bypass")
            }

            // ============================================================
            // TIMEOUTS Y RETRIES OPTIMIZADOS PARA VELOCIDAD
            // ============================================================
            // Shorter socket timeout = fail fast and retry (was 30s)
            addOption("--socket-timeout", "15")
            // More retries for resilience (was 10)
            addOption("--retries", "15")
            addOption("--fragment-retries", "15")
            addOption("--no-cache-dir")

            // ============================================================
            // YOUTUBE-SPECIFIC OPTIMIZATIONS
            // ============================================================
            if (isYoutube) {
                val customUa = settings.customUserAgent
                if (customUa.isNotEmpty()) {
                    addOption("--user-agent", customUa)
                }
            }

            addOption("--referer", Config.REFERER_DEFAULT)
            if (com.fabian.downloader.ui.AppSettings.bypassSslVerification) {
                addOption("--no-check-certificate")
            }
            addOption("--no-check-formats")
            // Note: removed --force-ipv4 by default (was slowing down on IPv6-capable networks)
            addOption("--no-warnings")

            // Miniaturas y Metadatos globales
            if (settings.embedMetadata) {
                addOption("--embed-metadata")

                // Limpiar sufijos no deseados (- video, - audio, - Topic, etc.) directamente en las etiquetas incrustadas
                val metaFields = "title,uploader,artist,channel,album,album_artist"
                addOption("--replace-in-metadata", "$metaFields (?i)\\s*-\\s*(video|audio|topic)$ ")
                addOption("--replace-in-metadata", "$metaFields (?i)\\s*\\(video|audio\\)$ ")
                addOption("--replace-in-metadata", "$metaFields (?i)\\s*\\[video|audio\\]$ ")

                // Parsear álbum para que solo sea el artista/subidor
                addOption("--parse-metadata", "%(uploader,artist)s:%(album)s")

                if (settings.markAsMV) {
                    addOption("--parse-metadata", "MV:%(genre)s")
                    addOption("--postprocessor-args", "FFmpegMetadata:-metadata media_type=6 -metadata stik=6 -metadata genre=\"MV\"")
                } else {
                    // Si el usuario no activó "Marcar como MV", establecer género como "Music" y media_type=0 para evitar que reproductores añadan la etiqueta [MV]
                    addOption("--parse-metadata", "Music:%(genre)s")
                    addOption("--postprocessor-args", "FFmpegMetadata:-metadata media_type=0 -metadata stik=0 -metadata genre=\"Music\"")
                }
            }
            if (settings.embedThumbnail) {
                addOption("--embed-thumbnail")
            }

            // SponsorBlock
            if (settings.sponsorBlockEnabled) {
                addOption("--sponsorblock-remove", "sponsor,intro,outro,selfpromo,interaction")
            }

            // Argumentos Personalizados Libres (Estilo Seal/YTDLnis)
            val customArgs = settings.customArguments
            if (customArgs.isNotEmpty()) {
                try {
                    val allowedArgs = setOf(
                        "--sleep-requests", "--sleep-interval", "--max-sleep-interval",
                        "--limit-rate", "--socket-timeout", "--abort-on-error",
                        "--user-agent", "--referer", "--proxy", "--geo-verification-proxy",
                        "--yes-playlist", "--no-playlist", "--flat-playlist",
                        "--buffer-size", "--http-chunk-size", "--concurrent-fragments",
                        "--throttled-rate", "--retries", "--fragment-retries"
                    )
                    val tokens = customArgs.trim().split(Regex("\\s+"))
                    var i = 0
                    while (i < tokens.size) {
                        val token = tokens[i]
                        if (token.startsWith("--")) {
                            if (allowedArgs.contains(token)) {
                                if (i + 1 < tokens.size && !tokens[i + 1].startsWith("-")) {
                                    addOption(token, tokens[i + 1])
                                    i += 2
                                } else {
                                    addOption(token)
                                    i += 1
                                }
                            } else {
                                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Blocked unauthorized argument: $token")
                                i += 1
                                if (i < tokens.size && !tokens[i].startsWith("-")) {
                                    i += 1
                                }
                            }
                        } else {
                            i += 1
                        }
                    }
                } catch (e: Exception) {
                    Log.e(Config.TAG_YTDLP_DOWNLOADER, "Error parsing custom arguments", e)
                }
            }

            // Call optional customizer
            customizeRequest?.invoke(this)
        }
    }

    suspend fun descargar(
        rawVideoUrl: String,
        quality: String,
        format: String,
        destFolder: java.io.File,
        fileNameWithoutExt: String,
        processId: String = java.util.UUID.randomUUID().toString(),
        customizeRequest: ((YoutubeDLRequest) -> Unit)? = null,
        alProgresar: (Float, String, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val videoUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(rawVideoUrl)
        com.fabian.downloader.MyApplication.getInstance().waitForInitialization()
        var lastLine = ""
        var executionError: Exception? = null

        val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || 
                      format.equals(Config.FORMAT_PNG, ignoreCase = true) || 
                      format.equals(Config.FORMAT_WEBP, ignoreCase = true) || 
                      format.equals("JPEG", ignoreCase = true)

        if (isImage) {
            val isDirectImgUrl = videoUrl.endsWith(".jpg", ignoreCase = true) ||
                                 videoUrl.endsWith(".jpeg", ignoreCase = true) ||
                                 videoUrl.endsWith(".png", ignoreCase = true) ||
                                 videoUrl.endsWith(".webp", ignoreCase = true) ||
                                 videoUrl.contains("/image", ignoreCase = true)
            if (isDirectImgUrl) {
                try {
                    alProgresar(15f, Config.STATUS_DOWNLOADING, Config.STATUS_DOWNLOADING)
                    val imgFile = java.io.File(destFolder, "$fileNameWithoutExt.${format.lowercase()}")
                    val request = okhttp3.Request.Builder()
                        .url(videoUrl)
                        .addHeader("User-Agent", Config.UA_DESKTOP)
                        .build()
                    com.fabian.downloader.network.NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful && response.body != null) {
                            imgFile.outputStream().use { out ->
                                response.body!!.byteStream().copyTo(out)
                            }
                            alProgresar(100f, Config.STATUS_COMPLETED, Config.STATUS_COMPLETED)
                            return@withContext true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(Config.TAG_YTDLP_DOWNLOADER, "Direct image download failed, falling back to yt-dlp", e)
                }
            }
        }

        // Helper: destroy any previous yt-dlp process
        suspend fun cleanupBeforeRetry(isNetworkRetry: Boolean = false) {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Could not destroy previous process before retry", e)
            }
            
            // If it's a network retry, we want to KEEP the .part files so yt-dlp can resume!
            // If it's a fallback (format change), we must delete them to avoid conflicts.
            if (!isNetworkRetry) {
                try {
                    destFolder.listFiles { _, name ->
                        name.startsWith("${fileNameWithoutExt}.") 
                    }?.forEach { it.delete() }
                } catch (e: Exception) {
                    Log.w(Config.TAG_YTDLP_DOWNLOADER, "Could not clean files before fallback retry", e)
                }
            }
        }

        // Respect autoRetry setting: if disabled, only attempt level 0 (no fallbacks)
        val autoRetry = com.fabian.downloader.ui.AppSettings.autoRetry

        val connService = com.fabian.downloader.network.ConnectionService()

        fun isNetworkOrTemporaryError(e: Throwable, line: String): Boolean {
            if (e is java.io.InterruptedIOException) return true
            val lowerMsg = (e.message ?: "").lowercase()
            val lowerClass = e.javaClass.name.lowercase()
            val lowerLine = line.lowercase()
            val keywords = listOf(
                "timeout", "time out", "connection", "unable to resolve host", 
                "network is unreachable", "502", "503", "504", "429", 
                "read error", "connection reset", "ssl", "socket", "try again",
                "interrupted", "quickjs", "solving js challenges", "streamgobbler",
                "process id already exists", "canceledexception", "canceled"
            )
            return keywords.any { lowerMsg.contains(it) || lowerClass.contains(it) || lowerLine.contains(it) }
        }

        suspend fun executeWithRetry(
            level: Int,
            onFailAction: suspend (Exception) -> Unit
        ): Boolean {
            var attempt = 0
            val maxAttempts = if (autoRetry) 3 else 1
            while (attempt < maxAttempts) {
                if (!isActive) throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
                var holdsPostProcLock = false
                try {
                    val request = createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, level, customizeRequest)
                    var lastUiUpdate = 0L
                    YoutubeDL.getInstance().execute(request, processId) { progreso, _, line ->
                        lastLine = line
                        val lowerLine = line.lowercase()
                        val isPostProcLine = lowerLine.contains("[extractaudio]") ||
                                 lowerLine.contains("[merger]") ||
                                 lowerLine.contains("[videoconvertor]") ||
                                 lowerLine.contains("[ffmpeg]") ||
                                 lowerLine.contains("[fixup") ||
                                 lowerLine.contains("[postprocessor]") ||
                                 lowerLine.contains("[embed") ||
                                 lowerLine.contains("[sponsorblock]") ||
                                 lowerLine.contains("deleting original file") ||
                                 progreso >= 100f

                        if (isPostProcLine && !holdsPostProcLock) {
                            try {
                                if (postProcessingSemaphore.tryAcquire(50, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                                    holdsPostProcLock = true
                                    Log.i(Config.TAG_YTDLP_DOWNLOADER, "Turno de post-procesado/conversión asignado a $processId")
                                }
                            } catch (e: Exception) {
                                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Error al obtener turno de post-procesado para $processId", e)
                            }
                        }

                        val now = System.currentTimeMillis()
                        // Throttle updates: process regex and notify progress only every 1000ms or on critical milestones (0%, 100%)
                        if (now - lastUiUpdate >= 1000 || progreso == 0f || progreso >= 100f) {
                            lastUiUpdate = now
                            var speedText = Config.STATUS_CALCULATING
                            var sizeText = Config.STATUS_DOWNLOADING

                            val match = SPEED_REGEX.find(line)
                            if (match != null) {
                                speedText = match.groupValues[1]
                            }

                            val sizeMatch = SIZE_REGEX.find(line)
                            if (sizeMatch != null) {
                                sizeText = sizeMatch.groupValues[1].replace("~", "")
                            }

                            if (progreso >= 100f || holdsPostProcLock) {
                                if (speedText == Config.STATUS_CALCULATING || speedText == Config.STATUS_DOWNLOADING) {
                                    speedText = Config.STATUS_FINALIZING
                                }
                            }

                            alProgresar(progreso, sizeText, speedText)
                        }
                    }
                    return true
                } catch (e: Throwable) {
                    val lowerMsg = (e.message ?: "").lowercase()
                    val lowerLast = lastLine.lowercase()
                    
                    // Solo tratar como cancelación explícita del usuario si la corrutina no está activa
                    // o e es CancellationException.
                    val isExplicitCancel = (e is kotlinx.coroutines.CancellationException) || !isActive

                    if (isExplicitCancel) {
                        throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
                    }
                    
                    attempt++
                    Log.w(Config.TAG_YTDLP_DOWNLOADER, "Intento $attempt/$maxAttempts fallido para nivel $level (${e.javaClass.simpleName}: ${e.message}). Notificando reintento...")
                    
                    // Notificar a la UI y a Room DB que estamos reintentando
                    alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                    
                    val isCorruptBinary = lowerMsg.contains("zipimport") || lowerLast.contains("zipimport") ||
                                          lowerMsg.contains("bad local file header") || lowerLast.contains("bad local file header") ||
                                          lowerMsg.contains("cannot link executable") || lowerLast.contains("cannot link executable") ||
                                          lowerMsg.contains("libandroid-support") || lowerLast.contains("libandroid-support") ||
                                          lowerMsg.contains("cannot link") || lowerLast.contains("cannot link")
                    if (isCorruptBinary) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Detectado binario yt-dlp corrupto durante descarga. Ejecutando reset de emergencia desde APK assets...")
                        val appCtx = com.fabian.downloader.MyApplication.getInstance()
                        appCtx.resetAndReinitYtdlp(appCtx)
                    } else if (lowerMsg.contains("player api") || lowerLast.contains("player api") ||
                        lowerMsg.contains("ios") || lowerLast.contains("ios") ||
                        lowerMsg.contains("requested format") || lowerLast.contains("requested format") ||
                        lowerMsg.contains("format is not available") || lowerLast.contains("format is not available") ||
                        lowerMsg.contains("quickjs") || lowerLast.contains("quickjs")) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Detectada incompatibilidad de API/extractor en YouTube. Intentando refrescar binario yt-dlp...")
                        com.fabian.downloader.MyApplication.getInstance().forceUpdateYtdlpBinary(com.fabian.downloader.MyApplication.getInstance())
                    }

                    val hasInternet = connService.checkConnection()
                    val isNetworkOrIoError = !hasInternet || isNetworkOrTemporaryError(e, lastLine)

                    if (isNetworkOrIoError && attempt < maxAttempts) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Intento $attempt fallido por error de red/I/O. Esperando recuperación de red...")
                        var secondsWaited = 0
                        while (secondsWaited < 15 && !connService.checkConnection()) {
                            if (!isActive) throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
                            kotlinx.coroutines.delay(1000)
                            secondsWaited++
                        }
                        
                        if (connService.checkConnection()) {
                            Log.i(Config.TAG_YTDLP_DOWNLOADER, "Reintentando el mismo nivel $level (intento ${attempt + 1})...")
                            cleanupBeforeRetry(isNetworkRetry = true)
                            kotlinx.coroutines.delay(1000)
                            continue
                        }
                    }

                    if (attempt >= maxAttempts) {
                        onFailAction(Exception(e))
                    } else {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Intento $attempt fallido. Reintentando en 2 segundos...")
                        cleanupBeforeRetry(isNetworkRetry = true)
                        kotlinx.coroutines.delay(2000)
                    }
                } finally {
                    if (!coroutineContext.isActive) {
                        try {
                            com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(processId)
                        } catch (_: Exception) {}
                    }
                    if (holdsPostProcLock) {
                        holdsPostProcLock = false
                        postProcessingSemaphore.release()
                        Log.i(Config.TAG_YTDLP_DOWNLOADER, "Turno de post-procesado/conversión liberado por $processId")
                    }
                }
            }
            return false
        }

        try {
            // Nivel 0: Intentar con la calidad / formato solicitados (y fallback interno de calidad)
            val success0 = executeWithRetry(0) { e ->
                executionError = e
                if (autoRetry) {
                    Log.w(Config.TAG_YTDLP_DOWNLOADER, "Primer nivel fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 1...")
                    alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                    cleanupBeforeRetry()
                }
            }
            if (success0 || !autoRetry) return@withContext success0

            // Nivel 1: Intentar con mejor formato disponible sin limitación estricta de altura
            val success1 = executeWithRetry(1) { e ->
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Segundo nivel fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 2...")
                executionError = e
                alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                cleanupBeforeRetry()
            }
            if (success1) return@withContext true

            // Nivel 2: Descargar formato de fallback amplio
            val success2 = executeWithRetry(2) { e ->
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Tercer nivel fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 3 (best/b)...")
                executionError = e
                alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                cleanupBeforeRetry()
            }
            if (success2) return@withContext true

            // Nivel 3: Descargar formato absoluto básico 'best/b'
            val success3 = executeWithRetry(3) { e ->
                Log.e(Config.TAG_YTDLP_DOWNLOADER, "Todos los niveles e intentos de descarga fallaron para $videoUrl. Última línea: $lastLine", e)
                val finalError = executionError ?: e
                val errorMessage = resolveUserFacingError(finalError, lastLine)
                throw Exception(Config.STATUS_FAILED_PREFIX + errorMessage)
            }
            return@withContext success3
        } finally {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Could not destroy process $processId in final cleanup", e)
            }
        }
    }
}
