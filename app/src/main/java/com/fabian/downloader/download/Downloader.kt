package com.fabian.downloader.download

import android.util.Log
import com.fabian.downloader.MyApplication
import com.fabian.downloader.configs.Config
import com.fabian.downloader.network.ConnectionService
import com.fabian.downloader.services.YtdlpErrorResolver
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.YtdlpParser
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fabian.downloader.util.DownloadUtil

/**
 * EJECUTOR DEL MOTOR (estructura portada de Seal: `Downloader.kt` / `DownloaderV2Impl`)
 *
 * Filosofía de Seal: UNA ejecución limpia por intento, con el proceso
 * identificado por taskId y cancelable vía `destroyProcessById`. Sin watchdogs
 * que maten descargas sanas (el antiguo watchdog abortaba el proceso a los
 * 35-60s sin salida, cuando yt-dlp puede tardar más resolviendo retos JS),
 * sin cascadas de 4 niveles y sin borrado de parciales: los ".part" se
 * reutilizan gracias a `--continue`.
 *
 * El progreso se reporta con el mismo contrato que la UI ya espera:
 * `(progress: Float, sizeText: String, speedText: String)`.
 */
object Downloader {

    private const val TAG = "DownloadEngine"

    /**
     * Descarga una tarea con el motor de yt-dlp.
     *
     * @return true si la descarga se completó; lanza excepción en caso contrario.
     * @throws CancellationException si la tarea fue pausada, cancelada o borrada.
     * @throws Exception con prefijo [Config.STATUS_FAILED_PREFIX] y mensaje
     *         apto para el usuario si la descarga falló de forma irrecuperable.
     */
    suspend fun downloadVideo(
        task: DownloadTask,
        customizer: ((com.yausername.youtubedl_android.YoutubeDLRequest) -> Unit)? = null,
        onProgress: (progress: Float, sizeText: String, speedText: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        MyApplication.getInstance().ensureInitialized()

        val connectionService = ConnectionService()
        var lastLine = ""
        var lastError: Throwable = Exception("Error en la descarga")

        // Imágenes: yt-dlp no gestiona descargas de imágenes; se preserva el
        // comportamiento del motor anterior (fallo controlado, sin proceso).
        val upperFormat = task.format.uppercase()
        if (upperFormat == Config.FORMAT_JPG || upperFormat == Config.FORMAT_PNG ||
            upperFormat == Config.FORMAT_WEBP || upperFormat == "JPEG"
        ) {
            Log.w(TAG, "Descarga de imágenes no soportada por el motor yt-dlp (taskId=${task.taskId})")
            return@withContext false
        }

        // Asegurar que no quede un proceso huérfano con el mismo id.
        destroyQuietly(task.taskId)

        // Nivel 0 = motor por defecto de yt-dlp (como Seal).
        // Nivel 1 = reintento con clientes alternativos de YouTube.
        val maxLevels = if (AppSettings.autoRetry) 2 else 1
        var level = 0

        while (level < maxLevels) {
            throwIfCancelled()
            destroyQuietly(task.taskId)

            try {
                val request = TaskFactory.buildRequest(task, fallbackLevel = level, customizer = customizer)

                onProgress(0f, Config.STATUS_CALCULATING, Config.STATUS_CONNECTING)
                Log.i(TAG, "Iniciando descarga taskId=${task.taskId} (nivel=$level, url=${task.url})")

                var lastUiUpdate = 0L
                var lastReportedProgress = -1f
                var maxObservedRawProgress = 0f
                var isSecondTrack = false
                var lastDetectedSpeed = Config.STATUS_CONNECTING
                var lastDetectedSize = Config.STATUS_CALCULATING

                YoutubeDL.getInstance().execute(request, task.taskId) { rawProgress, _, line ->
                    lastLine = line
                    Log.d(TAG, "[${task.taskId}] $line")

                    DownloadUtil.parseSpeed(line)?.let { lastDetectedSpeed = it }
                    DownloadUtil.parseSize(line)?.let { lastDetectedSize = it }

                    val isPostProc = DownloadUtil.isPostProcessingLine(line)

                    // Descarga en dos pistas (video + audio): al terminar la
                    // primera (~100%), la segunda reinicia el progreso bruto.
                    if (!isSecondTrack && maxObservedRawProgress >= 80f &&
                        rawProgress < 20f && lowerContainsDownload(line)
                    ) {
                        isSecondTrack = true
                        maxObservedRawProgress = 0f
                    }
                    if (rawProgress > maxObservedRawProgress) {
                        maxObservedRawProgress = rawProgress
                    }

                    val computedProgress: Float = when {
                        isPostProc -> 99f
                        isSecondTrack -> 85f + (maxObservedRawProgress * 0.13f)
                        else -> maxObservedRawProgress.coerceIn(0f, 98f)
                    }

                    val smoothedProgress =
                        if (computedProgress >= lastReportedProgress || isPostProc) computedProgress
                        else lastReportedProgress

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
                        } else if (speedText == Config.STATUS_CONNECTING &&
                            (smoothedProgress > 0f || line.lowercase().contains("[download]"))
                        ) {
                            speedText = Config.STATUS_DOWNLOADING
                        }

                        val cleanSpeed = YtdlpParser.formatSpeed(speedText)
                        val cleanSize = YtdlpParser.formatSize(sizeText)
                        onProgress(smoothedProgress, cleanSize, cleanSpeed)
                    }
                }

                // Éxito: salida limpia como Seal.
                destroyQuietly(task.taskId)
                return@withContext true
            } catch (t: Throwable) {
                lastError = t
                // ----------------------------------------------------------------
                // Cancelaciones (pausa/borrado desde la UI o notificaciones)
                // ----------------------------------------------------------------
                if (t is YoutubeDL.CanceledException) {
                    throw CancellationException("Descarga cancelada/pausada")
                }
                if (t is CancellationException) throw t
                if (!isActive) {
                    throw CancellationException("Descarga cancelada/pausada")
                }

                // ----------------------------------------------------------------
                // Fallo: reintento del mismo nivel solo si es un error de red
                // transitorio y se recupera la conectividad (espera breve).
                // ----------------------------------------------------------------
                destroyQuietly(task.taskId)

                if (YtdlpErrorResolver.isFatalUnrecoverableError(t, lastLine)) {
                    Log.w(TAG, "Error irrecuperable (taskId=${task.taskId}): $lastLine")
                    throw failed(t, lastLine)
                }

                // Auto-sanado (comportamiento conservado del motor anterior):
                // si el extractor/firma quedó desactualizado, verificar en segundo
                // plano si existe una versión más nueva de yt-dlp y actualizarla.
                if (YtdlpErrorResolver.isExtractorOrCipherError(t, lastLine)) {
                    try {
                        val appCtx = MyApplication.getInstance()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                com.fabian.downloader.managers.YtdlpUpdateManager
                                    .autoUpdateSilentlyOnFailure(appCtx)
                            } catch (updEx: Exception) {
                                Log.w(TAG, "Auto-actualización de yt-dlp falló: ${updEx.message}")
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                val isNetworkOrTemporary = !connectionService.checkConnection() ||
                    YtdlpErrorResolver.isNetworkOrTemporaryError(t, lastLine)

                if (isNetworkOrTemporary) {
                    Log.w(TAG, "Error de red transitorio (taskId=${task.taskId}): ${t.message}")
                    var secondsWaited = 0
                    while (secondsWaited < 5 && !connectionService.checkConnection()) {
                        throwIfCancelled()
                        delay(1000)
                        secondsWaited++
                    }
                    if (connectionService.checkConnection()) {
                        onProgress(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                        delay(500)
                        continue // Reintenta el MISMO nivel (resume con .part)
                    }
                }

                // Sin reintento posible en este nivel: pasar al siguiente.
                if (level + 1 < maxLevels) {
                    Log.w(TAG, "Nivel $level fallido (taskId=${task.taskId}). Pasando a nivel de reintento...")
                    onProgress(-1f, Config.STATUS_DOWNLOADING, Config.STATUS_RETRYING)
                    delay(500)
                }
                level++
            }
        }

        // Agotados todos los niveles: error con mensaje apto para el usuario,
        // preservando la excepción original del último intento.
        throw failed(lastError, lastLine)
    }

    // ------------------------------------------------------------------------

    private fun failed(e: Throwable, lastLine: String): Exception {
        val message = YtdlpErrorResolver.resolveUserFacingError(e, lastLine.ifEmpty { e.message ?: "" })
        return Exception(Config.STATUS_FAILED_PREFIX + message)
    }

    private fun lowerContainsDownload(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("[download] destination") || lower.contains("[download]")
    }

    private fun destroyQuietly(processId: String) {
        try {
            YoutubeDL.getInstance().destroyProcessById(processId)
        } catch (_: Exception) {
        }
    }

    private suspend fun throwIfCancelled() {
        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
            throw CancellationException("Descarga cancelada/pausada")
        }
    }
}
