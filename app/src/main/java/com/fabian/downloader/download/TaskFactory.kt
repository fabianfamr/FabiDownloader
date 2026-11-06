package com.fabian.downloader.download

import android.util.Log
import com.fabian.downloader.MyApplication
import com.fabian.downloader.configs.Config
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.util.DownloadUtil
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/**
 * FÁBRICA DE TAREAS Y SOLICITUDES (estructura portada de Seal: `download/TaskFactory.kt`)
 *
 * Dos responsabilidades, igual que en Seal:
 *  1. `create` -> instancia una [DownloadTask] lista para encolar.
 *  2. `buildRequest` -> construye el `YoutubeDLRequest` con las opciones del
 *     motor de Seal: ordenador `-S`, descargador aria2c, plantilla de salida
 *     con `%(ext)s`, `--continue` para reanudar, y las preferencias de la UI
 *     (subs, capítulos, metadatos, SponsorBlock, límite de velocidad...).
 *
 * Sustituye al antiguo `YtdlpCommandBuilder`, cuyas expresiones `-f` en cadena
 * y su sufijo de salida `.downloading` provocaban fallos y archivos difíciles
 * de resolver tras la descarga.
 */
object TaskFactory {

    private const val TAG = "TaskFactory"

    /** Conjunto de argumentos personalizados permitidos (lista blanca, igual que antes). */
    private val ALLOWED_CUSTOM_ARGS = setOf(
        "--sleep-requests", "--sleep-interval", "--max-sleep-interval",
        "--limit-rate", "--socket-timeout", "--abort-on-error",
        "--user-agent", "--referer", "--proxy", "--geo-verification-proxy",
        "--yes-playlist", "--no-playlist", "--flat-playlist",
        "--buffer-size", "--http-chunk-size", "--concurrent-fragments",
        "--throttled-rate", "--retries", "--fragment-retries"
    )

    fun create(
        url: String,
        quality: String,
        format: String,
        destFolder: File,
        fileNameWithoutExt: String,
        taskId: String,
        title: String = "",
        type: DownloadTask.TypeInfo = DownloadTask.TypeInfo.URL,
    ): DownloadTask = DownloadTask(
        url = url,
        quality = quality,
        format = format,
        destFolder = destFolder,
        fileNameWithoutExt = fileNameWithoutExt,
        taskId = taskId,
        title = title,
        type = type,
    )

