package com.fabian.downloader.services

import com.fabian.downloader.utils.Config
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
        val isYoutube = videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be") || videoUrl.contains("shorts") || videoUrl.contains("music.youtube.com")
        val settings = com.fabian.downloader.ui.AppSettings

        return YoutubeDLRequest(videoUrl).apply {
            if (format == Config.FORMAT_MP3) {
                if (fallbackLevel == 0) {
                    addOption("-f", "bestaudio/best")
                } else {
                    addOption("-f", "best")
                }
                addOption("--extract-audio")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", quality.filter { it.isDigit() }.ifEmpty { "128" })
            } else if (format == Config.FORMAT_M4A) {
                if (fallbackLevel == 0) {
                    addOption("-f", "bestaudio/best")
                } else {
                    addOption("-f", "best")
                }
                addOption("--extract-audio")
                addOption("--audio-format", "m4a")
            } else {
                val height = quality.filter { it.isDigit() }.ifEmpty { "720" }
                when (fallbackLevel) {
                    0 -> {
                        // Priorizar mejor video hasta la altura deseada + mejor audio, luego cualquier formato combinado
                        addOption("-f", "bv*[height<=$height]+ba/b[height<=$height]/bv*+ba/b/best")
                    }
                    1 -> {
                        addOption("-f", "bv*+ba/b/best")
                    }
                    else -> {
                        addOption("-f", "best")
                    }
                }
                addOption("--merge-output-format", "mp4")
            }

            addOption("-o", "${destFolder.absolutePath}/$fileNameWithoutExt.%(ext)s")

            val cookiesFile = java.io.File(com.fabian.downloader.MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }

            // ============================================================
            // PARALELISMO Y VELOCIDAD (respetar ajustes del usuario)
            // ============================================================
            // Concurrent fragments: user setting (default 10 for max speed)
            val fragments = settings.concurrentFragments
            addOption("--concurrent-fragments", fragments)

            // Larger buffer = better throughput on fast connections
            addOption("--buffer-size", "16K")

            // HTTP chunk size: improves speed on large downloads (10MB chunks)
            addOption("--http-chunk-size", "10M")

            // Detect YouTube throttling (~70KB/s) and abort+retry immediately
            addOption("--throttled-rate", "100K")

            // Abort immediately if a fragment is unavailable (don't waste time waiting)
            addOption("--abort-on-unavailable-fragment")

            // ============================================================
            // LIMITACIÓN DE VELOCIDAD (respetar maxSpeed + dataSaver)
            // ============================================================
            val maxSpeed = settings.maxSpeed
            val dataSaver = settings.dataSaverEnabled

            // Effective speed limit: if dataSaver is on and user hasn't set a limit, force 1MB/s
            val effectiveSpeed = when {
                dataSaver && maxSpeed == Config.SPEED_UNLIMITED -> Config.SPEED_1M
                dataSaver -> maxSpeed // If user set a lower limit, respect it
                else -> maxSpeed
            }

            if (effectiveSpeed != Config.SPEED_UNLIMITED) {
                val limit = when (effectiveSpeed) {
                    Config.SPEED_500K -> Config.RATE_LIMIT_500K
                    Config.SPEED_1M -> Config.RATE_LIMIT_1M
                    Config.SPEED_5M -> Config.RATE_LIMIT_5M
                    Config.SPEED_10M -> Config.RATE_LIMIT_10M
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

            addOption("--no-overwrites")
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
                // Use multiple player clients to avoid throttling (ios is faster, android avoids bot detection)
                addOption("--extractor-args", "youtube:player-client=ios,android,web")

                val customUa = settings.customUserAgent
                if (customUa.isNotEmpty()) {
                    addOption("--user-agent", customUa)
                }

                // Skip DASH manifest (faster extraction)
                addOption("--youtube-skip-dash-manifest")
            }

            addOption("--referer", Config.REFERER_DEFAULT)
            // Note: removed --force-ipv4 by default (was slowing down on IPv6-capable networks)
            addOption("--no-warnings")

            // Miniaturas y Metadatos globales
            if (settings.embedMetadata) {
                addOption("--embed-metadata")
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
        videoUrl: String,
        quality: String,
        format: String,
        destFolder: java.io.File,
        fileNameWithoutExt: String,
        processId: String = java.util.UUID.randomUUID().toString(),
        customizeRequest: ((YoutubeDLRequest) -> Unit)? = null,
        alProgresar: (Float, String, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        com.fabian.downloader.MyApplication.getInstance().waitForInitialization()
        var lastLine = ""
        var executionError: Exception? = null

        // Helper: destroy any previous yt-dlp process and clean partial files before retrying
        suspend fun cleanupBeforeRetry() {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Could not destroy previous process before retry", e)
            }
            // Remove partial files left by previous attempt so --no-overwrites doesn't skip
            try {
                destFolder.listFiles { _, name ->
                    name.startsWith("${fileNameWithoutExt}.") &&
                    (name.endsWith(".part") || name.endsWith(".ytdl") || name.endsWith(".temp"))
                }?.forEach { it.delete() }
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Could not clean partial files before retry", e)
            }
        }

        // Respect autoRetry setting: if disabled, only attempt level 0 (no fallbacks)
        val autoRetry = com.fabian.downloader.ui.AppSettings.autoRetry

        // Nivel 0: Intentar con la calidad / formato solicitados (y fallback interno de calidad)
        try {
            val request = createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, 0, customizeRequest)
            YoutubeDL.getInstance().execute(request, processId) { progreso, _, line ->
                lastLine = line
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

                if (progreso == 100f && speedText == Config.STATUS_CALCULATING) {
                    speedText = Config.STATUS_FINALIZING
                }

                alProgresar(progreso, sizeText, speedText)
            }
            return@withContext true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException || !isActive) {
                throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
            }
            if (!autoRetry) {
                // If autoRetry is disabled, fail immediately without fallbacks
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Descarga fallida (autoRetry desactivado): $videoUrl - ${e.message}")
                val errorMessage = lastLine.ifEmpty { e.message ?: com.fabian.downloader.MyApplication.getInstance().getString(com.fabian.downloader.R.string.downloads_error_unknown) }
                throw Exception(Config.STATUS_FAILED_PREFIX + errorMessage)
            }
            Log.w(Config.TAG_YTDLP_DOWNLOADER, "Primer intento fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 1 (bestvideo+bestaudio/best)...")
            executionError = e
            cleanupBeforeRetry()
        }

        // Nivel 1: Intentar con mejor formato disponible sin limitación estricta de altura
        try {
            val request = createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, 1, customizeRequest)
            YoutubeDL.getInstance().execute(request, processId) { progreso, _, line ->
                lastLine = line
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

                if (progreso == 100f && speedText == Config.STATUS_CALCULATING) {
                    speedText = Config.STATUS_FINALIZING
                }

                alProgresar(progreso, sizeText, speedText)
            }
            return@withContext true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException || !isActive) {
                throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
            }
            Log.w(Config.TAG_YTDLP_DOWNLOADER, "Segundo intento fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 2 (best)...")
            executionError = e
            cleanupBeforeRetry()
        }

        // Nivel 2: Descargar formato absoluto básico 'best' (el formato más compatible soportado por cualquier extractor)
        try {
            val request = createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, 2, customizeRequest)
            YoutubeDL.getInstance().execute(request, processId) { progreso, _, line ->
                lastLine = line
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

                if (progreso == 100f && speedText == Config.STATUS_CALCULATING) {
                    speedText = Config.STATUS_FINALIZING
                }

                alProgresar(progreso, sizeText, speedText)
            }
            return@withContext true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException || !isActive) {
                throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
            }
            Log.e(Config.TAG_YTDLP_DOWNLOADER, "Todos los intentos de descarga fallaron para $videoUrl. Última línea: $lastLine", e)
            val errorMessage = lastLine.ifEmpty { e.message ?: executionError?.message ?: com.fabian.downloader.MyApplication.getInstance().getString(com.fabian.downloader.R.string.downloads_error_unknown) }
            throw Exception(Config.STATUS_FAILED_PREFIX + errorMessage)
        }
    }
}
