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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    private val client = com.fabian.downloader.network.NetworkClient.okHttpClient
    
    private val activeJobs = java.util.concurrent.ConcurrentHashMap<Long, kotlinx.coroutines.Job>()
    private val activeCalls = java.util.concurrent.ConcurrentHashMap<Long, okhttp3.Call>()
    private val processingIds = java.util.concurrent.ConcurrentSkipListSet<Long>()
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

    init {
        startQueueProcessor()
    }

    private fun startQueueProcessor() {
        if (isQueueProcessorRunning) return
        isQueueProcessorRunning = true
        serviceScope.launch {
            // Trigger immediately on start
            triggerQueue()
            
            // Collect signals from either direct triggers or a 3-second fallback timer
            merge(
                queueTrigger,
                flow {
                    while (true) {
                        emit(Unit)
                        kotlinx.coroutines.delay(3000)
                    }
                }
            ).collect {
                try {
                    val activeInDb = storageService.getActiveDownloadsDirect()
                    val nextToProcess = activeInDb.filter {
                        !it.isPaused &&
                        !it.title.startsWith("Fallo: ") &&
                        !processingIds.contains(it.id)
                    }
                    val maxParallel = AppSettings.maxConcurrentDownloads
                    val slotsAvailable = maxParallel - processingIds.size
                    if (slotsAvailable > 0 && nextToProcess.isNotEmpty()) {
                        nextToProcess.take(slotsAvailable).forEach { record ->
                            processingIds.add(record.id)
                            
                            serviceScope.launch {
                                try {
                                    runDownloadDirect(record.id)
                                } finally {
                                    processingIds.remove(record.id)
                                    triggerQueue() // Trigger again to let other queued items start instantly
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DownloadManager", "Error in queue loop", e)
                }
            }
        }
    }

    fun startDownload(rawUrl: String, quality: String, format: String, passedTitle: String? = null, passedThumbnailUrl: String? = null, existingId: Long? = null) {
        val url = rawUrl.trim().let { text ->
            val regex = Regex("""https?://[^\s]+""")
            regex.find(text)?.value ?: text
        }
        serviceScope.launch {
            var newId: Long = existingId ?: 0L
            try {
                if (!connectionService.checkConnection()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(application, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                if (existingId == null) {
                    val existing = storageService.getDownloadsByUrl(url)
                    val inProgress = existing.find { !it.isCompleted && !it.isPaused && !it.title.startsWith("Fallo: ") }
                    if (inProgress != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(application, "Esta descarga ya está en progreso o en cola", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    
                    val completed = existing.find { it.isCompleted }
                    if (completed != null) {
                        val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(application, completed.title, completed.id, completed.format)
                        if (file.exists()) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(application, "Este archivo ya ha sido descargado", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                    }

                    val tempTitle = passedTitle ?: "Procesando enlace..."
                    val record = DownloadRecord(
                        title = tempTitle,
                        url = url,
                        isCompleted = false,
                        format = format,
                        quality = quality,
                        progress = 0,
                        size = "En cola...",
                        thumbnailUrl = passedThumbnailUrl,
                        speed = "Esperando..."
                    )
                    newId = storageService.insertDownload(record)
                } else {
                    storageService.updatePausedState(existingId, false)
                    val existingRecord = storageService.getDownloadById(existingId)
                    if (existingRecord != null) {
                        var cleanTitle = existingRecord.title
                        while (cleanTitle.startsWith("Fallo: ")) {
                            cleanTitle = cleanTitle.substringAfter("Fallo: ")
                        }
                        storageService.updateDownloadInfoWithThumbnail(existingId, cleanTitle, "En cola...", existingRecord.thumbnailUrl)
                        storageService.updateDownloadProgressAndSizeAndSpeed(existingId, 0, "En cola...", "Esperando...")
                    }
                }
                triggerQueue()
            } catch (e: Exception) {
                Log.e("DownloadManager", "Error in startDownload", e)
            }
        }
    }

    private suspend fun runDownloadDirect(id: Long) {
        var videoTitle = "Descarga"
        var passedThumbnailUrl: String? = null
        val job = serviceScope.launch {
            try {
                val record = storageService.getDownloadById(id) ?: return@launch
                if (record.isPaused || record.isCompleted) return@launch
                
                videoTitle = record.title
                val url = record.url
                val quality = record.quality
                val format = record.format
                passedThumbnailUrl = record.thumbnailUrl

                if (!connectionService.checkConnection()) {
                    storageService.updateDownloadInfo(id, "Fallo: " + videoTitle.substringAfter("Fallo: "), "Sin conexión")
                    return@launch
                }

                // Si se inició como una descarga rápida y tiene un título provisional, resolvemos la info real en segundo plano
                val isProvisional = videoTitle == "Procesando enlace..." || videoTitle == "Analizando enlace compartido..."
                var resolvedTitle: String? = null
                var resolvedThumb: String? = null
                
                if (isProvisional) {
                    val domainName = url.substringAfter("://").substringBefore("/").removePrefix("www.").substringBefore(".")
                    val quickTitle = if (domainName.isNotEmpty()) {
                        domainName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } + "_$id"
                    } else {
                        "Descarga_$id"
                    }
                    videoTitle = quickTitle
                    storageService.updateDownloadInfoWithThumbnail(id, videoTitle, "Conectando...", null)
                }

                val titleJob = if (isProvisional) {
                    serviceScope.launch {
                        try {
                            resolvedTitle = kotlinx.coroutines.withTimeoutOrNull(10000) {
                                extractionService.extractTitle(url, id)
                            }
                            resolvedThumb = kotlinx.coroutines.withTimeoutOrNull(10000) {
                                extractionService.extractThumbnail(url, id)
                            }
                            if (resolvedTitle != null) {
                                val oldTitle = videoTitle
                                videoTitle = resolvedTitle!!
                                passedThumbnailUrl = resolvedThumb
                                storageService.updateDownloadInfoWithThumbnail(id, videoTitle, "Descargando...", passedThumbnailUrl)
                                Log.d("DownloadManager", "Resolved provisional title in background: $oldTitle -> $videoTitle")
                            }
                        } catch (e: Exception) {
                            Log.e("DownloadManager", "Error resolving title in background", e)
                        }
                    }
                } else null

                storageService.updateDownloadProgressAndSizeAndSpeed(id, 0, "Conectando...", "Conectando...")

                val service = com.fabian.downloader.services.sites.SiteServiceProvider.getServiceForUrl(url)
                var lastProgressUpdate = System.currentTimeMillis()

                val fileNameWithoutExt = if (isProvisional) "download_temp_$id" else "${sanitizeFileName(videoTitle)}_$id"
                val destFolder = com.fabian.downloader.utils.PathUtils.getDownloadFolder(application, format)

                service.download(url, quality, format, destFolder, fileNameWithoutExt, processId = id.toString()) { progress, sizeText, speedText ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastProgressUpdate > 1000 || progress >= 100f) {
                        lastProgressUpdate = currentTime
                        serviceScope.launch {
                            val currentRecord = storageService.getDownloadById(id)
                            if (currentRecord != null && !currentRecord.isPaused) {
                                val displaySize = if (sizeText == "Descargando...") {
                                    currentRecord.size.takeIf { it != "0 MB" && it.isNotEmpty() } ?: "Descargando..."
                                } else {
                                    sizeText
                                }
                                storageService.updateDownloadProgressAndSizeAndSpeed(id, progress.toInt(), displaySize, speedText)
                                
                                if (AppSettings.notificationsEnabled) {
                                    notificationService.showDownloadProgress(
                                        id = id.toInt(), 
                                        title = videoTitle, 
                                        progress = progress.toInt(), 
                                        thumbnailUrl = currentRecord.thumbnailUrl,
                                        speed = speedText,
                                        size = displaySize
                                    )
                                }
                            }
                        }
                    }
                }

                // Esperar a que se resuelva el título real en segundo plano para poder renombrar el archivo correctamente al finalizar
                titleJob?.join()

                val finalTitle = videoTitle
                val finalSanitizedTitle = sanitizeFileName(finalTitle)
                val finalFileName = "${finalSanitizedTitle}_$id"

                storageService.updateDownloadProgressAndSizeAndSpeed(id, 100, "Completado", "Completado")
                storageService.markAsCompleted(id)

                val actualFile = destFolder.listFiles { _, name ->
                    name.startsWith("${fileNameWithoutExt}.")
                }?.firstOrNull()

                if (actualFile != null) {
                    val ext = actualFile.extension.uppercase()
                    storageService.updateDownloadFormat(id, ext)
                    
                    var finalFile = actualFile
                    if (fileNameWithoutExt != finalFileName) {
                        val destinationFile = File(destFolder, "${finalFileName}.${actualFile.extension}")
                        if (actualFile.renameTo(destinationFile)) {
                            Log.d("DownloadManager", "Successfully renamed file from $fileNameWithoutExt to $finalFileName")
                            finalFile = destinationFile
                        } else {
                            Log.w("DownloadManager", "Failed to rename file from $fileNameWithoutExt to $finalFileName")
                        }
                    }

                    // Escanear el archivo para que aparezca inmediatamente en el sistema / galería / descargas
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            application,
                            arrayOf(finalFile.absolutePath),
                            null
                        ) { path, uri ->
                            Log.d("DownloadManager", "MediaScanner: Archivo escaneado $path -> Uri: $uri")
                        }
                    } catch (e: Exception) {
                        Log.e("DownloadManager", "Error al escanear archivo con MediaScannerConnection", e)
                    }
                }
                
                if (AppSettings.notificationsEnabled) {
                    notificationService.showDownloadProgress(
                        id = id.toInt(), 
                        title = videoTitle, 
                        progress = 100, 
                        thumbnailUrl = passedThumbnailUrl
                    )
                }
                
            } catch (e: Exception) {
                Log.e("DownloadManager", "Error downloading id $id", e)
                val isCurrentlyPaused = storageService.getDownloadById(id)?.isPaused ?: false
                if (isCurrentlyPaused) {
                    return@launch
                }
                val rawMsg = e.message ?: "Error de descarga"
                val errorMsg = if (rawMsg.contains("Sign in to confirm you’re not a bot", ignoreCase = true) || rawMsg.contains("login", ignoreCase = true)) {
                    "Requiere inicio de sesión. Configura las Cookies en Ajustes."
                } else {
                    rawMsg
                }
                var cleanTitle = videoTitle
                while (cleanTitle.startsWith("Fallo: ")) {
                    cleanTitle = cleanTitle.substringAfter("Fallo: ")
                }
                storageService.updateDownloadInfo(id, "Fallo: $cleanTitle", errorMsg)
                
                if (AppSettings.notificationsEnabled) {
                    notificationService.showDownloadFailed(
                        id = id.toInt(),
                        title = videoTitle,
                        errorMsg = errorMsg,
                        thumbnailUrl = passedThumbnailUrl
                    )
                }
            } finally {
                activeJobs.remove(id)
            }
        }
        
        activeJobs[id] = job
        job.join()
    }

    fun pauseDownload(id: Long) {
        serviceScope.launch {
            storageService.updatePausedState(id, true)
            activeCalls[id]?.cancel()
            activeJobs[id]?.cancel()
            activeCalls.remove(id)
            activeJobs.remove(id)
            
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                Log.e("DownloadManager", "Failed to destroy process", e)
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(application, "Descarga pausada", Toast.LENGTH_SHORT).show()
            }
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
                Log.e("DownloadManager", "Failed to destroy process", e)
            }
            
            val record = storageService.getDownloadById(id)
            if (record != null) {
                val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(application, record.title, record.id, record.format)
                if (file.exists()) {
                    file.delete()
                }
            }
            
            storageService.deleteDownload(id)
            notificationService.cancelNotification(id.toInt())
        }
    }
    
    fun clearCompletedDownloads() {
        serviceScope.launch {
            val completed = storageService.getAllDownloadsDirect().filter { it.isCompleted }
            completed.forEach { record ->
                val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(application, record.title, record.id, record.format)
                if (file.exists()) {
                    file.delete()
                }
            }
            storageService.deleteCompletedDownloads()
        }
    }

    private fun sanitizeFileName(title: String): String {
        return title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
    
    private fun localFileSize(file: File): Double {
        return file.length().toDouble() / (1024 * 1024)
    }

    private fun getDomainName(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val domain = uri.host ?: ""
            if (domain.startsWith("www.")) domain.substring(4) else domain
        } catch (e: Exception) {
            "Internet"
        }
    }
}