    /**
     * Construye la solicitud de descarga.
     *
     * @param fallbackLevel 0 = motor por defecto de yt-dlp (clientes mantenidos
     *        por el proyecto, igual que Seal). 1 = reintento con clientes
     *        alternativos de YouTube (ios,mweb) para casos de bot-detection.
     */
    fun buildRequest(
        task: DownloadTask,
        fallbackLevel: Int = 0,
        customizer: ((YoutubeDLRequest) -> Unit)? = null
    ): YoutubeDLRequest {
        val settings = AppSettings
        val isYoutube = com.fabian.downloader.utils.UrlUtils.isYoutubeUrl(task.url)
        val isImage = task.format.equals(Config.FORMAT_JPG, ignoreCase = true) ||
            task.format.equals(Config.FORMAT_PNG, ignoreCase = true) ||
            task.format.equals(Config.FORMAT_WEBP, ignoreCase = true) ||
            task.format.equals("JPEG", ignoreCase = true)

        return YoutubeDLRequest(task.url).apply {
            // ------------------------------------------------------------------
            // Base común (Seal): --no-mtime + reanudación nativa con .part
            // ------------------------------------------------------------------
            addOption("--no-mtime")
            addOption("--continue")

            if (isImage) {
                // Las imágenes no pasan por yt-dlp en este motor (igual que antes).
            } else if (DownloadUtil.isAudioFormat(task.format)) {
                // ----------------------------------------------------------------
                // AUDIO: extracción con -x (estilo Seal addOptionsForAudioDownloads)
                // ----------------------------------------------------------------
                addOption("-x")
                when (task.format.uppercase()) {
                    Config.FORMAT_MP3 -> {
                        addOption("--audio-format", "mp3")
                        val defaultBitrateDigits = settings.defaultAudioBitrate.filter { it.isDigit() }.ifEmpty { "320" }
                        val selectedBitrateDigits = task.quality.filter { it.isDigit() }
                        val finalBitrate = selectedBitrateDigits.ifEmpty { defaultBitrateDigits }
                        addOption("--audio-quality", "${finalBitrate}k")
                    }
                    Config.FORMAT_M4A -> {
                        addOption("--audio-format", "m4a")
                        // Preset M4A de Seal: priorizar AAC nativo.
                        DownloadUtil.buildAudioSorter(task.format)?.let { addOption("-S", it) }
                    }
                    Config.FORMAT_OGG -> addOption("--audio-format", "ogg")
                    Config.FORMAT_WAV -> addOption("--audio-format", "wav")
                    else -> addOption("--audio-format", "mp3")
                }
                // En reintento, forzar el mejor audio disponible sin restricciones.
                if (fallbackLevel > 0) {
                    addOption("-f", "bestaudio/best")
                }
            } else {
                // ----------------------------------------------------------------
                // VIDEO: ordenador -S (estilo Seal) + contenedor de destino
                // ----------------------------------------------------------------
                DownloadUtil.buildFormatSorter(task.quality, task.format)?.let { addOption("-S", it) }
                val targetFormat = task.format.lowercase()
                val supportedVideoFormats = listOf("mp4", "mkv", "webm")
                if (supportedVideoFormats.contains(targetFormat)) {
                    addOption("--merge-output-format", targetFormat)
                }
                // En reintento sin ordenador: dejar que yt-dlp elija lo mejor.
                if (fallbackLevel > 0) {
                    addOption("-f", "bv*+ba/b")
                }
            }

            // ------------------------------------------------------------------
            // YouTube: clientes por defecto en el nivel 0 (los que el proyecto
            // yt-dlp mantiene); en reintentos se prueba con clientes alternativos.
            // ------------------------------------------------------------------
            if (isYoutube && fallbackLevel > 0) {
                addOption("--extractor-args", "youtube:player_client=ios,mweb")
            }

            // ------------------------------------------------------------------
            // Motor de descarga: aria2c (Seal) o fragmentos concurrentes seguros
            // ------------------------------------------------------------------
            if (DownloadUtil.shouldUseAria2c()) {
                addOption("--downloader", DownloadUtil.ARIA2C_LIBRARY)
            } else {
                addOption("--concurrent-fragments", DownloadUtil.safeConcurrentFragments())
            }

            // ------------------------------------------------------------------
            // Rutas: directorio de destino + plantilla con extensión real.
            // FIX crítico: antes se usaba "<nombre>.downloading" sin %(ext)s,
            // lo que producía archivos "titulo.downloading.mp4" difíciles de
            // resolver y confundía al post-procesado. Ahora yt-dlp escribe
            // directamente "titulo.mp4" y deja ".part" durante el proceso.
            // ------------------------------------------------------------------
            addOption("-o", "${task.destFolder.absolutePath}/${task.fileNameWithoutExt}${DownloadUtil.OUTPUT_TEMPLATE_SUFFIX}")

            // ------------------------------------------------------------------
            // Listas de reproducción
            // ------------------------------------------------------------------
            when (val typeInfo = task.type) {
                is DownloadTask.TypeInfo.Playlist -> addOption("--playlist-items", typeInfo.index)
                DownloadTask.TypeInfo.URL -> {
                    if (!settings.playlistEnabled) {
                        addOption("--no-playlist")
                    }
                }
            }

            // ------------------------------------------------------------------
            // Integraciones opcionales de la UI (subs, capítulos, metadatos...)
            // ------------------------------------------------------------------
            if (settings.embedChapters && !DownloadUtil.isAudioFormat(task.format)) {
                addOption("--embed-chapters")
            }

            if (settings.embedSubtitles && !DownloadUtil.isAudioFormat(task.format)) {
                addOption("--embed-subs")
                addOption("--write-subs")
                addOption("--sub-langs", "all")
            }

            if (settings.embedMetadata) {
                addOption("--embed-metadata")
                addOption("--parse-metadata", "%(uploader,artist)s:%(album)s")
                if (settings.markAsMV) {
                    addOption("--parse-metadata", "Music Video:%(genre)s")
                }
            }
            if (settings.embedThumbnail) {
                addOption("--embed-thumbnail")
            }

            if (settings.sponsorBlockEnabled) {
                addOption("--sponsorblock-remove", "sponsor,intro,outro,selfpromo,interaction")
            }

            // ------------------------------------------------------------------
            // Red: límite de velocidad, geo-bypass, SSL, timeouts y reintentos
            // (yt-dlp ya reintenta por sí mismo; no hace falta watchdog externo)
            // ------------------------------------------------------------------
            val isBatteryLowMode = try {
                val batteryManager =
                    com.fabian.downloader.managers.BatteryOptimizerManager.getInstance(MyApplication.getInstance())
                settings.batteryOptimizationEnabled &&
                    batteryManager.isBatteryLowAndNotCharging() &&
                    settings.batteryLowAction == Config.BATTERY_ACTION_OPTIMIZE
            } catch (_: Exception) {
                false
            }

            DownloadUtil.maxSpeedToRateLimit(settings.maxSpeed, isBatteryLowMode)?.let {
                addOption("--limit-rate", it)
            }

            if (settings.bypassGeo) {
                addOption("--geo-bypass")
            }
            if (AppSettings.bypassSslVerification) {
                addOption("--no-check-certificate")
            }

            addOption("--socket-timeout", "10")
            addOption("--retries", "3")
            addOption("--fragment-retries", "5")
            addOption("--extractor-retries", "2")
            addOption("--no-cache-dir")
            addOption("--no-update")
            addOption("--no-warnings")

            // ------------------------------------------------------------------
            // Cookies, User-Agent y Referer
            // ------------------------------------------------------------------
            val cookiesFile = File(MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }

            if (isYoutube) {
                val customUa = settings.customUserAgent
                if (customUa.isNotEmpty()) {
                    addOption("--user-agent", customUa)
                }
            } else {
                addOption("--referer", Config.REFERER_DEFAULT)
            }

            // ------------------------------------------------------------------
            // Argumentos personalizados (lista blanca)
            // ------------------------------------------------------------------
            applyCustomArguments(settings.customArguments)

            // ------------------------------------------------------------------
            // Personalización por sitio (SiteService.customizeDownloaderRequest)
            // ------------------------------------------------------------------
            try {
                customizer?.invoke(this)
            } catch (e: Exception) {
                Log.w(TAG, "Error aplicando personalización por sitio: ${e.message}")
            }
        }
    }

    private fun YoutubeDLRequest.applyCustomArguments(customArgs: String) {
        if (customArgs.isEmpty()) return
        try {
            val tokens = customArgs.trim().split(Regex("\\s+"))
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                if (token.startsWith("--")) {
                    if (ALLOWED_CUSTOM_ARGS.contains(token)) {
                        if (i + 1 < tokens.size && !tokens[i + 1].startsWith("-")) {
                            addOption(token, tokens[i + 1])
                            i += 2
                        } else {
                            addOption(token)
                            i += 1
                        }
                    } else {
                        Log.w(TAG, "Argumento no autorizado bloqueado: $token")
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
            Log.e(TAG, "Error procesando argumentos personalizados", e)
        }
    }
}
