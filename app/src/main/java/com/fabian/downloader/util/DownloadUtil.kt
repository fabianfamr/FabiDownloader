package com.fabian.downloader.util

import com.fabian.downloader.MyApplication
import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.BatteryOptimizerManager
import com.fabian.downloader.services.DownloadManagerService
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.DeviceOptimizationHelper

/**
 * NÚCLEO DE OPCIONES DEL MOTOR (estructura portada de Seal: `util/DownloadUtil.kt`)
 *
 * Contiene la traducción de preferencias de usuario a opciones de yt-dlp:
 *  - ordenador de formatos `-S` (más robusto que las expresiones `-f`),
 *  - descargador externo aria2c (`--downloader libaria2c.so`),
 *  - plantilla de salida con extensión real (`%(ext)s`),
 *  - cálculo de fragmentos concurrentes seguro según hardware/batería.
 *
 * Seal demuestra que con estas piezas yt-dlp descarga de forma fiable;
 * las expresiones `-f bv*[height<=N]+ba/...` que usaba el motor anterior son
 * las principales causantes de fallos en YouTube y sitios con formatos
 * separados, porque fallan en cadena cuando un único eslabón no existe.
 */
object DownloadUtil {

    /** Descargador externo empaquetado por la librería (igual que en Seal). */
    const val ARIA2C_LIBRARY = "libaria2c.so"

    /** Plantilla de salida: nombre fijo + extensión real decidida por yt-dlp. */
    const val OUTPUT_TEMPLATE_SUFFIX = ".%(ext)s"

    private val SPEED_REGEX = Regex("""at\s+([0-9.]+[a-zA-Z]+/s)""")
    private val SIZE_REGEX = Regex("""of\s+([~]?[0-9.]+[a-zA-Z]+)""")

    /** Extrae la velocidad textual ("2.5MiB/s") de una línea de progreso. */
    fun parseSpeed(line: String): String? = SPEED_REGEX.find(line)?.groupValues?.get(1)

    /** Extrae el tamaño textual ("50MiB") de una línea de progreso. */
    fun parseSize(line: String): String? = SIZE_REGEX.find(line)?.groupValues?.get(1)?.replace("~", "")

    /** Indica si una línea de log corresponde a post-procesado (merge, audio, ffmpeg...). */
    fun isPostProcessingLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("[extractaudio]") ||
            lower.contains("[merger]") ||
            lower.contains("[videoconvertor]") ||
            lower.contains("[ffmpeg]") ||
            lower.contains("[fixup") ||
            lower.contains("[postprocessor]") ||
            lower.contains("[embed") ||
            lower.contains("[sponsorblock]") ||
            lower.contains("deleting original file")
    }

    /** Devuelve true para formatos de audio (extracción con -x). */
    fun isAudioFormat(format: String): Boolean {
        val upper = format.uppercase()
        return upper == Config.FORMAT_MP3 || upper == Config.FORMAT_M4A ||
            upper == Config.FORMAT_OGG || upper == Config.FORMAT_WAV
    }

    /**
     * Traduce una calidad de la UI ("best", "1080p", ...) a la altura en píxeles.
     * "best" devuelve null para no restringir (yt-dlp elige lo mejor disponible).
     */
    fun qualityToResolution(quality: String): Int? {
        val digits = quality.filter { it.isDigit() }
        return digits.toIntOrNull()
    }

    /**
     * ORDENADOR DE FORMATOS estilo Seal (`-S`).
     *
     * A diferencia de `-f`, `-S` nunca falla si un formato concreto no existe:
     * yt-dlp ordena los formatos disponibles y elige el mejor que cumpla la
     * restricción de resolución. Para MP4 se prefiere h264 (compatibilidad de
     * hardware máxima en Android), igual que el preset "compatibilidad" de Seal.
     *
     * Ejemplos: "1080p" + MP4 -> "res:1080,vcodec:h264"
     */
    fun buildFormatSorter(quality: String, format: String): String? {
        val resolution = qualityToResolution(quality)
        val isBest = quality.trim().equals(Config.QUALITY_BEST, ignoreCase = true)
        if (isBest || resolution == null) return null

        val upperFormat = format.uppercase()
        return when {
            upperFormat == Config.FORMAT_MP4 -> "res:$resolution,vcodec:h264"
            upperFormat == Config.FORMAT_WEBM -> "res:$resolution"
            else -> "res:$resolution"
        }
    }

    /** Sorter de audio estilo Seal: preferir AAC para M4A. */
    fun buildAudioSorter(format: String): String? {
        return if (format.uppercase() == Config.FORMAT_M4A) "acodec:aac" else null
    }

    /**
     * Indica si se debe usar el descargador externo aria2c (motor rápido de Seal).
     * Con aria2c activo NO se usan `--concurrent-fragments` (igual que Seal).
     */
    fun shouldUseAria2c(): Boolean = AppSettings.useAria2c

    /**
     * Calcula un número de fragmentos concurrentes seguro según el hardware,
     * la batería y la carga actual de la cola. Puerto del builder anterior;
     * solo se consulta cuando aria2c está desactivado.
     */
    fun safeConcurrentFragments(): String {
        val appCtx = try {
            MyApplication.getInstance()
        } catch (_: Exception) {
            return "1"
        }

        return try {
            val settings = AppSettings
            val batteryManager = BatteryOptimizerManager.getInstance(appCtx)
            val isBatteryLowMode = settings.batteryOptimizationEnabled &&
                batteryManager.isBatteryLowAndNotCharging() &&
                settings.batteryLowAction == Config.BATTERY_ACTION_OPTIMIZE

            val tuning = DeviceOptimizationHelper.getAutoTuning()
            val activeCount = try {
                DownloadManagerService.getInstance(appCtx).getActiveDownloadsCount()
            } catch (_: Exception) {
                1
            }

            val userFragments = settings.concurrentFragments.toIntOrNull()
                ?: tuning.defaultConcurrentFragments
            val maxSafe = tuning.maxSafeFragments

            when {
                isBatteryLowMode -> 1
                activeCount > 2 -> (userFragments / 2).coerceIn(1, maxSafe)
                activeCount > 1 -> (userFragments * 3 / 4).coerceIn(1, maxSafe)
                else -> userFragments.coerceIn(1, maxSafe)
            }.toString()
        } catch (_: Exception) {
            "1"
        }
    }

    /**
     * Traduce el límite de velocidad de la UI a bytes/s para `--limit-rate`.
     * Devuelve null cuando es ilimitado o el valor no se reconoce.
     */
    fun maxSpeedToRateLimit(maxSpeed: String, isBatteryLowMode: Boolean): String? {
        val effective = if (isBatteryLowMode) {
            if (maxSpeed == Config.SPEED_UNLIMITED ||
                maxSpeed == Config.SPEED_5M ||
                maxSpeed == Config.SPEED_10M
            ) Config.SPEED_1M else maxSpeed
        } else {
            maxSpeed
        }
        if (effective == Config.SPEED_UNLIMITED) return null
        return when (effective) {
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
    }
}
