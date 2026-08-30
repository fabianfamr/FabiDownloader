package com.fabian.downloader.services

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.managers.BatteryOptimizerManager
import com.fabian.downloader.network.ConnectionService
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.PathUtils
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import java.io.File
import java.util.concurrent.ConcurrentHashMap

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

    typealias LiveProgress = DownloadProgressTracker.LiveProgress

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeCalls = ConcurrentHashMap<Long, Call>()
    private val startDownloadMutex = Mutex()

    private val progressTracker = DownloadProgressTracker(serviceScope, storageService, notificationService)
    val liveProgressFlow: StateFlow<Map<Long, DownloadProgressTracker.LiveProgress>> = progressTracker.liveProgressFlow

    private val downloadExecutor = DownloadExecutor(
        application = application,
        storageService = storageService,
        connectionService = connectionService,
        notificationService = notificationService,
        progressTracker = progressTracker,
        activeCalls = activeCalls,
        onTriggerQueue = { triggerQueue() }
    )

    private val queueManager = DownloadQueueManager(
        application = application,
        storageService = storageService,
        progressTracker = progressTracker,
        downloadExecutor = downloadExecutor,
        serviceScope = serviceScope,
        activeJobs = activeJobs,
        activeCalls = activeCalls
    )

    init {
        BatteryOptimizerManager.getInstance(application)
        queueManager.startQueueProcessor()
        registerSettingsListener()
    }

    fun triggerQueue() = queueManager.triggerQueue()
    fun registerActiveCall(id: Long, call: Call) { activeCalls[id] = call }
    fun unregisterActiveCall(id: Long) { activeCalls.remove(id) }
    fun hasActiveDownloads(): Boolean = queueManager.processingIds.isNotEmpty()
    fun getActiveDownloadsCount(): Int = queueManager.processingIds.size

    fun startDownload(
        rawUrl: String,
        quality: String,
        format: String,
        passedTitle: String? = null,
        passedThumbnailUrl: String? = null,
        existingId: Long? = null,
        isForced: Boolean = false
    ) {
        val url = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(rawUrl)
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
                        Toast.makeText(application, application.getString(R.string.downloads_toast_wifi_only), Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val batteryManager = BatteryOptimizerManager.getInstance(application)
                if (!isForced && AppSettings.batteryOptimizationEnabled && batteryManager.isBatteryLowAndNotCharging()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(application, application.getString(R.string.downloads_toast_battery_low), Toast.LENGTH_SHORT).show()
                    }
                }

                var provisionalTitle = ""
                startDownloadMutex.withLock {
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
                                val file = PathUtils.getDownloadFile(application, completed.title, completed.id, completed.format)
                                if (file.exists()) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(application, application.getString(R.string.downloads_toast_already_downloaded), Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }
                            }
                        }

                        provisionalTitle = passedTitle?.takeIf { it.isNotEmpty() && it != Config.TITLE_PROCESSING_LINK && it != Config.TITLE_ANALYZING_SHARED }
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
                            queueManager.forcedDownloadIds.add(newId)
                        }
                    }
                }

                if (existingId == null) {
                    triggerQueue()
                    val capturedProvisionalTitle = provisionalTitle
                    val safeNewId = newId
                    serviceScope.launch {
                        var resolvedTitleBg: String? = null
                        var resolvedThumbnailBg: String? = passedThumbnailUrl

                        if (passedTitle == null || passedTitle == Config.TITLE_PROCESSING_LINK || passedTitle == Config.TITLE_ANALYZING_SHARED) {
                            try {
                                resolvedTitleBg = withTimeoutOrNull(4000) { extractionService.extractTitle(url) }
                                val extractedThumb = withTimeoutOrNull(4000) { extractionService.extractThumbnail(url) }
                                if (extractedThumb != null) resolvedThumbnailBg = extractedThumb
                            } catch (e: Exception) {
                                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Background title resolution failed: ${e.message}")
                            }
                        }

                        val localThumb = PathUtils.saveThumbnail(application, resolvedThumbnailBg, safeNewId)
                        val finalTitle = if (!resolvedTitleBg.isNullOrEmpty() && resolvedTitleBg != Config.TITLE_PROCESSING_LINK && resolvedTitleBg != Config.TITLE_ANALYZING_SHARED) {
                            resolvedTitleBg
                        } else {
                            capturedProvisionalTitle
                        }

                        if (finalTitle != capturedProvisionalTitle || localThumb != passedThumbnailUrl) {
                            val isAlreadyDownloading = queueManager.processingIds.contains(safeNewId) || activeJobs.containsKey(safeNewId)
                            val titleToSave = if (isAlreadyDownloading) capturedProvisionalTitle else finalTitle
                            storageService.updateDownloadInfoWithThumbnail(safeNewId, titleToSave, Config.STATUS_QUEUED, localThumb)
                        }
                    }
                } else {
                    if (isForced) {
                        queueManager.forcedDownloadIds.add(existingId)
                    }
                    storageService.updatePausedState(existingId, false)
                    val existingRecord = storageService.getDownloadById(existingId)
                    if (existingRecord != null) {
                        var cleanTitle = existingRecord.title
                        while (cleanTitle.startsWith(Config.STATUS_FAILED_PREFIX)) {
                            cleanTitle = cleanTitle.substringAfter(Config.STATUS_FAILED_PREFIX)
                        }
                        val localThumb = PathUtils.saveThumbnail(application, existingRecord.thumbnailUrl, existingId)
                        val cleanProgress = if (existingRecord.progress < 0) 0 else existingRecord.progress
                        storageService.updateDownloadInfoWithThumbnail(existingId, cleanTitle, Config.STATUS_QUEUED, localThumb ?: existingRecord.thumbnailUrl)
                        storageService.updateDownloadProgressAndSizeAndSpeed(existingId, cleanProgress, Config.STATUS_QUEUED, Config.STATUS_WAITING)
                    }
                    triggerQueue()
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error in startDownload", e)
            }
        }
    }

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

    fun pauseDownload(id: Long) {
        serviceScope.launch {
            pauseDownloadInternal(id)
        }
    }

    private suspend fun pauseDownloadInternal(id: Long) {
        progressTracker.removeProgress(id)
        queueManager.forcedDownloadIds.remove(id)
        storageService.updatePausedState(id, true)

        try {
            val currentRecord = storageService.getDownloadById(id)
            if (currentRecord != null) {
                val currentProgress = if (currentRecord.progress < 0) 0 else currentRecord.progress
                val currentSize = if (currentRecord.size == Config.STATUS_QUEUED || currentRecord.size == Config.STATUS_CONNECTING) Config.STATUS_ZERO_MB else currentRecord.size
                storageService.updateDownloadProgressAndSizeAndSpeed(id, currentProgress, currentSize, Config.STATUS_WAITING)
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error actualizando pausa", e)
        }

        activeCalls[id]?.cancel()
        val job = activeJobs[id]
        job?.cancel()
        activeCalls.remove(id)
        activeJobs.remove(id)
        queueManager.processingIds.remove(id)

        if (AppSettings.notificationsEnabled) {
            val currentRecord = storageService.getDownloadById(id)
            if (currentRecord != null) {
                notificationService.showDownloadPaused(id = id.toInt(), title = currentRecord.title, thumbnailUrl = currentRecord.thumbnailUrl)
            } else {
                notificationService.cancelProgressNotification(id.toInt())
            }
        } else {
            notificationService.cancelProgressNotification(id.toInt())
        }

        try {
            YoutubeDL.getInstance().destroyProcessById(id.toString())
        } catch (_: Exception) {}

        if (job != null) {
            try { job.join() } catch (_: Exception) {}
        }

        if (queueManager.processingIds.isEmpty()) {
            DownloadForegroundService.stop(application)
        }
        triggerQueue()

        withContext(Dispatchers.Main) {
            Toast.makeText(application, application.getString(R.string.downloads_toast_paused), Toast.LENGTH_SHORT).show()
        }
    }

    fun requeueDownload(id: Long) {
        serviceScope.launch {
            progressTracker.removeProgress(id)
            queueManager.forcedDownloadIds.remove(id)
            storageService.updatePausedState(id, false)

            try {
                val currentRecord = storageService.getDownloadById(id)
                if (currentRecord != null) {
                    var cleanTitle = currentRecord.title
                    while (cleanTitle.startsWith(Config.STATUS_FAILED_PREFIX)) {
                        cleanTitle = cleanTitle.substringAfter(Config.STATUS_FAILED_PREFIX)
                    }
                    val currentProgress = if (currentRecord.progress < 0) 0 else currentRecord.progress
                    storageService.updateDownloadInfo(id, cleanTitle, Config.STATUS_QUEUED)
                    storageService.updateDownloadProgressAndSizeAndSpeed(id, currentProgress, Config.STATUS_QUEUED, Config.STATUS_WAITING)
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error en requeueDownload", e)
            }

            try {
                YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (_: Exception) {}
            activeCalls[id]?.cancel()
            val job = activeJobs[id]
            job?.cancel()
            activeCalls.remove(id)
            activeJobs.remove(id)
            queueManager.processingIds.remove(id)

            notificationService.cancelProgressNotification(id.toInt())
            triggerQueue()
        }
    }

    fun pauseAllActiveDownloads() {
        serviceScope.launch {
            val activeIds = activeJobs.keys.toList()
            coroutineScope {
                activeIds.map { id ->
                    launch { pauseDownloadInternal(id) }
                }
            }
        }
    }

    fun throttleActiveDownloads() {
        serviceScope.launch {
            val activeIds = activeJobs.keys.toList()
            if (activeIds.size > 1) {
                activeIds.drop(1).forEach { id -> requeueDownload(id) }
            }
            triggerQueue()
        }
    }

    fun deleteDownload(id: Long) {
        serviceScope.launch {
            progressTracker.removeProgress(id)
            queueManager.forcedDownloadIds.remove(id)
            activeCalls[id]?.cancel()
            val job = activeJobs[id]
            job?.cancel()
            activeCalls.remove(id)
            activeJobs.remove(id)
            queueManager.processingIds.remove(id)

            try {
                YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (_: Exception) {}

            val record = storageService.getDownloadById(id)
            if (record != null) {
                val file = PathUtils.getDownloadFile(application, record.title, record.id, record.format)
                if (file.exists()) file.delete()
            }
            if (AppSettings.cleanTempOnCancel) {
                downloadExecutor.cleanTempFiles(id, record?.title, record?.format ?: "MP4")
            }

            val thumbnailsDir = File(application.filesDir, "thumbnails")
            val thumbFile = File(thumbnailsDir, "thumb_$id.jpg")
            if (thumbFile.exists()) thumbFile.delete()

            storageService.deleteDownload(id)
            notificationService.cancelNotification(id.toInt())

            if (queueManager.processingIds.isEmpty()) {
                DownloadForegroundService.stop(application)
            }
            triggerQueue()
        }
    }

    fun deleteDownloadHistory(id: Long) = deleteDownload(id)

    fun clearCompletedDownloads() {
        serviceScope.launch {
            val completed = storageService.getAllDownloadsDirect().filter { it.isCompleted }
            val thumbnailsDir = File(application.filesDir, "thumbnails")
            completed.forEach { record ->
                val thumbFile = File(thumbnailsDir, "thumb_${record.id}.jpg")
                if (thumbFile.exists()) thumbFile.delete()
            }
            storageService.deleteCompletedDownloads()
        }
    }

    private fun registerSettingsListener() {
        AppSettings.addListener { key ->
            Log.i(Config.TAG_DOWNLOAD_MANAGER, "Configuración cambiada: $key")
            when (key) {
                "maxConcurrentDownloads" -> triggerQueue()
                "batteryOptimizationEnabled", "selectedBatteryLowThreshold", "selectedBatteryLowAction" -> {
                    BatteryOptimizerManager.getInstance(application).evaluateBatteryStatus()
                }
            }
        }
    }

    fun onAppClosed() {
        Log.i(Config.TAG_DOWNLOAD_MANAGER, "onAppClosed llamado. Limpiando descargas activas.")
        val jobsToCancel = activeJobs.values.toList()
        val idsToReset = queueManager.processingIds.toList()

        jobsToCancel.forEach { job ->
            try { job.cancel() } catch (_: Exception) {}
        }
        activeJobs.clear()

        activeCalls.forEach { (_, call) ->
            try { call.cancel() } catch (_: Exception) {}
        }
        activeCalls.clear()

        idsToReset.forEach { id ->
            try {
                YoutubeDL.getInstance().destroyProcessById(id.toString())
                notificationService.cancelProgressNotification(id.toInt())
            } catch (_: Exception) {}
        }
        com.fabian.downloader.services.sites.BaseSiteService.cancelAllExtractions()

        try {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(3000) { jobsToCancel.forEach { it.join() } }
                idsToReset.forEach { id ->
                    try {
                        storageService.updatePausedState(id, true)
                        storageService.updateDownloadProgressAndSizeAndSpeed(id, 0, Config.STATUS_WAITING, Config.STATUS_WAITING)
                    } catch (_: Exception) {}
                }
                queueManager.processingIds.clear()
                progressTracker.clearAll()
                storageService.flushPendingWrites()
            }
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error flushing state on app closed", e)
        }
    }

    private fun isCellularNetwork(): Boolean {
        return try {
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            false
        }
    }
}
