package com.fabian.downloader.services

import android.content.Context
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class ProgressUpdate(
    val progress: Int,
    val size: String,
    val speed: String
)

class StorageService(private val database: AppDatabase) {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val memoryCache = ConcurrentHashMap<Long, DownloadRecord>()
    private val activeProgressUpdates = MutableStateFlow<Map<Long, ProgressUpdate>>(emptyMap())
    private val dirtyIds = ConcurrentHashMap.newKeySet<Long>()
    private val flushMutex = Mutex()
    private var flushJob: Job? = null

    init {
        // Bucle de escritura en segundo plano: guarda de forma agrupada los progresos en SQLite
        // Job cancelable: mientras(isActive) permite terminarlo limpiamente en shutdown.
        flushJob = serviceScope.launch {
            while (isActive) {
                delay(1500L) // Guarda en la BD cada 1.5 segundos los progresos acumulados
                if (dirtyIds.isNotEmpty()) {
                    flushPendingWrites()
                }
            }
        }
    }

    fun shutdownAndFlush() {
        flushJob?.cancel()
        try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                flushPendingWrites()
            }
        } catch (_: Exception) {}
    }

    suspend fun flushPendingWrites() {
        if (dirtyIds.isEmpty()) return
        flushMutex.withLock {
            val idsToFlush = dirtyIds.toList()
            dirtyIds.clear()
            val currentUpdates = activeProgressUpdates.value
            for (id in idsToFlush) {
                val update = currentUpdates[id]
                if (update != null) {
                    try {
                        database.downloadDao().updateDownloadProgressSizeAndSpeed(
                            id,
                            update.progress,
                            update.size,
                            update.speed
                        )
                    } catch (e: Exception) {
                        dirtyIds.add(id)
                    }
                }
            }
        }
    }

    fun getAllDownloads(): Flow<List<DownloadRecord>> {
        return database.downloadDao().getAllDownloads()
            .combine(activeProgressUpdates.sample(250L)) { dbList, updates ->
                dbList.map { record ->
                    val isActiveOrQueued = !record.isCompleted && !record.isPaused && !record.size.startsWith(Config.STATUS_FAILED_PREFIX)
                    val update = updates[record.id]
                    if (update != null && isActiveOrQueued) {
                        record.copy(
                            progress = update.progress,
                            size = update.size,
                            speed = update.speed
                        )
                    } else {
                        record
                    }
                }
            }
    }

    fun getActiveDownloads(): Flow<List<DownloadRecord>> {
        return getAllDownloads().map { list -> list.filter { !it.isCompleted } }
    }

    fun getCompletedDownloads(): Flow<List<DownloadRecord>> {
        return getAllDownloads().map { list -> list.filter { it.isCompleted } }
    }

    suspend fun insertDownload(record: DownloadRecord): Long {
        val newId = database.downloadDao().insertDownload(record)
        val created = record.copy(id = newId)
        memoryCache[newId] = created
        return newId
    }

    suspend fun updateDownloadProgress(id: Long, progress: Int) {
        val existingUpdate = activeProgressUpdates.value[id]
        val newUpdate = ProgressUpdate(
            progress = progress,
            size = existingUpdate?.size ?: Config.STATUS_ZERO_MB,
            speed = existingUpdate?.speed ?: ""
        )
        activeProgressUpdates.update { current -> current + (id to newUpdate) }
        dirtyIds.add(id)
    }

    suspend fun updateDownloadInfo(id: Long, title: String, size: String) {
        database.downloadDao().updateDownloadInfo(id, title, size)
        val cached = memoryCache[id]
        if (cached != null) {
            memoryCache[id] = cached.copy(title = title, size = size)
        }
    }

    suspend fun updateDownloadInfoWithThumbnail(id: Long, title: String, size: String, thumbnailUrl: String?) {
        database.downloadDao().updateDownloadInfoWithThumbnail(id, title, size, thumbnailUrl)
        val cached = memoryCache[id]
        if (cached != null) {
            memoryCache[id] = cached.copy(title = title, size = size, thumbnailUrl = thumbnailUrl)
        }
    }

    suspend fun updateDownloadProgressAndSizeAndSpeed(id: Long, progress: Int, size: String, speed: String) {
        // Ignorar actualizaciones de progreso si la descarga ya se completó en memoria
        val cached = memoryCache[id]
        if (cached?.isCompleted == true) return

        if (speed == "FAILED" || size.startsWith(Config.STATUS_FAILED_PREFIX)) {
            database.downloadDao().updateDownloadProgressSizeAndSpeed(id, progress, size, speed)
            activeProgressUpdates.update { current -> current - id }
            dirtyIds.remove(id)
            if (cached != null) {
                memoryCache[id] = cached.copy(progress = progress, size = size, speed = speed)
            }
            flushPendingWrites()
            return
        }

        // Actualización de alta frecuencia durante la descarga.
        // Se actualiza la memoria de inmediato (60fps en la UI) y se marca como "sucia" para volcado periódico a disco.
        val update = ProgressUpdate(progress, size, speed)
        activeProgressUpdates.update { current -> current + (id to update) }
        dirtyIds.add(id)

        if (cached != null) {
            memoryCache[id] = cached.copy(progress = progress, size = size, speed = speed)
        }
    }

    suspend fun updateDownloadProgressAndSize(id: Long, progress: Int, size: String) {
        val existingSpeed = activeProgressUpdates.value[id]?.speed ?: ""
        updateDownloadProgressAndSizeAndSpeed(id, progress, size, existingSpeed)
    }

    suspend fun markAsCompleted(id: Long) {
        // Proteger con el mismo mutex del flush: si el bucle de 1.5s está escribiendo
        // una actualización vieja (ej. 97%), el orden de escritura podría pisar el 100%.
        flushMutex.withLock {
            activeProgressUpdates.update { current -> current - id }
            dirtyIds.remove(id)

            // Guardar estado final garantizado en la base de datos SQLite.
            // Se escribe 100% siempre (no solo si lastUpdate != null) para no
            // perder la escritura final si el flush ya consumió la actualización.
            database.downloadDao().updateDownloadProgressSizeAndSpeed(id, 100, Config.STATUS_COMPLETED, Config.STATUS_COMPLETED)
            database.downloadDao().markAsCompleted(id)
            memoryCache.remove(id)
        }
    }

    suspend fun updatePausedState(id: Long, isPaused: Boolean) {
        val currentUpdate = activeProgressUpdates.value[id]
        if (currentUpdate != null) {
            database.downloadDao().updateDownloadProgressSizeAndSpeed(id, currentUpdate.progress, currentUpdate.size, currentUpdate.speed)
            activeProgressUpdates.update { current -> current - id }
            dirtyIds.remove(id)
        }
        database.downloadDao().updatePausedState(id, isPaused)
        val cached = memoryCache[id]
        if (cached != null) {
            memoryCache[id] = cached.copy(isPaused = isPaused)
        }
    }

    suspend fun updateDownloadFormat(id: Long, format: String) {
        database.downloadDao().updateDownloadFormat(id, format)
        val cached = memoryCache[id]
        if (cached != null) {
            memoryCache[id] = cached.copy(format = format)
        }
    }

    suspend fun getDownloadsByUrl(url: String): List<DownloadRecord> {
        val dbList = database.downloadDao().getDownloadsByUrl(url)
        val updates = activeProgressUpdates.value
        return dbList.map { record ->
            val update = updates[record.id]
            if (update != null && !record.isCompleted) {
                record.copy(progress = update.progress, size = update.size, speed = update.speed)
            } else record
        }
    }

    suspend fun getActiveDownloadsDirect(): List<DownloadRecord> {
        val dbList = database.downloadDao().getActiveDownloadsDirect()
        val updates = activeProgressUpdates.value
        return dbList.map { record ->
            val update = updates[record.id]
            if (update != null && !record.isCompleted) {
                record.copy(progress = update.progress, size = update.size, speed = update.speed)
            } else record
        }
    }

    suspend fun getDownloadById(id: Long): DownloadRecord? {
        val cached = memoryCache[id]
        val update = activeProgressUpdates.value[id]
        if (cached != null) {
            return if (update != null && !cached.isCompleted) {
                cached.copy(progress = update.progress, size = update.size, speed = update.speed)
            } else {
                cached
            }
        }
        val dbRecord = database.downloadDao().getDownloadById(id) ?: return null
        if (!dbRecord.isCompleted) {
            memoryCache[dbRecord.id] = dbRecord
        }
        return if (update != null && !dbRecord.isCompleted) {
            dbRecord.copy(progress = update.progress, size = update.size, speed = update.speed)
        } else dbRecord
    }

    suspend fun deleteDownload(id: Long) {
        activeProgressUpdates.update { current -> current - id }
        dirtyIds.remove(id)
        memoryCache.remove(id)
        database.downloadDao().deleteDownload(id)
    }

    suspend fun getAllDownloadsDirect(): List<DownloadRecord> {
        val dbList = database.downloadDao().getAllDownloadsDirect()
        val updates = activeProgressUpdates.value
        return dbList.map { record ->
            val update = updates[record.id]
            if (update != null && !record.isCompleted) {
                record.copy(progress = update.progress, size = update.size, speed = update.speed)
            } else record
        }
    }

    suspend fun deleteCompletedDownloads() {
        val updates = activeProgressUpdates.value
        val activeIds = updates.keys
        memoryCache.keys.retainAll(activeIds)
        database.downloadDao().deleteCompletedDownloads()
    }

    companion object {
        @Volatile
        private var instance: StorageService? = null

        fun getInstance(context: Context): StorageService {
            return instance ?: synchronized(this) {
                instance ?: StorageService(AppDatabase.getInstance(context.applicationContext)).also { instance = it }
            }
        }
    }
}
