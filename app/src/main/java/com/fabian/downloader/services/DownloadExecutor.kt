package com.fabian.downloader.services

import android.app.Application
import android.content.Context
import android.os.Process
import android.os.StatFs
import android.util.Log
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.ErrorLogManager
import com.fabian.downloader.network.ConnectionService
import com.fabian.downloader.pipeline.DownloadAssemblyLine
import com.fabian.downloader.services.sites.SiteServiceProvider
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.MediaMetadataHelper
import com.fabian.downloader.utils.PathUtils
import com.fabian.downloader.utils.YtdlpParser
import com.fabian.downloader.workers.CacheCleanupWorker
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class DownloadExecutor(
    private val application: Application,
    private val storageService: StorageService,
    private val connectionService: ConnectionService,
    private val notificationService: NotificationService,
    private val progressTracker: DownloadProgressTracker,
    private val activeCalls: ConcurrentHashMap<Long, Call>,
    private val onTriggerQueue: () -> Unit
) {
    suspend fun runDownload(id: Long, scope: CoroutineScope) {
        val preRecord = storageService.getDownloadById(id)
        var videoTitle = preRecord?.title ?: application.getString(R.string.downloads_default_title)
        var passedThumbnailUrl: String? = preRecord?.thumbnailUrl

        val oldPriority = try {
            Process.getThreadPriority(Process.myTid())
        } catch (_: Exception) {
            0
        }

        try {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            } catch (e: Exception) {
                Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo establecer la prioridad de fondo para el hilo de descarga", e)
            }

            val record = storageService.getDownloadById(id) ?: return
            if (record.isPaused || record.isCompleted) return

            videoTitle = YtdlpParser.cleanTitleOfSuffixes(record.title)
            val url = record.url
            val quality = record.quality
            val format = record.format
            passedThumbnailUrl = record.thumbnailUrl

            if (!connectionService.checkConnection()) {
                throw Exception(application.getString(R.string.downloads_toast_no_connection))
            }

            storageService.updateDownloadProgressAndSizeAndSpeed(id, record.progress, Config.STATUS_CALCULATING, Config.STATUS_CONNECTING)
            progressTracker.updateProgress(
                id = id,
                videoTitle = videoTitle,
                progress = if (record.progress < 0) 0f else record.progress.toFloat(),
                sizeText = Config.STATUS_CALCULATING,
                speedText = Config.STATUS_CONNECTING,
                lastDbPersistTime = 0L,
                lastNotificationUpdate = 0L,
                onDbPersistDone = {},
                onNotificationDone = {},
                onEarlyStartTrigger = {}
            )

            val service = SiteServiceProvider.getServiceForUrl(url)

            val initialSpec = DownloadAssemblyLine.station2_assembleUserSettings(
                rawUrl = url,
                cleanUrl = url,
                requestedQuality = quality,
                requestedFormat = format
            )

            val specWithDest = DownloadAssemblyLine.station3_assignDestination(
                context = application,
                spec = initialSpec,
                recordId = id,
                title = videoTitle,
                thumbnailUrl = passedThumbnailUrl
            )

            val destFolder = specWithDest.outputDirectory ?: PathUtils.getDownloadFolder(application, format)
            val fileNameWithoutExt = PathUtils.sanitizeFileName(videoTitle).ifEmpty { "download_$id" }

            checkStorageSpace(destFolder, id)

            var lastDbPersistTime = 0L
            var lastNotificationUpdate = 0L
            var lastSpaceCheck = 0L
            val storageSpaceExceeded = AtomicBoolean(false)

            val downloadSuccess = service.download(url, quality, format, destFolder, fileNameWithoutExt, processId = id.toString()) { progress, sizeText, speedText ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSpaceCheck > 5000) {
                    lastSpaceCheck = currentTime
                    if (!hasEnoughStorageSpace(destFolder)) {
                        storageSpaceExceeded.set(true)
                        stopDownloadForStorage(id)
                    }
                }

                progressTracker.updateProgress(
                    id = id,
                    videoTitle = videoTitle,
                    progress = progress,
                    sizeText = sizeText,
                    speedText = speedText,
                    lastDbPersistTime = lastDbPersistTime,
                    lastNotificationUpdate = lastNotificationUpdate,
                    onDbPersistDone = { lastDbPersistTime = it },
                    onNotificationDone = { lastNotificationUpdate = it },
                    onEarlyStartTrigger = { onTriggerQueue() }
                )
            }

            if (storageSpaceExceeded.get()) {
                throw Exception(application.getString(R.string.downloads_error_storage))
            }

            if (!downloadSuccess) {
                throw Exception(application.getString(R.string.downloads_error_generic))
            }

            val downloadingFile = destFolder.listFiles { _, name ->
                (name.startsWith("${fileNameWithoutExt}.") || name == "$fileNameWithoutExt.downloading") && 
                name.contains(".downloading", ignoreCase = true)
            }?.firstOrNull()

            val actualFile = if (downloadingFile != null) {
                val rawNameWithoutDownloading = if (downloadingFile.name.endsWith(".downloading", ignoreCase = true)) {
                    downloadingFile.name.removeSuffix(".downloading")
                } else {
                    downloadingFile.name.replace(".downloading", "")
                }
                val hasValidExt = Config.VALID_EXTENSIONS.any { ext -> 
                    rawNameWithoutDownloading.endsWith(".$ext", ignoreCase = true) 
                }
                val finalName = if (hasValidExt) {
                    rawNameWithoutDownloading
                } else {
                    "$rawNameWithoutDownloading.${format.lowercase()}"
                }
                val targetFile = File(destFolder, finalName)
                if (targetFile.exists() && targetFile.absolutePath != downloadingFile.absolutePath) {
                    targetFile.delete()
                }
                if (downloadingFile.renameTo(targetFile)) {
                    targetFile
                } else {
                    try {
                        downloadingFile.copyTo(targetFile, overwrite = true)
                        downloadingFile.delete()
                        targetFile
                    } catch (_: Exception) {
                        downloadingFile
                    }
                }
            } else {
                destFolder.listFiles { _, name ->
                    name.startsWith("${fileNameWithoutExt}.") &&
                    Config.VALID_EXTENSIONS.any { ext -> name.endsWith(".$ext", ignoreCase = true) } &&
                    !name.contains(".downloading", ignoreCase = true)
                }?.firstOrNull()
            }

            if (actualFile == null) {
                throw Exception(application.getString(R.string.downloads_error_generic))
            }

            val ext = actualFile.extension.uppercase()
            var finalFile = actualFile
            var finalExt = ext
            val requestedExt = format.uppercase().trim()
            val requestedHeight = quality.filter { it.isDigit() }.toIntOrNull()
            val isRequestedAudio = Config.AUDIO_EXTENSIONS.any { it.equals(requestedExt, ignoreCase = true) }

            // Verificación estilo Snaptube: si se descargó en otra calidad o formato
            var needsAutoConversion = false
            var conversionTargetHeight: Int? = null

            if (isRequestedAudio) {
                // Si el usuario solicitó audio (MP3, M4A, etc.) pero el archivo descargado no es del formato solicitado
                // o contiene pista de video (ej. se descargó un contenedor mp4/webm con video):
                val hasVideo = MediaMetadataHelper.hasVideoStream(actualFile)
                if (ext != requestedExt || hasVideo) {
                    needsAutoConversion = true
                }
            } else {
                // El usuario solicitó video
                // 1. Verificación de formato (ej. pidió MP4 y se descargó WEBM o MKV)
                if (requestedExt == "MP4" && ext != "MP4") {
                    needsAutoConversion = true
                }
                // 2. Verificación de calidad/resolución (ej. pidió 720p y se descargó a 1080p o 480p)
                if (requestedHeight != null && requestedHeight > 0) {
                    val resolution = MediaMetadataHelper.getVideoResolution(actualFile)
                    if (resolution != null) {
                        val currentHeight = resolution.second
                        // Si la resolución descargada difiere significativamente (> 16px) de la solicitada
                        if (Math.abs(currentHeight - requestedHeight) > 16) {
                            needsAutoConversion = true
                            conversionTargetHeight = requestedHeight
                        }
                    }
                }
            }

            if (needsAutoConversion) {
                try {
                    Log.i(Config.TAG_DOWNLOAD_MANAGER, "Auto-conversión estilo Snaptube: el archivo descargado ($ext) difiere del solicitado ($requestedExt, $quality). Convirtiendo...")
                    val convStatus = application.getString(R.string.downloads_converting)
                    storageService.updateDownloadProgressAndSizeAndSpeed(id, 98, convStatus, "CONVERTING")

                    val converter = MediaConverterService(application)
                    val targetFolder = PathUtils.getDownloadFolder(application, requestedExt.lowercase())
                    val convResult = converter.convertFile(
                        inputFile = actualFile,
                        outputFolder = targetFolder,
                        fileNameWithoutExt = fileNameWithoutExt,
                        targetExt = requestedExt.lowercase(),
                        targetHeight = conversionTargetHeight,
                        targetBitrate = if (isRequestedAudio) quality else null,
                        processId = "conv_$id"
                    ) { convProgress, _ ->
                        progressTracker.updateProgress(
                            id = id,
                            videoTitle = videoTitle,
                            progress = 95f + (convProgress * 0.04f).coerceIn(0f, 4f),
                            sizeText = convStatus,
                            speedText = "Snaptube Converter",
                            lastDbPersistTime = lastDbPersistTime,
                            lastNotificationUpdate = lastNotificationUpdate,
                            onDbPersistDone = { lastDbPersistTime = it },
                            onNotificationDone = { lastNotificationUpdate = it },
                            onEarlyStartTrigger = { onTriggerQueue() }
                        )
                    }

                    if (convResult.isSuccess) {
                        val convertedFile = convResult.getOrThrow()
                        if (convertedFile.exists() && convertedFile.length() > 0) {
                            if (convertedFile.absolutePath != actualFile.absolutePath) {
                                actualFile.delete()
                            }
                            finalFile = convertedFile
                            finalExt = convertedFile.extension.uppercase()
                        }
                    } else {
                        Log.w(Config.TAG_DOWNLOAD_MANAGER, "Auto-conversión no completada: ${convResult.exceptionOrNull()?.message}. Conservando archivo descargado.")
                    }
                } catch (convEx: Exception) {
                    Log.w(Config.TAG_DOWNLOAD_MANAGER, "Excepción durante auto-conversión estilo Snaptube: ${convEx.message}", convEx)
                }
            }

            storageService.updateDownloadFormat(id, finalExt)

            DownloadAssemblyLine.station5_verifyAndDeliver(application, finalFile)
            cleanTempFiles(id, videoTitle, finalExt)

            if (AppSettings.keepHistory) {
                storageService.updateDownloadProgressAndSizeAndSpeed(id, 100, Config.STATUS_COMPLETED, Config.STATUS_COMPLETED)
                storageService.markAsCompleted(id)
            } else {
                storageService.deleteDownload(id)
            }

            if (AppSettings.notificationsEnabled) {
                notificationService.showDownloadSuccess(
                    id = id.toInt(), 
                    title = videoTitle, 
                    thumbnailUrl = passedThumbnailUrl
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) return
            val currentRecord = storageService.getDownloadById(id)
            if (currentRecord == null || currentRecord.isPaused || !currentCoroutineContext().isActive) {
                return
            }
            ErrorLogManager.logError(application, Config.TAG_DOWNLOAD_MANAGER, "Error downloading id $id (Title: $videoTitle)", e)
            val rawMsg = e.message ?: application.getString(R.string.downloads_error_generic)
            val normalizedMsg = rawMsg.replace("\u2019", "'").replace("\u2018", "'")
            val lowerMsg = normalizedMsg.lowercase()
            val errorMsg = if (normalizedMsg.contains(Config.BOT_DETECTION_PATTERN, ignoreCase = true) || normalizedMsg.contains(Config.BOT_DETECTION_LOGIN, ignoreCase = true)) {
                application.getString(R.string.downloads_error_requires_login)
            } else if (lowerMsg.contains("no space left") || lowerMsg.contains("enospc") || lowerMsg.contains("disk full") || lowerMsg.contains("espacio en disco") || lowerMsg.contains("almacenamiento casi lleno") || lowerMsg.contains("espacio insuficiente")) {
                application.getString(R.string.downloads_error_storage)
            } else {
                rawMsg
            }
            var cleanTitle = videoTitle
            while (cleanTitle.startsWith(Config.STATUS_FAILED_PREFIX)) {
                cleanTitle = cleanTitle.substringAfter(Config.STATUS_FAILED_PREFIX)
            }
            val cleanErrorMsg = errorMsg.removePrefix(Config.STATUS_FAILED_PREFIX)
            storageService.updateDownloadInfo(id, cleanTitle, cleanErrorMsg)
            storageService.updateDownloadProgressAndSizeAndSpeed(id, 0, cleanErrorMsg, "FAILED")

            if (AppSettings.notificationsEnabled) {
                notificationService.showDownloadFailed(
                    id = id.toInt(),
                    title = cleanTitle,
                    errorMsg = cleanErrorMsg,
                    thumbnailUrl = passedThumbnailUrl
                )
            }
        } finally {
            progressTracker.removeProgress(id)
            activeCalls.remove(id)
            try {
                YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Failed to destroy process in finally", e)
            }
            try {
                notificationService.cancelProgressNotification(id.toInt())
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al limpiar la notificación de progreso", e)
            }
            try {
                Process.setThreadPriority(oldPriority)
            } catch (_: Exception) {}
            CacheCleanupWorker.scheduleCleanup(application)
        }
    }

    fun hasEnoughStorageSpace(destFolder: File): Boolean {
        val minimumRequiredBytes = AppSettings.storageMarginBytes
        if (minimumRequiredBytes <= 0L) return true

        return try {
            var targetDir = destFolder
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            while (!targetDir.exists() && targetDir.parentFile != null) {
                targetDir = targetDir.parentFile!!
            }
            if (!targetDir.exists()) {
                targetDir = application.filesDir
            }

            val stat = StatFs(targetDir.absolutePath)
            stat.availableBytes >= minimumRequiredBytes
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error comprobando espacio en ${destFolder.absolutePath}", e)
            true
        }
    }

    fun stopDownloadForStorage(id: Long) {
        if (id <= 0) return
        try {
            YoutubeDL.getInstance().destroyProcessById(id.toString())
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo destruir el proceso $id durante parada por espacio", e)
        }
        try {
            activeCalls[id]?.cancel()
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo cancelar la llamada $id durante parada por espacio", e)
        }
    }

    private fun checkStorageSpace(destFolder: File, id: Long) {
        if (!hasEnoughStorageSpace(destFolder)) {
            stopDownloadForStorage(id)
            throw Exception(application.getString(R.string.downloads_error_storage))
        }
    }

    fun cleanTempFiles(id: Long, title: String?, format: String = "MP4") {
        try {
            val destFolder = PathUtils.getDownloadFolder(application, format)
            if (destFolder.exists() && destFolder.isDirectory) {
                destFolder.listFiles()?.forEach { file ->
                    val name = file.name
                    val cleanTitle = if (title != null) PathUtils.sanitizeFileName(title) else ""
                    if ((name.endsWith(".part") || name.endsWith(".ytdl") || name.endsWith(".temp") || name.endsWith(".tmp") || name.endsWith(".downloading") || name.contains(".downloading")) &&
                        (name.contains(id.toString()) || (cleanTitle.isNotEmpty() && name.startsWith(cleanTitle)))) {
                        file.delete()
                    }
                    if (name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true) || 
                        name.endsWith(".png", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)) {
                        val baseName = name.substringBeforeLast(".")
                        if ((cleanTitle.isNotEmpty() && baseName.equals(cleanTitle, ignoreCase = true)) || 
                            baseName.contains(id.toString()) || name.startsWith("thumb_")) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error cleaning temp files for $id", e)
        }
    }
}
