package com.fabian.downloader.services

import android.util.Log
import com.fabian.downloader.MyApplication
import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.BatteryOptimizerManager
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.DeviceOptimizationHelper
import com.fabian.downloader.utils.UrlUtils
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

object YtdlpCommandBuilder {

    fun createRequest(
        videoUrl: String,
        quality: String,
        format: String,
        destFolder: File,
        fileNameWithoutExt: String,
        fallbackLevel: Int,
        customizeRequest: ((YoutubeDLRequest) -> Unit)? = null
    ): YoutubeDLRequest {
        val lowerUrl = videoUrl.lowercase()
        val isYoutube = UrlUtils.isYoutubeUrl(videoUrl)
        val settings = AppSettings

        return YoutubeDLRequest(videoUrl).apply {
            val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || 
                          format.equals(Config.FORMAT_PNG, ignoreCase = true) || 
                          format.equals(Config.FORMAT_WEBP, ignoreCase = true) || 
                          format.equals("JPEG", ignoreCase = true)

            if (isImage) {
                // Descarga de imágenes deshabilitada
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
                addOption("--audio-quality", "${finalAudioQuality}k")
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
            }

            if (isYoutube) {
                when (fallbackLevel) {
                    0 -> addOption("--extractor-args", "youtube:player_client=ios,mweb")
                    1 -> addOption("--extractor-args", "youtube:player_client=ios,web")
                    2 -> addOption("--extractor-args", "youtube:player_client=android_creator,mweb")
                    else -> { /* omit player_client for raw yt-dlp fallback */ }
                }
            }

            if (settings.embedChapters) {
                addOption("--embed-chapters")
            }

            addOption("-o", "${destFolder.absolutePath}/$fileNameWithoutExt.downloading")
            addOption("--no-part")

            val cookiesFile = File(MyApplication.getInstance().filesDir, Config.COOKIES_FILE_NAME)
            if (cookiesFile.exists() && cookiesFile.length() > 0) {
                addOption("--cookies", cookiesFile.absolutePath)
            }

            // Paralelismo y velocidad
            val appCtx = MyApplication.getInstance()
            val batteryManager = BatteryOptimizerManager.getInstance(appCtx)
            val isBatteryLowMode = settings.batteryOptimizationEnabled && 
                                   batteryManager.isBatteryLowAndNotCharging() && 
                                   settings.batteryLowAction == "Optimizar recursos"

            val brandTuning = DeviceOptimizationHelper.getAutoTuning()

            val activeCount = try {
                DownloadManagerService.getInstance(appCtx).getActiveDownloadsCount()
            } catch (e: Exception) {
                1
            }
            val userFragments = settings.concurrentFragments.toIntOrNull() ?: brandTuning.defaultConcurrentFragments
            val safeFragments = when {
                isBatteryLowMode -> 2
                activeCount > 2 -> userFragments.coerceAtMost(3)
                activeCount > 1 -> userFragments.coerceAtMost(4)
                else -> userFragments.coerceAtMost(6)
            }
            addOption("--concurrent-fragments", safeFragments.toString())

            addOption("--buffer-size", brandTuning.bufferSize)
            addOption("--http-chunk-size", brandTuning.httpChunkSize)

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

            addOption("--socket-timeout", "10")
            addOption("--retries", "3")
            addOption("--fragment-retries", "5")
            addOption("--extractor-retries", "2")
            addOption("--file-access-retries", "2")
            addOption("--retry-sleep", "fragment:1")
            addOption("--no-cache-dir")
            addOption("--no-update")

            if (isYoutube) {
                val customUa = settings.customUserAgent
                if (customUa.isNotEmpty()) {
                    addOption("--user-agent", customUa)
                }
            }

            addOption("--referer", Config.REFERER_DEFAULT)
            if (AppSettings.bypassSslVerification) {
                addOption("--no-check-certificate")
            }
            addOption("--no-warnings")

            if (settings.embedMetadata) {
                addOption("--embed-metadata")
                addOption("--parse-metadata", "%(uploader,artist)s:%(album)s")
            }
            if (settings.embedThumbnail) {
                addOption("--embed-thumbnail")
            }
            addOption("--no-write-thumbnail")

            if (settings.sponsorBlockEnabled) {
                addOption("--sponsorblock-remove", "sponsor,intro,outro,selfpromo,interaction")
            }

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

            customizeRequest?.invoke(this)
        }
    }
}
