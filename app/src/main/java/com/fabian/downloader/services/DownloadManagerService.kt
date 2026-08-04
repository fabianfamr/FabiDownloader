package com.fabian.downloader.services

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.network.ConnectionService
import com.fabian.downloader.ui.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import android.content.Context
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.BatteryOptimizerManager

class DownloadManagerService private constructor(
    private val application: Application,
    private val storageService: StorageService,
    private val extractionService: ExtractionService,
    private val connectionService: ConnectionService,
    private val notificationService: NotificationService
) {
    companion object {
        @Volatile
        var instance: DownloadManagerService? = null
            private set

        fun getInstance(
            application: Application,
            storageService: StorageService,
            extractionService: ExtractionService,
            connectionService: ConnectionService,
            notificationService: NotificationService
        ): DownloadManagerService {
            return instance ?: synchronized(this) {
                instance ?: DownloadManagerService(
                    application,
                    storageService,
                    extractionService,
                    connectionService,
                    notificationService
                ).also { instance = it }
            }
        }

        fun getInstance(context: Context): DownloadManagerService {
            val app = context.applicationContext as Application
            return instance ?: synchronized(this) {
                instance ?: DownloadManagerService(
                    app,
                    StorageService.getInstance(app),
                    ExtractionService(),
                    ConnectionService(),
                    NotificationService(app)
                ).also { instance = it }
            }
        }
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val client = com.fabian.downloader.network.NetworkClient.okHttpClient
    
    private val activeJobs = java.util.concurrent.ConcurrentHashMap<Long, kotlinx.coroutines.Job>()
    private val activeCalls = java.util.concurrent.ConcurrentHashMap<Long, okhttp3.Call>()
    private val processingIds = java.util.concurrent.ConcurrentSkipListSet<Long>()
    private val forcedDownloadIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
    private val activeProgresses = java.util.concurrent.ConcurrentHashMap<Long, Int>()
    private var isQueueProcessorRunning = false
    private val queueTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    fun triggerQueue() {
        queueTrigger.tryEmit(Unit)
    }

    fun registerActiveCall(id: Long, call: okhttp3.Call) {
        activeCalls[id] = call
    }

    fun unregisterActiveCall(id: Long) {
        activeCalls.remove(id)
    }

    fun hasActiveDownloads(): Boolean {
        return processingIds.isNotEmpty()
    }

    init {
        // Inicializar el optimizador de batería para que empiece a monitorear desde el inicio del servicio
        BatteryOptimizerManager.getInstance(application)
        startQueueProcessor()
        registerSettingsListener()
    }

    private fun startQueueProcessor() {
        if (isQueueProcessorRunning) return
        isQueueProcessorRunning = true
        serviceScope.launch {
            try {
                // Trigger immediately on start
                triggerQueue()
                
                // On-demand only: wait for trigger signals (no polling timer)
                queueTrigger.collect {
                    try {
                        val activeInDb = storageService.getActiveDownloadsDirect()
                        val nextToProcess = activeInDb.filter {
                            !it.isPaused &&
                            it.speed != "FAILED" &&
                            !it.title.startsWith(Config.STATUS_FAILED_PREFIX) &&
                            !processingIds.contains(it.id)
                        }
                        
                        val forcedToProcess = nextToProcess.filter { it.id in forcedDownloadIds }
                        val normalToProcess = nextToProcess.filter { it.id !in forcedDownloadIds }

                        // Iniciar de inmediato descargas forzadas ignorando los límites
                        if (forcedToProcess.isNotEmpty()) {
                            DownloadForegroundService.start(application)
                            forcedToProcess.forEach { record ->
                                processingIds.add(record.id)
                                serviceScope.launch {
                                    try {
                                        runDownloadDirect(record.id)
                                    } finally {
                                        processingIds.remove(record.id)
                                        activeProgresses.remove(record.id)
                                        forcedDownloadIds.remove(record.id)
                                        if (processingIds.isEmpty()) {
                                            DownloadForegroundService.stop(application)
                                            System.gc()
                                        }
                                        triggerQueue()
                                    }
                                }
                            }
                        }

                        var maxParallel = AppSettings.maxConcurrentDownloads
                        val batteryManager = BatteryOptimizerManager.getInstance(application)
                        if (AppSettings.batteryOptimizationEnabled && batteryManager.isBatteryLowAndNotCharging()) {
                            if (AppSettings.batteryLowAction == "Optimizar recursos" || AppSettings.batteryLowAction == "Limitar concurrencia") {
                                maxParallel = 1
                            }
                        }
                        
                        val threshold = AppSettings.earlyStartThreshold
                        val almostFinishedCount = if (threshold in 90..99) {
                            processingIds.count { id -> (activeProgresses[id] ?: 0) in threshold..99 }
                        } else {
                            0
                        }
                        
                        val activeNormalCount = processingIds.count { it !in forcedDownloadIds }
                        val slotsAvailable = maxParallel - (activeNormalCount - almostFinishedCount)
                        if (slotsAvailable > 0 && normalToProcess.isNotEmpty()) {
                            // Iniciar servicio en segundo plano para que no se muera la descarga
                            DownloadForegroundService.start(application)
                            
                            normalToProcess.take(slotsAvailable).forEach { record ->
                                processingIds.add(record.id)
                                
                                serviceScope.launch {
                                    try {
                                        runDownloadDirect(record.id)
                                    } finally {
                                        processingIds.remove(record.id)
                                        activeProgresses.remove(record.id)
                                        forcedDownloadIds.remove(record.id)
                                        if (processingIds.isEmpty()) {
                                            // Detener servicio en segundo plano cuando no queden descargas activas
                                            DownloadForegroundService.stop(application)
                                            System.gc()
                                        }
                                        triggerQueue() // Trigger again to let other queued items start instantly
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error in queue loop", e)
                    }
                }
            } finally {
                isQueueProcessorRunning = false
            }
        }
    }

    fun startDownload(rawUrl: String, quality: String, format: String, passedTitle: String? = null, passedThumbnailUrl: String? = null, existingId: Long? = null, isForced: Boolean = false) {
        // Estación 1: Recepción y limpieza del enlace
        val url = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(rawUrl)
        // Estación 2: Inyección de configuración de usuario
        val taskSpec = com.fabian.downloader.pipeline.DownloadAssemblyLine.station2_assembleUserSettings(rawUrl, url, quality, format)
        
        serviceScope.launch {
            var newId: Long = existingId ?: 0L
            try {
                if (!connectionService.checkConnection()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(application, application.getString(R.string.downloads_toast_no_connection), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                if (AppSettings.dataSaverEnabled && isCellularNetwork()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(application, "Modo 'Solo Wi-Fi' activo: Conéctate a Wi-Fi para descargar", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val batteryManager = BatteryOptimizerManager.getInstance(application)
                if (!isForced && AppSettings.batteryOptimizationEnabled && batteryManager.isBatteryLowAndNotCharging()) {
                    withContext(Dispatchers.Main) {
                        val message = if (AppSettings.batteryLowAction == "Optimizar recursos") {
                            "Descarga iniciada en modo optimizado de recursos (batería baja)"
                        } else {
                            "Descarga iniciada con prioridad baja (concurrencia limitada por batería)"
                        }
                        Toast.makeText(application, message, Toast.LENGTH_LONG).show()
                    }
                }

                if (existingId == null) {
                    val existing = storageService.getDownloadsByUrl(url)
                    val inProgress = existing.find { !it.isCompleted && !it.isPaused && it.speed != "FAILED" && !it.title.startsWith(Config.STATUS_FAILED_PREFIX) }
                    if (inProgress != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(application, application.getString(R.string.downloads_toast_already_in_progress), Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    if (!AppSettings.allowDuplicateDownloads) {
                        val completed = existing.find { it.isCompleted }
                        if (completed != null) {
                            val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(application, completed.title, completed.id, completed.format)
                            if (file.exists()) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(application, application.getString(R.string.downloads_toast_already_downloaded), Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                        }
                    }

                    var resolvedTitle = passedTitle
                    var resolvedThumbnail = passedThumbnailUrl

                    // FAST PATH: Insert the download IMMEDIATELY with a provisional title
                    // so the user sees it in the queue right away. Title/thumbnail are
                    // resolved in the BACKGROUND while the download may already start.
                    val provisionalTitle = passedTitle?.takeIf { it.isNotEmpty() && it != Config.TITLE_PROCESSING_LINK && it != Config.TITLE_ANALYZING_SHARED }
                        ?: generateProvisionalTitle(url)

                    val record = DownloadRecord(
                        title = provisionalTitle,
                        url = url,
                        isCompleted = false,
                        format = format,
                        quality = quality,
                        progress = 0,
                        size = Config.STATUS_QUEUED,
                        thumbnailUrl = passedThumbnailUrl,
                        speed = Config.STATUS_WAITING
                    )
                    newId = storageService.insertDownload(record)
                    if (isForced) {
                        forcedDownloadIds.add(newId)
                    }

                    // Trigger queue IMMEDIATELY so download can start while we resolve title
                    triggerQueue()

                    // Background title/thumbnail resolution and caching (non-blocking, short timeout)
                    serviceScope.launch {
                        var resolvedTitleBg: String? = null
                        var resolvedThumbnailBg: String? = passedThumbnailUrl

                        if (passedTitle == null || passedTitle == Config.TITLE_PROCESSING_LINK || passedTitle == Config.TITLE_ANALYZING_SHARED) {
                            try {
                                resolvedTitleBg = withTimeoutOrNull(4000) { extractionService.extractTitle(url) }
                                val extractedThumb = withTimeoutOrNull(4000) { extractionService.extractThumbnail(url) }
                                if (extractedThumb != null) resolvedThumbnailBg = extractedThumb
                            } catch (e: Exception) {
                                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Background title resolution failed for $url: ${e.message}")
                            }
                        }

                        val localThumb = com.fabian.downloader.utils.PathUtils.saveThumbnail(application, resolvedThumbnailBg, newId)
                        val finalTitle = if (!resolvedTitleBg.isNullOrEmpty() && resolvedTitleBg != Config.TITLE_PROCESSING_LINK && resolvedTitleBg != Config.TITLE_ANALYZING_SHARED) {
                            resolvedTitleBg
                        } else {
                            provisionalTitle
                        }

                        if (finalTitle != provisionalTitle || localThumb != passedThumbnailUrl) {
                            storageService.updateDownloadInfoWithThumbnail(newId, finalTitle, Config.STATUS_QUEUED, localThumb)
                        }
                    }
                } else {
                    if (isForced) {
                        forcedDownloadIds.add(existingId)
                    }
                    storageService.updatePausedState(existingId, false)
                    val existingRecord = storageService.getDownloadById(existingId)
                    if (existingRecord != null) {
                        var cleanTitle = existingRecord.title
                        while (cleanTitle.startsWith(Config.STATUS_FAILED_PREFIX)) {
                            cleanTitle = cleanTitle.substringAfter(Config.STATUS_FAILED_PREFIX)
                        }
                        
                        serviceScope.launch {
                            val localThumb = com.fabian.downloader.utils.PathUtils.saveThumbnail(application, existingRecord.thumbnailUrl, existingId)
                            storageService.updateDownloadInfoWithThumbnail(existingId, cleanTitle, Config.STATUS_QUEUED, localThumb ?: existingRecord.thumbnailUrl)
                        }
                        storageService.updateDownloadProgressAndSizeAndSpeed(existingId, existingRecord.progress, Config.STATUS_QUEUED, Config.STATUS_WAITING)
                    }
                    triggerQueue()
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error in startDownload", e)
            }
        }
    }

    /**
     * Generates a quick provisional title from the URL without any network call.
     * Used to insert the download record immediately so the user sees it in the queue.
     */
    private fun generateProvisionalTitle(url: String): String {
        val lastSegment = url.substringAfterLast("/").substringBefore("?").trim()
        if (lastSegment.isNotEmpty() && lastSegment.contains(".")) {
            return lastSegment.substringBeforeLast(".").take(60)
        }
        val domainName = url.substringAfter("://").substringBefore("/").removePrefix("www.").substringBefore(".")
        return if (domainName.isNotEmpty()) {
            domainName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        } else {
            application.getString(R.string.downloads_default_title)
        }
    }

    private suspend fun runDownloadDirect(id: Long) {
        // Pre-fetch the record so videoTitle is available in the catch block
        val preRecord = storageService.getDownloadById(id)
        var videoTitle = preRecord?.title ?: application.getString(R.string.downloads_default_title)
        var passedThumbnailUrl: String? = preRecord?.thumbnailUrl
        val job = serviceScope.launch {
            val oldPriority = try {
                android.os.Process.getThreadPriority(android.os.Process.myTid())
            } catch (e: Exception) {
                0
            }
            try {
                try {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                } catch (e: Exception) {
                    Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo establecer la prioridad de fondo para el hilo de descarga", e)
                }
                val record = storageService.getDownloadById(id) ?: return@launch
                if (record.isPaused || record.isCompleted) return@launch
                
                videoTitle = com.fabian.downloader.utils.YtdlpParser.cleanTitleOfSuffixes(record.title)
                val url = record.url
                val quality = record.quality
                val format = record.format
                passedThumbnailUrl = record.thumbnailUrl
                
                // Comprobación de conexión a internet real antes de empezar
                if (!connectionService.checkConnection()) {
                    throw Exception(application.getString(R.string.downloads_toast_no_connection))
                }

                storageService.updateDownloadProgressAndSizeAndSpeed(id, record.progress, Config.STATUS_QUEUED, Config.STATUS_WAITING)

                val service = com.fabian.downloader.services.sites.SiteServiceProvider.getServiceForUrl(url)
                var lastProgressUpdate = System.currentTimeMillis()

                // Estación 2: Inyección de Configuración
                val initialSpec = com.fabian.downloader.pipeline.DownloadAssemblyLine.station2_assembleUserSettings(
                    rawUrl = url,
                    cleanUrl = url,
                    requestedQuality = quality,
                    requestedFormat = format
                )
                // Estación 3: Asignación de Almacenamiento y Destino
                val specWithDest = com.fabian.downloader.pipeline.DownloadAssemblyLine.station3_assignDestination(
                    context = application,
                    spec = initialSpec,
                    recordId = id,
                    title = videoTitle,
                    thumbnailUrl = passedThumbnailUrl
                )

                val destFolder = specWithDest.outputDirectory ?: com.fabian.downloader.utils.PathUtils.getDownloadFolder(application, format)
                val fileNameWithoutExt = "${sanitizeFileName(videoTitle)}_$id"

                // Comprobar espacio antes de iniciar la descarga
                checkStorageSpace(destFolder, id)

                var lastSpaceCheck = 0L
                service.download(url, quality, format, destFolder, fileNameWithoutExt, processId = id.toString()) { progress, sizeText, speedText ->
                    val currentTime = System.currentTimeMillis()
                    // Comprobar espacio de forma ultra eficiente cada 5 segundos para priorizar rendimiento y velocidad
                    if (currentTime - lastSpaceCheck > 5000) {
                        lastSpaceCheck = currentTime
                        checkStorageSpace(destFolder, id)
                    }

                    val currentProgressInt = progress.toInt()
                    val oldProgress = activeProgresses[id] ?: 0
                    activeProgresses[id] = currentProgressInt
                    val earlyThreshold = AppSettings.earlyStartThreshold
                    if (earlyThreshold in 90..99 && oldProgress < earlyThreshold && currentProgressInt >= earlyThreshold) {
                        Log.i(Config.TAG_DOWNLOAD_MANAGER, "Descarga $id alcanzó el umbral de inicio temprano ($currentProgressInt% >= $earlyThreshold%). Disparando cola.")
                        triggerQueue()
                    }
                    // Throttling de actualizaciones de progreso a 2000ms para evitar lags de pantalla y saturación de base de datos
                    if (currentTime - lastProgressUpdate > 2000 || progress >= 100f) {
                        lastProgressUpdate = currentTime
                        kotlinx.coroutines.runBlocking {
                            val currentRecord = storageService.getDownloadById(id)
                            if (currentRecord != null && !currentRecord.isPaused && !currentRecord.isCompleted) {
                                val displaySize = if (sizeText == Config.STATUS_DOWNLOADING) {
                                    currentRecord.size.takeIf { it != Config.STATUS_ZERO_MB && it.isNotEmpty() } ?: Config.STATUS_DOWNLOADING
                                } else {
                                    sizeText
                                }
                                
                                // Limitamos el progreso en notificaciones a 99% durante la descarga activa
                                // para que la notificación de completada llegue solo cuando termine el postprocesado
                                val cappedProgress = if (progress >= 100f) 99 else progress.toInt()
                                val displaySpeed = if (progress >= 100f) Config.STATUS_FINALIZING else speedText
                                
                                storageService.updateDownloadProgressAndSizeAndSpeed(id, cappedProgress, displaySize, displaySpeed)
                                
                                if (AppSettings.notificationsEnabled) {
                                    notificationService.showDownloadProgress(
                                        id = id.toInt(), 
                                        title = videoTitle, 
                                        progress = cappedProgress, 
                                        thumbnailUrl = currentRecord.thumbnailUrl,
                                        speed = displaySpeed,
                                        size = displaySize
                                    )
                                }
                            }
                        }
                    }
                }

                if (AppSettings.keepHistory) {
                    storageService.updateDownloadProgressAndSizeAndSpeed(id, 100, Config.STATUS_COMPLETED, Config.STATUS_COMPLETED)
                    storageService.markAsCompleted(id)
                } else {
                    storageService.deleteDownload(id)
                }

                val actualFile = destFolder.listFiles { _, name ->
                    name.startsWith("${fileNameWithoutExt}.") &&
                    Config.VALID_EXTENSIONS.any { ext -> name.endsWith(".$ext", ignoreCase = true) }
                }?.firstOrNull()

                if (actualFile != null) {
                    val ext = actualFile.extension.uppercase()
                    storageService.updateDownloadFormat(id, ext)
                    
                    // Estación 5: Control de Calidad y Escaneo en MediaStore (Entrega Final)
                    com.fabian.downloader.pipeline.DownloadAssemblyLine.station5_verifyAndDeliver(application, actualFile)
                }
                
                // Mostrar notificación de éxito REAL solo cuando ya se tiene el archivo y terminó el post-procesado
                if (AppSettings.notificationsEnabled) {
                    notificationService.showDownloadSuccess(
                        id = id.toInt(), 
                        title = videoTitle, 
                        thumbnailUrl = passedThumbnailUrl
                    )
                }
                
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error downloading id $id", e)
                com.fabian.downloader.managers.ErrorLogManager.logError(application, Config.TAG_DOWNLOAD_MANAGER, "Error downloading id $id (Title: $videoTitle)", e)
                val isCurrentlyPaused = storageService.getDownloadById(id)?.isPaused ?: false
                if (isCurrentlyPaused) {
                    return@launch
                }
                val rawMsg = e.message ?: application.getString(R.string.downloads_error_generic)
                val normalizedMsg = rawMsg.replace("\u2019", "'").replace("\u2018", "'")
                val errorMsg = if (normalizedMsg.contains(Config.BOT_DETECTION_PATTERN, ignoreCase = true) || normalizedMsg.contains(Config.BOT_DETECTION_LOGIN, ignoreCase = true)) {
                    application.getString(R.string.downloads_error_requires_login)
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
                activeJobs.remove(id)
                activeCalls.remove(id)
                activeProgresses.remove(id)
                try {
                    com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_MANAGER, "Failed to destroy process in finally", e)
                }
                try {
                    notificationService.cancelProgressNotification(id.toInt())
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al limpiar la notificación de progreso", e)
                }
                try {
                    android.os.Process.setThreadPriority(oldPriority)
                } catch (e: Exception) {
                    // Ignorar
                }
                // Programar tarea en segundo plano con WorkManager para liberar caché, archivos temporales y memoria RAM
                com.fabian.downloader.workers.CacheCleanupWorker.scheduleCleanup(application)
            }
        }
        
        activeJobs[id] = job
        job.join()
    }

    fun pauseDownload(id: Long) {
        serviceScope.launch {
            forcedDownloadIds.remove(id)
            storageService.updatePausedState(id, true)
            
            // Forzar actualización de progreso y velocidad en la base de datos para evitar estados residuales de carga
            try {
                val currentRecord = storageService.getDownloadById(id)
                if (currentRecord != null) {
                    val currentProgress = if (currentRecord.progress < 0) 0 else currentRecord.progress
                    val currentSize = if (currentRecord.size == Config.STATUS_QUEUED || currentRecord.size == Config.STATUS_CONNECTING) Config.STATUS_ZERO_MB else currentRecord.size
                    storageService.updateDownloadProgressAndSizeAndSpeed(
                        id,
                        currentProgress,
                        currentSize,
                        Config.STATUS_WAITING
                    )
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al actualizar estado durante pausa", e)
            }

            activeCalls[id]?.cancel()
            val job = activeJobs[id]
            job?.cancel()
            activeCalls.remove(id)
            activeJobs.remove(id)
            
            // Mostrar la notificación de pausa si están activadas las notificaciones
            if (AppSettings.notificationsEnabled) {
                val currentRecord = storageService.getDownloadById(id)
                if (currentRecord != null) {
                    notificationService.showDownloadPaused(
                        id = id.toInt(),
                        title = currentRecord.title,
                        thumbnailUrl = currentRecord.thumbnailUrl
                    )
                } else {
                    notificationService.cancelProgressNotification(id.toInt())
                }
            } else {
                notificationService.cancelProgressNotification(id.toInt())
            }
            
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Failed to destroy process", e)
            }
            
            // Wait for the job to actually complete cancellation before proceeding
            if (job != null) {
                try {
                    job.join()
                } catch (e: Exception) {
                    Log.w(Config.TAG_DOWNLOAD_MANAGER, "Job join interrupted during pause", e)
                }
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(application, application.getString(R.string.downloads_toast_paused), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun requeueDownload(id: Long) {
        serviceScope.launch {
            forcedDownloadIds.remove(id)
            storageService.updatePausedState(id, false)
            
            try {
                val currentRecord = storageService.getDownloadById(id)
                if (currentRecord != null) {
                    val currentProgress = if (currentRecord.progress < 0) 0 else currentRecord.progress
                    storageService.updateDownloadProgressAndSizeAndSpeed(
                        id,
                        currentProgress,
                        Config.STATUS_QUEUED,
                        Config.STATUS_WAITING
                    )
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al actualizar estado durante reencolado", e)
            }

            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Error al destruir proceso $id al reencolar", e)
            }
            activeCalls[id]?.cancel()
            val job = activeJobs[id]
            job?.cancel()
            activeCalls.remove(id)
            activeJobs.remove(id)
            processingIds.remove(id)
            activeProgresses.remove(id)
            
            notificationService.cancelProgressNotification(id.toInt())
            
            triggerQueue()
        }
    }

    fun pauseAllActiveDownloads() {
        serviceScope.launch {
            val activeIds = activeJobs.keys.toList()
            activeIds.forEach { id ->
                pauseDownload(id)
            }
        }
    }

    fun throttleActiveDownloads() {
        serviceScope.launch {
            val activeIds = activeJobs.keys.toList()
            if (activeIds.size > 1) {
                activeIds.drop(1).forEach { id ->
                    requeueDownload(id)
                }
            }
            triggerQueue()
        }
    }

    fun deleteDownload(id: Long) {
        serviceScope.launch {
            activeCalls[id]?.cancel()
            activeJobs[id]?.cancel()
            activeCalls.remove(id)
            activeJobs.remove(id)
            
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Failed to destroy process", e)
            }
            
            val record = storageService.getDownloadById(id)
            if (record != null) {
                val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(application, record.title, record.id, record.format)
                if (file.exists()) {
                    file.delete()
                }
            }
            if (AppSettings.cleanTempOnCancel) {
                cleanTempFiles(id, record?.title, record?.format ?: "MP4")
            }
            
            val thumbnailsDir = java.io.File(application.filesDir, "thumbnails")
            val thumbFile = java.io.File(thumbnailsDir, "thumb_$id.jpg")
            if (thumbFile.exists()) thumbFile.delete()
            
            storageService.deleteDownload(id)
            notificationService.cancelNotification(id.toInt())
        }
    }
    
    fun deleteDownloadHistory(id: Long) {
        serviceScope.launch {
            activeCalls[id]?.cancel()
            activeJobs[id]?.cancel()
            activeCalls.remove(id)
            activeJobs.remove(id)
            
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Failed to destroy process", e)
            }
            
            val thumbnailsDir = java.io.File(application.filesDir, "thumbnails")
            val thumbFile = java.io.File(thumbnailsDir, "thumb_$id.jpg")
            if (thumbFile.exists()) thumbFile.delete()
            
            storageService.deleteDownload(id)
            notificationService.cancelNotification(id.toInt())
        }
    }
    
    fun clearCompletedDownloads() {
        serviceScope.launch {
            val completed = storageService.getAllDownloadsDirect().filter { it.isCompleted }
            val thumbnailsDir = java.io.File(application.filesDir, "thumbnails")
            completed.forEach { record ->
                val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(application, record.title, record.id, record.format)
                if (file.exists()) {
                    file.delete()
                }
                val thumbFile = java.io.File(thumbnailsDir, "thumb_${record.id}.jpg")
                if (thumbFile.exists()) thumbFile.delete()
            }
            storageService.deleteCompletedDownloads()
        }
    }

    private fun sanitizeFileName(title: String): String {
        var sanitized = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        // Remove control characters and leading/trailing dots/spaces (Windows doesn't like them)
        sanitized = sanitized.replace(Regex("[\\x00-\\x1f]"), "").trim().trim('.')
        if (sanitized.isEmpty()) {
            sanitized = "download"
        }
        // Avoid Windows-reserved names (in case of cloud sync to Windows)
        val nameWithoutExt = sanitized.substringBeforeLast('.')
        if (Config.RESERVED_FILENAMES.contains(nameWithoutExt.uppercase())) {
            sanitized = "_" + sanitized
        }
        // Limit total length to avoid filesystem issues
        if (sanitized.length > Config.MAX_FILENAME_LENGTH) {
            val ext = sanitized.substringAfterLast('.', "")
            val namePart = sanitized.substringBeforeLast('.')
            sanitized = if (ext.isNotEmpty()) {
                namePart.take(Config.MAX_FILENAME_LENGTH - ext.length - 1) + "." + ext
            } else {
                namePart.take(Config.MAX_FILENAME_LENGTH)
            }
        }
        return sanitized
    }

    private fun registerSettingsListener() {
        AppSettings.addListener { key ->
            Log.i(Config.TAG_DOWNLOAD_MANAGER, "Configuración cambiada detectada: $key")
            when (key) {
                "maxConcurrentDownloads" -> {
                    handleMaxConcurrentDownloadsChanged(AppSettings.maxConcurrentDownloads)
                }
                "maxSpeed", "concurrentFragments", "embedSubtitles", "customArguments", "cookies", "customUserAgent", "sponsorBlockEnabled", "embedThumbnail", "embedMetadata", "bypassGeo", "autoRetry" -> {
                    handleDownloadConfigChanged()
                }
                "batteryOptimizationEnabled", "selectedBatteryLowThreshold", "selectedBatteryLowAction" -> {
                    BatteryOptimizerManager.getInstance(application).evaluateBatteryStatus()
                }
            }
        }
    }

    private fun checkStorageSpace(destFolder: File, id: Long) {
        val minimumRequiredBytes = AppSettings.storageMarginBytes
        if (minimumRequiredBytes <= 0L) return // Comprobación desactivada por el usuario

        try {
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

            val stat = android.os.StatFs(targetDir.absolutePath)
            val availableBytes = stat.availableBytes
            if (availableBytes < minimumRequiredBytes) {
                if (id > 0) {
                    try {
                        com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
                    } catch (e: Exception) {
                        Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo destruir el proceso $id durante la parada por espacio", e)
                    }
                    try {
                        activeCalls[id]?.cancel()
                    } catch (e: Exception) {
                        Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo cancelar la llamada $id durante la parada por espacio", e)
                    }
                }
                val marginText = AppSettings.selectedStorageMargin
                val availableMb = availableBytes / (1024 * 1024)
                throw Exception("Almacenamiento casi lleno (quedan $availableMb MB, requiere min $marginText libres). Libera espacio para descargar.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("Almacenamiento casi lleno") == true) {
                throw e
            }
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error comprobando espacio en ${destFolder.absolutePath}", e)
        }
    }

    private fun handleMaxConcurrentDownloadsChanged(newLimit: Int) {
        serviceScope.launch {
            val runningIds = processingIds.filter { activeJobs.containsKey(it) }
            if (runningIds.size > newLimit) {
                val excessCount = runningIds.size - newLimit
                val sortedIds = runningIds.sortedDescending()
                val toRequeue = sortedIds.take(excessCount)
                toRequeue.forEach { id ->
                    Log.i(Config.TAG_DOWNLOAD_MANAGER, "Reencolando descarga $id (esperando en cola) por reducción de descargas simultáneas a $newLimit")
                    requeueDownload(id)
                }
            } else {
                triggerQueue()
            }
        }
    }

    private fun handleDownloadConfigChanged() {
        // Optimización: No reiniciamos las descargas activas al cambiar de pantalla o configuraciones.
        // Se aplicará la nueva configuración automáticamente a las descargas futuras.
        Log.i(Config.TAG_DOWNLOAD_MANAGER, "Configuración cambiada. Se aplicará a las nuevas descargas.")
    }

    fun onAppClosed() {
        Log.i(Config.TAG_DOWNLOAD_MANAGER, "onAppClosed llamado. Limpiando descargas activas para evitar procesos huérfanos nativos.")
        
        // 1. Cancelar todos los trabajos activos
        activeJobs.forEach { (id, job) ->
            try {
                job.cancel()
            } catch (e: Exception) {
                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Error al cancelar trabajo $id", e)
            }
        }
        activeJobs.clear()

        // 2. Cancelar todas las llamadas de red activas
        activeCalls.forEach { (id, call) ->
            try {
                call.cancel()
            } catch (e: Exception) {
                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Error al cancelar llamada $id", e)
            }
        }
        activeCalls.clear()

        // 3. Destruir todos los procesos nativos de yt-dlp para no laguear el teléfono
        processingIds.forEach { id ->
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Error al destruir el proceso yt-dlp $id", e)
            }
        }
        
        // 4. Actualizar estados en BD para evitar quedar congelados en 'Descargando' al reiniciar la app
        val activeIdsList = processingIds.toList()
        processingIds.clear()
        activeProgresses.clear()
        
        // Cancelar de inmediato las notificaciones de progreso activas para que no queden huérfanas
        activeIdsList.forEach { id ->
            try {
                notificationService.cancelProgressNotification(id.toInt())
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error cancelando notificación de progreso para $id al cerrar", e)
            }
        }
        
        serviceScope.launch {
            activeIdsList.forEach { id ->
                try {
                    storageService.updatePausedState(id, true)
                    storageService.updateDownloadProgressAndSizeAndSpeed(id, 0, "Pausado", "PAUSED")
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al restablecer el estado de la descarga $id", e)
                }
            }
        }
    }

    private fun isCellularNetwork(): Boolean {
        return try {
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (e: Exception) {
            false
        }
    }

    private fun cleanTempFiles(id: Long, title: String?, format: String = "MP4") {
        try {
            val destFolder = com.fabian.downloader.utils.PathUtils.getDownloadFolder(application, format)
            if (destFolder.exists() && destFolder.isDirectory) {
                destFolder.listFiles()?.forEach { file ->
                    val name = file.name
                    if ((name.endsWith(".part") || name.endsWith(".ytdl") || name.endsWith(".temp")) &&
                        (name.contains(id.toString()) || (title != null && name.contains(title.take(15))))) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error cleaning temp files for $id", e)
        }
    }
}
