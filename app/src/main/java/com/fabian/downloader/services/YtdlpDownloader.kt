package com.fabian.downloader.services

import android.os.Environment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        return YoutubeDLRequest(videoUrl).apply {
            if (format == "MP3") {
                if (fallbackLevel == 0) {
                    addOption("-f", "bestaudio/best")
                } else {
                    addOption("-f", "best")
                }
                addOption("--extract-audio")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", quality.filter { it.isDigit() }.ifEmpty { "128" })
            } else if (format == "M4A") {
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
                        addOption("-f", "bv*[ext=mp4][height<=?$height]+ba[ext=m4a]/b[ext=mp4][height<=?$height]/bv*[height<=?$height]+ba/b[height<=?$height]/bv*+ba/b/best")
                    }
                    1 -> {
                        addOption("-f", "bv*+ba/b/best")
                    }
                    else -> {
                        addOption("-f", "best")
                    }
                }
                addOption("--merge-output-format", "mp4")
                addOption("--recode-video", "mp4")
            }
            
            addOption("-o", "${destFolder.absolutePath}/$fileNameWithoutExt.%(ext)s")
            
            val cookiesFile = java.io.File(com.fabian.downloader.MyApplication.getInstance().filesDir, "cookies.txt")
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }
            
            val fragments = com.fabian.downloader.ui.AppSettings.concurrentFragments
            addOption("--concurrent-fragments", fragments)
            
            val maxSpeed = com.fabian.downloader.ui.AppSettings.maxSpeed
            if (maxSpeed != "Ilimitada") {
                val limit = when (maxSpeed) {
                    "500 KB/s" -> "500K"
                    "1 MB/s" -> "1M"
                    "5 MB/s" -> "5M"
                    "10 MB/s" -> "10M"
                    else -> null
                }
                if (limit != null) {
                    addOption("--limit-rate", limit)
                }
            }

            if (com.fabian.downloader.ui.AppSettings.embedSubtitles) {
                addOption("--embed-subs")
                addOption("--write-subs")
                addOption("--sub-langs", "all")
            }

            if (!com.fabian.downloader.ui.AppSettings.playlistEnabled) {
                addOption("--no-playlist")
            }

            addOption("--no-overwrites")
            addOption("--no-mtime")
            // addOption("--no-check-certificate") // Removed for security as per user request
            if (com.fabian.downloader.ui.AppSettings.bypassGeo) {
                addOption("--geo-bypass")
            }
            addOption("--socket-timeout", "30")
            addOption("--retries", "10")
            addOption("--fragment-retries", "10")
            addOption("--no-cache-dir")
            
            if (isYoutube) {
                addOption("--extractor-args", "youtube:player-client=android,web,ios")
                
                val customUa = com.fabian.downloader.ui.AppSettings.customUserAgent
                if (customUa.isNotEmpty()) {
                    addOption("--user-agent", customUa)
                } else {
                    addOption("--user-agent", "com.google.android.youtube/19.29.37 (Linux; U; Android 14; en_US) gzip")
                }
                
                addOption("--add-header", "X-Youtube-Client-Name: 3")
                addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
            }
            
            addOption("--referer", "https://www.google.com/")
            addOption("--force-ipv4")
            addOption("--no-warnings")

            // Miniaturas y Metadatos globales
            if (com.fabian.downloader.ui.AppSettings.embedMetadata) {
                addOption("--embed-metadata")
            }
            if (com.fabian.downloader.ui.AppSettings.embedThumbnail) {
                addOption("--embed-thumbnail")
            }

            // SponsorBlock
            if (com.fabian.downloader.ui.AppSettings.sponsorBlockEnabled) {
                addOption("--sponsorblock-remove", "sponsor,intro,outro,selfpromo,interaction")
            }

            // Proxy
            val proxy = com.fabian.downloader.ui.AppSettings.proxyUrl
            if (proxy.isNotEmpty()) {
                addOption("--proxy", proxy)
            }

            // Argumentos Personalizados Libres (Estilo Seal/YTDLnis)
            val customArgs = com.fabian.downloader.ui.AppSettings.customArguments
            if (customArgs.isNotEmpty()) {
                try {
                    val allowedArgs = setOf(
                        "--sleep-requests", "--sleep-interval", "--max-sleep-interval", 
                        "--limit-rate", "--socket-timeout", "--abort-on-error", 
                        "--user-agent", "--referer", "--proxy", "--geo-verification-proxy", 
                        "--yes-playlist", "--no-playlist", "--flat-playlist"
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
                                Log.w("YtdlpDownloader", "Blocked unauthorized argument: $token")
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
                    Log.e("YtdlpDownloader", "Error parsing custom arguments", e)
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

        // Nivel 0: Intentar con la calidad / formato solicitados (y fallback interno de calidad)
        try {
            val request = createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, 0, customizeRequest)
            YoutubeDL.getInstance().execute(request, processId) { progreso, _, line ->
                lastLine = line
                var speedText = "Calculando..."
                var sizeText = "Descargando..."
                
                val match = SPEED_REGEX.find(line)
                if (match != null) {
                    speedText = match.groupValues[1]
                }
                
                val sizeMatch = SIZE_REGEX.find(line)
                if (sizeMatch != null) {
                    sizeText = sizeMatch.groupValues[1].replace("~", "")
                }
                
                if (progreso == 100f && speedText == "Calculando...") {
                    speedText = "Finalizando..."
                }
                
                alProgresar(progreso, sizeText, speedText)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.w("YtdlpDownloader", "Primer intento fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 1 (bestvideo+bestaudio/best)...")
            executionError = e
        }

        // Nivel 1: Intentar con mejor formato disponible sin limitación estricta de altura
        try {
            val request = createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, 1, customizeRequest)
            YoutubeDL.getInstance().execute(request, processId) { progreso, _, line ->
                lastLine = line
                var speedText = "Calculando..."
                var sizeText = "Descargando..."
                
                val match = SPEED_REGEX.find(line)
                if (match != null) {
                    speedText = match.groupValues[1]
                }
                
                val sizeMatch = SIZE_REGEX.find(line)
                if (sizeMatch != null) {
                    sizeText = sizeMatch.groupValues[1].replace("~", "")
                }
                
                if (progreso == 100f && speedText == "Calculando...") {
                    speedText = "Finalizando..."
                }
                
                alProgresar(progreso, sizeText, speedText)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.w("YtdlpDownloader", "Segundo intento fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 2 (best)...")
            executionError = e
        }

        // Nivel 2: Descargar formato absoluto básico 'best' (el formato más compatible soportado por cualquier extractor)
        try {
            val request = createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, 2, customizeRequest)
            YoutubeDL.getInstance().execute(request, processId) { progreso, _, line ->
                lastLine = line
                var speedText = "Calculando..."
                var sizeText = "Descargando..."
                
                val match = SPEED_REGEX.find(line)
                if (match != null) {
                    speedText = match.groupValues[1]
                }
                
                val sizeMatch = SIZE_REGEX.find(line)
                if (sizeMatch != null) {
                    sizeText = sizeMatch.groupValues[1].replace("~", "")
                }
                
                if (progreso == 100f && speedText == "Calculando...") {
                    speedText = "Finalizando..."
                }
                
                alProgresar(progreso, sizeText, speedText)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("YtdlpDownloader", "Todos los intentos de descarga fallaron para $videoUrl. Última línea: $lastLine", e)
            val errorMessage = lastLine.ifEmpty { e.message ?: executionError?.message ?: "Error desconocido" }
            throw Exception("Fallo: $errorMessage")
        }
    }
}
