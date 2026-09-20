package com.fabian.downloader.services

import android.app.Application
import android.os.Process
import android.os.StatFs
import android.util.Log
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.ErrorLogManager
import com.fabian.downloader.network.ConnectionService
import com.fabian.downloader.pipeline.DownloadAssemblyLine
import com.fabian.downloader.pipeline.DownloadTaskSpec
import com.fabian.downloader.services.sites.SiteServiceProvider
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.MediaMetadataHelper
import com.fabian.downloader.utils.PathUtils
import com.fabian.downloader.utils.YtdlpParser
import com.fabian.downloader.workers.CacheCleanupWorker
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ejecuta el ciclo completo de una descarga: preparación, descarga, post-procesado,
 * entrega y manejo de errores.
 *
 * Refactorizado de una sola función de 290 líneas a funciones pequeñas:
 *  - [prepareDownloadSpec]
 *  - [executeDownload]
 *  - [postProcessFile]
 *  - [finalizeDownload]
 *  - [handleDownloadFailure]
 *  - [cleanupDownload]
 */
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
        var videoTitle = preRecord?.title
            ?: application.getString(R.string.downloads_default_title)
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
                Log.w(Config.TAG_DOWNLOAD_MANAGER,
                    "No se pudo establecer la prioridad de fondo", e)
            }

            val record = storageService.getDownloadById(id) ?: return
            if (record.isPaused || record.isCompleted) return

            videoTitle = YtdlpParser.cleanTitleOfSuffixes(record.title)
            val spec = prepareDownloadSpec(record, videoTitle, id, passedThumbnailUrl)

            // Pre-flight inspection: ahora SÍ usamos el resultado (antes se ignoraba).
            if (!DownloadAssemblyLine.station4_preflightInspection(spec)) {
                throw StorageException(application.getString(R.string.downloads_error_storage))
            }

            checkStorageSpace(spec.outputDirectory ?: PathUtils.getDownloadFolder(application, record.format), id)

            val rawFile = executeDownload(id, spec, videoTitle, record.progress, scope)
            val finalFile = postProcessFile(id, rawFile, spec, videoTitle, record)

            finalizeDownload(id, videoTitle, finalFile, passedThumbnailUrl)
        } catch (e: Exception) {
            if (e is CancellationException) return
            handleDownloadFailure(id, videoTitle, passedThumbnailUrl, e, scope)
        } finally {
            cleanupDownload(id, videoTitle, oldPriority)
        }
    }

    /**
     * Estación 2+3: ensambla la configuración del usuario y asigna destino.
     */
    private suspend fun prepareDownloadSpec(
        record: com.fabian.downloader.database.DownloadRecord,
        videoTitle: String,
        id: Long,
        passedThumbnailUrl: String?
    ): DownloadTaskSpec {
        if (!connectionService.checkConnection()) {
            throw DownloadException(application.getString(R.string.downloads_toast_no_connection))
        }

        storageService.updateDownloadProgressAndSizeAndSpeed(
            id, record.progress, Config.STATUS_CALCULATING, Config.STATUS_CONNECTING
        )
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

        val initialSpec = DownloadAssemblyLine.station2_assembleUserSettings(
            rawUrl = record.url,
            cleanUrl = record.url,
            requestedQuality = record.quality,
            requestedFormat = record.format
        )

        return DownloadAssemblyLine.station3_assignDestination(
            context = application,
            spec = initialSpec,
            recordId = id,
            title = videoTitle,
            thumbnailUrl = passedThumbnailUrl
        )
    }

    /**
     * Lanza la descarga vía yt-dlp/SiteService y devuelve el archivo resultante.
     */
    private suspend fun executeDownload(
        id: Long,
        spec: DownloadTaskSpec,
        videoTitle: String,
        initialProgress: Int,
        scope: CoroutineScope
    ): File {
        val service = SiteServiceProvider.getServiceForUrl(spec.cleanUrl)
        val destFolder = spec.outputDirectory
            ?: PathUtils.getDownloadFolder(application, spec.format)
        val fileNameWithoutExt = PathUtils.sanitizeFileName(videoTitle).ifEmpty { "download_$id" }

        var lastDbPersistTime = 0L
        var lastNotificationUpdate = 0L
        var lastSpaceCheck = 0L
        val storageSpaceExceeded = AtomicBoolean(false)

        val downloadSuccess = service.download(
            spec.cleanUrl, spec.quality, spec.format, destFolder, fileNameWithoutExt,
            processId = id.toString()
        ) { progress, sizeText, speedText ->
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

        // Excepción tipada: ahora diferenciamos "sin espacio" de "error genérico".
        if (storageSpaceExceeded.get()) {
            throw StorageException(application.getString(R.string.downloads_error_storage))
        }
        if (!downloadSuccess) {
            throw DownloadException(application.getString(R.string.downloads_error_generic))
        }

        return resolveDownloadedFile(destFolder, fileNameWithoutExt, spec.format)
            ?: throw DownloadException(application.getString(R.string.downloads_error_generic))
    }

    /**
     * Localiza el archivo descargado en disco (maneja sufijos `.downloading`,
     * extensiones válidas, renombrado).
     */
    private fun resolveDownloadedFile(
        destFolder: File,
        fileNameWithoutExt: String,
        format: String
    ): File? {
        val downloadingFile = destFolder.listFiles { _, name ->
            (name.startsWith("${fileNameWithoutExt}.") ||
                name == "$fileNameWithoutExt.downloading") &&
                name.contains(".downloading", ignoreCase = true)
        }?.firstOrNull()

        return if (downloadingFile != null) {
            val rawNameWithoutDownloading = if (
                downloadingFile.name.endsWith(".downloading", ignoreCase = true)
            ) {
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
    }

    /**
     * Post-procesado: detección de formato/quality mismatch y auto-conversión.
     */
    private suspend fun postProcessFile(
        id: Long,
        actualFile: File,
        spec: DownloadTaskSpec,
        videoTitle: String,
        record: com.fabian.downloader.database.DownloadRecord
    ): File {
        val ext = actualFile.extension.uppercase()
        var finalFile = actualFile
        var finalExt = ext
        val requestedExt = spec.format.uppercase().trim()
        val requestedHeight = spec.quality.filter { it.isDigit() }.toIntOrNull()
        val isRequestedAudio = Config.AUDIO_EXTENSIONS.any { it.equals(requestedExt, ignoreCase = true) }

        var needsAutoConversion = false
        var conversionTargetHeight: Int? = null

        if (isRequestedAudio) {
            val hasVideo = MediaMetadataHelper.hasVideoStream(actualFile)
            if (ext != requestedExt || hasVideo) {
                needsAutoConversion = true
            }
        } else {
            if (requestedExt == "MP4" && ext != "MP4") {
                needsAutoConversion = true
            }
            if (requestedHeight != null && requestedHeight > 0) {
                val resolution = MediaMetadataHelper.getVideoResolution(actualFile)
                if (resolution != null) {
                    val currentHeight = resolution.second
                    if (Math.abs(currentHeight - requestedHeight) > 16) {
                        needsAutoConversion = true
                        conversionTargetHeight = requestedHeight
                    }
                }
            }
        }

        if (needsAutoConversion) {
            finalFile = runAutoConversion(
                id, actualFile, spec, videoTitle, requestedExt,
                isRequestedAudio, conversionTargetHeight
            )
            finalExt = finalFile.extension.uppercase()
        }

        storageService.updateDownloadFormat(id, finalExt)

        DownloadAssemblyLine.station5_verifyAndDeliver(application, finalFile)
        cleanTempFiles(id, videoTitle, finalExt)

        return finalFile
    }

    private suspend fun runAutoConversion(
        id: Long,
        actualFile: File,
        spec: DownloadTaskSpec,
        videoTitle: String,
        requestedExt: String,
        isRequestedAudio: Boolean,
        conversionTargetHeight: Int?
    ): File {
        var resultFile = actualFile
        try {
            Log.i(Config.TAG_DOWNLOAD_MANAGER,
                "Auto-conversión: el archivo descargado difiere del solicitado " +
                    "($requestedExt, ${spec.quality}). Convirtiendo...")
            val convStatus = application.getString(R.string.downloads_converting)
            storageService.updateDownloadProgressAndSizeAndSpeed(id, 98, convStatus, "CONVERTING")

            var lastDbPersistTime = 0L
            var lastNotificationUpdate = 0L

            val converter = MediaConverterService(application)
            val targetFolder = PathUtils.getDownloadFolder(application, requestedExt.lowercase())
            val convResult = converter.convertFile(
                inputFile = actualFile,
                outputFolder = targetFolder,
                fileNameWithoutExt = PathUtils.sanitizeFileName(videoTitle).ifEmpty { "download_$id" },
                targetExt = requestedExt.lowercase(),
                targetHeight = conversionTargetHeight,
                targetBitrate = if (isRequestedAudio) spec.quality else null,
                processId = "conv_$id"
            ) { convProgress, _ ->
                progressTracker.updateProgress(
                    id = id,
                    videoTitle = videoTitle,
                    progress = 95f + (convProgress * 0.04f).coerceIn(0f, 4f),
                    sizeText = convStatus,
                    speedText = "Converter",
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
                    resultFile = convertedFile
                }
            } else {
                Log.w(Config.TAG_DOWNLOAD_MANAGER,
                    "Auto-conversión no completada: " +
                        "${convResult.exceptionOrNull()?.message}. Conservando original.")
            }
        } catch (convEx: Exception) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER,
                "Excepción durante auto-conversión: ${convEx.message}", convEx)
        }
        return resultFile
    }

    /**
     * Marca la descarga como completada y muestra notificación de éxito.
     */
    private suspend fun finalizeDownload(
        id: Long,
        videoTitle: String,
        finalFile: File,
        passedThumbnailUrl: String?
    ) {
        if (AppSettings.keepHistory) {
            storageService.updateDownloadProgressAndSizeAndSpeed(
                id, 100, Config.STATUS_COMPLETED, Config.STATUS_COMPLETED
            )
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
    }

    /**
     * Manejo centralizado de fallos con mensajes de error normalizados.
     */
    private suspend fun handleDownloadFailure(
        id: Long,
        videoTitle: String,
        passedThumbnailUrl: String?,
        e: Exception,
        scope: CoroutineScope
    ) {
        val currentRecord = storageService.getDownloadById(id)
        // scope.isActive en lugar de currentCoroutineContext().isActive:
        // currentCoroutineContext() es una función suspend y su uso dentro de
        // catch puede comportarse de forma inesperada.
        if (currentRecord == null || currentRecord.isPaused || !scope.isActive) {
            return
        }
        ErrorLogManager.logError(
            application, Config.TAG_DOWNLOAD_MANAGER,
            "Error downloading id $id (Title: $videoTitle)", e
        )

        val rawMsg = e.message ?: application.getString(R.string.downloads_error_generic)
        val normalizedMsg = rawMsg.replace("\u2019", "'").replace("\u2018", "'")
        val lowerMsg = normalizedMsg.lowercase()
        val errorMsg = when {
            normalizedMsg.contains(Config.BOT_DETECTION_PATTERN, ignoreCase = true) ||
                normalizedMsg.contains(Config.BOT_DETECTION_LOGIN, ignoreCase = true) -> {
                application.getString(R.string.downloads_error_requires_login)
            }
            // Excepción tipada:.storage específico
            e is StorageException ||
                lowerMsg.contains("no space left") ||
                lowerMsg.contains("enospc") ||
                lowerMsg.contains("disk full") ||
                lowerMsg.contains("espacio en disco") ||
                lowerMsg.contains("almacenamiento casi lleno") ||
                lowerMsg.contains("espacio insuficiente") -> {
                application.getString(R.string.downloads_error_storage)
            }
            else -> rawMsg
        }

        // removePrefix simple: el while anterior sugería que el prefijo
        // se estaba añadiendo múltiples veces — hay que arreglar el origen,
        // pero al menos aquí no perpetuamos el bug.
        val cleanTitle = videoTitle.removePrefix(Config.STATUS_FAILED_PREFIX).trim()
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
    }

    /**
     * Limpieza final: tracker, calls activas, proceso yt-dlp, notificación,
     * prioridad del hilo y programación de limpieza de caché.
     */
    private fun cleanupDownload(id: Long, videoTitle: String, oldPriority: Int) {
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
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al limpiar la notificación", e)
        }
        try {
            Process.setThreadPriority(oldPriority)
        } catch (_: Exception) {
        }
        CacheCleanupWorker.scheduleCleanup(application)
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
            Log.e(Config.TAG_DOWNLOAD_MANAGER,
                "Error comprobando espacio en ${destFolder.absolutePath}", e)
            true
        }
    }

    fun stopDownloadForStorage(id: Long) {
        if (id <= 0) return
        try {
            YoutubeDL.getInstance().destroyProcessById(id.toString())
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER,
                "No se pudo destruir el proceso $id durante parada por espacio", e)
        }
        try {
            activeCalls[id]?.cancel()
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER,
                "No se pudo cancelar la llamada $id durante parada por espacio", e)
        }
    }

    private fun checkStorageSpace(destFolder: File, id: Long) {
        if (!hasEnoughStorageSpace(destFolder)) {
            stopDownloadForStorage(id)
            throw StorageException(application.getString(R.string.downloads_error_storage))
        }
    }

    fun cleanTempFiles(id: Long, title: String?, format: String = "MP4") {
        try {
            val destFolder = PathUtils.getDownloadFolder(application, format)
            if (destFolder.exists() && destFolder.isDirectory) {
                destFolder.listFiles()?.forEach { file ->
                    val name = file.name
                    val cleanTitle = if (title != null) PathUtils.sanitizeFileName(title) else ""
                    if ((name.endsWith(".part") || name.endsWith(".ytdl") ||
                            name.endsWith(".temp") || name.endsWith(".tmp") ||
                            name.endsWith(".downloading") || name.contains(".downloading")) &&
                        (name.contains(id.toString()) ||
                            (cleanTitle.isNotEmpty() && name.startsWith(cleanTitle)))
                    ) {
                        file.delete()
                    }
                    if (name.endsWith(".jpg", ignoreCase = true) ||
                        name.endsWith(".webp", ignoreCase = true) ||
                        name.endsWith(".png", ignoreCase = true) ||
                        name.endsWith(".jpeg", ignoreCase = true)
                    ) {
                        val baseName = name.substringBeforeLast(".")
                        if ((cleanTitle.isNotEmpty() &&
                                baseName.equals(cleanTitle, ignoreCase = true)) ||
                            baseName.contains(id.toString()) || name.startsWith("thumb_")
                        ) {
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
