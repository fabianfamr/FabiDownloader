package com.fabian.downloader.services

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class YtdlpDownloader {

    companion object {
        private val SPEED_REGEX = Regex("""at\s+([0-9.]+[a-zA-Z]+/s)""")
        private val SIZE_REGEX = Regex("""of\s+([~]?[0-9.]+[a-zA-Z]+)""")

        fun resolveUserFacingError(e: Throwable, lastLine: String): String {
            return YtdlpErrorResolver.resolveUserFacingError(e, lastLine)
        }
    }

    suspend fun descargar(
        rawVideoUrl: String,
        quality: String,
        format: String,
        destFolder: File,
        fileNameWithoutExt: String,
        processId: String = UUID.randomUUID().toString(),
        customizeRequest: ((YoutubeDLRequest) -> Unit)? = null,
        alProgresar: (Float, String, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val coroutineScope = this
        val videoUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(rawVideoUrl)
        com.fabian.downloader.MyApplication.getInstance().ensureInitialized()
        var lastLine = ""
        var executionError: Exception? = null

        val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || 
                      format.equals(Config.FORMAT_PNG, ignoreCase = true) || 
                      format.equals(Config.FORMAT_WEBP, ignoreCase = true) || 
                      format.equals("JPEG", ignoreCase = true)

        if (isImage) {
            Log.w(Config.TAG_YTDLP_DOWNLOADER, "Descarga de imágenes deshabilitada")
            return@withContext false
        }

        // Helper: destroy previous process
        suspend fun cleanupBeforeRetry(isNetworkRetry: Boolean = false) {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Could not destroy previous process before retry", e)
            }
            kotlinx.coroutines.delay(150)
            
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

        val autoRetry = com.fabian.downloader.ui.AppSettings.autoRetry
        val connService = com.fabian.downloader.network.ConnectionService()

        suspend fun isExplicitCancellation(e: Throwable): Boolean {
            if (e is kotlinx.coroutines.CancellationException) return true
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) return true
            
            val isPausedOrDeleted = try {
                val idLong = processId.toLongOrNull()
                if (idLong != null) {
                    val record = StorageService.getInstance(
                        com.fabian.downloader.MyApplication.getInstance()
                    ).getDownloadById(idLong)
                    record == null || record.isPaused
                } else false
            } catch (_: Exception) {
                false
            }
            if (isPausedOrDeleted) return true

            val lowerMsg = (e.message ?: "").lowercase()
            val lowerClass = e.javaClass.name.lowercase()
            val cancelKeywords = listOf(
                "process destroyed", "destroyed", "canceledexception", "canceled", "cancelled",
                "exit code 143", "sigterm", "sigkill"
            )
            return cancelKeywords.any { lowerMsg.contains(it) || lowerClass.contains(it) }
        }

        suspend fun executeWithRetry(
            level: Int,
            onFailAction: suspend (Exception) -> Unit
        ): Boolean {
            var attempt = 0
            val maxAttempts = if (autoRetry) 2 else 1
            while (attempt < maxAttempts) {
                if (!coroutineContext.isActive || isExplicitCancellation(kotlinx.coroutines.CancellationException())) {
                    throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
                }
                
                if (attempt > 0) {
                    cleanupBeforeRetry(isNetworkRetry = true)
                } else {
                    try {
                        YoutubeDL.getInstance().destroyProcessById(processId)
                    } catch (_: Exception) {}
                }

                val timedOutByWatchdog = java.util.concurrent.atomic.AtomicBoolean(false)
                val hasReceivedAnyOutput = java.util.concurrent.atomic.AtomicBoolean(false)
                var lastActivityTime = System.currentTimeMillis()
                var watchdogJob: kotlinx.coroutines.Job? = null

                try {
                    val request = YtdlpCommandBuilder.createRequest(videoUrl, quality, format, destFolder, fileNameWithoutExt, level, customizeRequest)
                    var lastUiUpdate = 0L
                    var lastReportedProgress = -1f
                    var maxObservedRawProgress = 0f
                    var isSecondTrack = false
                    var lastDetectedSpeed = Config.STATUS_CONNECTING
                    var lastDetectedSize = Config.STATUS_CALCULATING

                    alProgresar(0f, Config.STATUS_CALCULATING, Config.STATUS_CONNECTING)

                    watchdogJob = coroutineScope.launch(Dispatchers.IO) {
                        while (isActive) {
                            kotlinx.coroutines.delay(2000)
                            val elapsed = System.currentTimeMillis() - lastActivityTime
                            val maxWait = if (!hasReceivedAnyOutput.get()) 30_000L else 45_000L
                            if (elapsed > maxWait) {
                                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Watchdog timeout ($elapsed ms) en proceso $processId (level=$level). Abortando para siguiente nivel...")
                                timedOutByWatchdog.set(true)
                                try {
                                    YoutubeDL.getInstance().destroyProcessById(processId)
                                } catch (_: Exception) {}
                                break
                            }
                        }
                    }

                    YoutubeDL.getInstance().execute(request, processId) { rawProgress, _, line ->
                        lastActivityTime = System.currentTimeMillis()
                        hasReceivedAnyOutput.set(true)
                        lastLine = line
                        Log.d(Config.TAG_YTDLP_DOWNLOADER, "[$processId] $line")
                        val lowerLine = line.lowercase()
                        val isPostProc = lowerLine.contains("[extractaudio]") ||
                                         lowerLine.contains("[merger]") ||
                                         lowerLine.contains("[videoconvertor]") ||
                                         lowerLine.contains("[ffmpeg]") ||
                                         lowerLine.contains("[fixup") ||
                                         lowerLine.contains("[postprocessor]") ||
                                         lowerLine.contains("[embed") ||
                                         lowerLine.contains("[sponsorblock]") ||
                                         lowerLine.contains("deleting original file")

                        val match = SPEED_REGEX.find(line)
                        if (match != null) {
                            lastDetectedSpeed = match.groupValues[1]
                        }

                        val sizeMatch = SIZE_REGEX.find(line)
                        if (sizeMatch != null) {
                            lastDetectedSize = sizeMatch.groupValues[1].replace("~", "")
                        }

                        if (!isSecondTrack && maxObservedRawProgress >= 80f && rawProgress < 20f && (lowerLine.contains("[download] destination") || rawProgress > 0f)) {
                            isSecondTrack = true
                            maxObservedRawProgress = 0f
                        }

                        if (rawProgress > maxObservedRawProgress) {
                            maxObservedRawProgress = rawProgress
                        }

                        val computedProgress: Float = when {
                            isPostProc -> 99f
                            isSecondTrack -> {
                                85f + (maxObservedRawProgress * 0.13f)
                            }
                            else -> {
                                maxObservedRawProgress.coerceIn(0f, 98f)
                            }
                        }

                        val smoothedProgress = if (computedProgress >= lastReportedProgress || isPostProc) {
                            computedProgress
                        } else {
                            lastReportedProgress
                        }

                        val now = System.currentTimeMillis()
                        val isProgressAdvanced = (smoothedProgress - lastReportedProgress) >= 0.5f
                        val isMilestone = smoothedProgress == 0f || smoothedProgress >= 99f || isPostProc
                        val isTimeElapsed = now - lastUiUpdate >= 250

                        if (isMilestone || (isProgressAdvanced && now - lastUiUpdate >= 150) || isTimeElapsed) {
                            lastUiUpdate = now
                            lastReportedProgress = smoothedProgress

                            var speedText = lastDetectedSpeed
                            var sizeText = lastDetectedSize

                            if (smoothedProgress >= 98f || isPostProc) {
                                speedText = Config.STATUS_FINALIZING
                            } else if (speedText == Config.STATUS_CONNECTING && (smoothedProgress > 0f || lowerLine.contains("[download]"))) {
                                speedText = Config.STATUS_DOWNLOADING
                            }

                            val cleanSpeed = com.fabian.downloader.utils.YtdlpParser.formatSpeed(speedText)
                            val cleanSize = com.fabian.downloader.utils.YtdlpParser.formatSize(sizeText)

                            alProgresar(smoothedProgress, cleanSize, cleanSpeed)
                        }
                    }
                    return true
                } catch (originalEx: Throwable) {
                    watchdogJob?.cancel()
                    val e: Throwable = if (timedOutByWatchdog.get()) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Timeout de watchdog confirmado para proceso $processId en nivel $level. Forzando reintento/fallback...")
                        java.net.SocketTimeoutException("Tiempo de espera agotado al conectar con el servidor")
                    } else {
                        originalEx
                    }

                    val lowerMsg = (e.message ?: "").lowercase()
                    val lowerLast = lastLine.lowercase()
                    
                    if (!timedOutByWatchdog.get() && isExplicitCancellation(e)) {
                        throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
                    }
                    
                    attempt++
                    Log.w(Config.TAG_YTDLP_DOWNLOADER, "Intento $attempt/$maxAttempts fallido para nivel $level (${e.javaClass.simpleName}: ${e.message}). Notificando reintento...")
                    
                    alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                    
                    val isNotInitialized = lowerMsg.contains("not initialized") || lowerLast.contains("not initialized") || (e is IllegalStateException && lowerMsg.contains("initialized"))
                    val isCorruptBinary = lowerMsg.contains("zipimport") || lowerLast.contains("zipimport") ||
                                          lowerMsg.contains("bad local file header") || lowerLast.contains("bad local file header") ||
                                          lowerMsg.contains("cannot link executable") || lowerLast.contains("cannot link executable") ||
                                          lowerMsg.contains("libandroid-support") || lowerLast.contains("libandroid-support") ||
                                          lowerMsg.contains("cannot link") || lowerLast.contains("cannot link")
                    if (isNotInitialized) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "YoutubeDL no inicializado durante descarga. Auto-recuperando...")
                        val appCtx = com.fabian.downloader.MyApplication.getInstance()
                        appCtx.ensureInitialized(appCtx)
                    } else if (isCorruptBinary) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Detectado binario yt-dlp corrupto durante descarga. Ejecutando reset de emergencia desde APK assets...")
                        val appCtx = com.fabian.downloader.MyApplication.getInstance()
                        appCtx.resetAndReinitYtdlp(appCtx)
                    } else if (YtdlpErrorResolver.isExtractorOrCipherError(e, lastLine)) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Detectada incompatibilidad de extractor/firma. Solicitando verificación silenciosa con ahorro de Wi-Fi...")
                        val appCtx = com.fabian.downloader.MyApplication.getInstance()
                        coroutineScope.launch(Dispatchers.IO) {
                            com.fabian.downloader.managers.YtdlpUpdateManager.autoUpdateSilentlyOnFailure(appCtx)
                        }
                    }

                    if (YtdlpErrorResolver.isFatalUnrecoverableError(e, lastLine)) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Error irrecuperable detectado ($lastLine). Cancelando intentos y fallbacks inmediatamente.")
                        onFailAction(if (e is Exception) e else Exception(e))
                        return false
                    }

                    val hasInternet = connService.checkConnection()
                    val isNetworkOrIoError = !hasInternet || YtdlpErrorResolver.isNetworkOrTemporaryError(e, lastLine)

                    if (isNetworkOrIoError && attempt < maxAttempts) {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Intento $attempt fallido por error de red/I/O. Esperando breve recuperación...")
                        var secondsWaited = 0
                        while (secondsWaited < 5 && !connService.checkConnection()) {
                            if (!coroutineContext.isActive || isExplicitCancellation(e)) throw kotlinx.coroutines.CancellationException("Descarga cancelada/pausada")
                            kotlinx.coroutines.delay(1000)
                            secondsWaited++
                        }
                        
                        if (connService.checkConnection()) {
                            Log.i(Config.TAG_YTDLP_DOWNLOADER, "Reintentando el mismo nivel $level (intento ${attempt + 1})...")
                            cleanupBeforeRetry(isNetworkRetry = true)
                            kotlinx.coroutines.delay(500)
                            continue
                        }
                    }

                    if (attempt >= maxAttempts) {
                        onFailAction(if (e is Exception) e else Exception(e))
                    } else {
                        Log.w(Config.TAG_YTDLP_DOWNLOADER, "Intento $attempt fallido. Reintentando...")
                        cleanupBeforeRetry(isNetworkRetry = true)
                        kotlinx.coroutines.delay(500)
                    }
                } finally {
                    watchdogJob?.cancel()
                    try {
                        YoutubeDL.getInstance().destroyProcessById(processId)
                    } catch (_: Exception) {}
                }
            }
            return false
        }

        try {
            // Nivel 0: Intentar con la calidad / formato solicitados
            val success0 = executeWithRetry(0) { e ->
                executionError = e
                if (autoRetry && !YtdlpErrorResolver.isFatalUnrecoverableError(e, lastLine)) {
                    Log.w(Config.TAG_YTDLP_DOWNLOADER, "Primer nivel fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 1...")
                    alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                    cleanupBeforeRetry()
                } else {
                    val finalError = executionError ?: e
                    val errorMessage = resolveUserFacingError(finalError, lastLine)
                    throw Exception(Config.STATUS_FAILED_PREFIX + errorMessage)
                }
            }
            if (success0) return@withContext true
            if (!autoRetry || YtdlpErrorResolver.isFatalUnrecoverableError(executionError ?: Exception(), lastLine)) {
                val finalError = executionError ?: Exception(lastLine.ifEmpty { "Error en la descarga" })
                val errorMessage = resolveUserFacingError(finalError, lastLine)
                throw Exception(Config.STATUS_FAILED_PREFIX + errorMessage)
            }

            // Nivel 1: Fallback 1
            val success1 = executeWithRetry(1) { e ->
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Segundo nivel fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 2...")
                executionError = e
                alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                cleanupBeforeRetry()
            }
            if (success1) return@withContext true

            // Nivel 2: Fallback 2
            val success2 = executeWithRetry(2) { e ->
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Tercer nivel fallido para $videoUrl: ${e.message}. Reintentando nivel de fallback 3 (best/b)...")
                executionError = e
                alProgresar(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                cleanupBeforeRetry()
            }
            if (success2) return@withContext true

            // Nivel 3: Fallback 3
            val success3 = executeWithRetry(3) { e ->
                Log.e(Config.TAG_YTDLP_DOWNLOADER, "Todos los niveles e intentos de descarga fallaron para $videoUrl. Última línea: $lastLine", e)
                executionError = e
            }
            if (success3) return@withContext true

            val genericError = try {
                val appCtx = com.fabian.downloader.MyApplication.getInstance()
                com.fabian.downloader.utils.LocaleHelper.applyLocale(appCtx, com.fabian.downloader.ui.AppSettings.language)
                    .getString(com.fabian.downloader.R.string.downloads_error_generic)
            } catch (_: Exception) { "Error" }
            val finalError = executionError ?: Exception(lastLine.ifEmpty { genericError })
            val errorMessage = resolveUserFacingError(finalError, lastLine)
            throw Exception(Config.STATUS_FAILED_PREFIX + errorMessage)
        } finally {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                Log.w(Config.TAG_YTDLP_DOWNLOADER, "Could not destroy process $processId in final cleanup", e)
            }
        }
    }
}
