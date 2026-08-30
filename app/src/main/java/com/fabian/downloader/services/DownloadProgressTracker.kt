package com.fabian.downloader.services

import com.fabian.downloader.configs.Config
import com.fabian.downloader.ui.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class DownloadProgressTracker(
    private val scope: CoroutineScope,
    private val storageService: StorageService,
    private val notificationService: NotificationService
) {
    data class LiveProgress(
        val progress: Int = 0,
        val sizeText: String = "",
        val speedText: String = ""
    )

    private val _liveProgressFlow = MutableStateFlow<Map<Long, LiveProgress>>(emptyMap())
    val liveProgressFlow: StateFlow<Map<Long, LiveProgress>> = _liveProgressFlow.asStateFlow()

    private val activeProgresses = ConcurrentHashMap<Long, Int>()

    fun getProgress(id: Long): Int = activeProgresses[id] ?: 0

    fun setProgress(id: Long, progress: Int) {
        activeProgresses[id] = progress
    }

    fun removeProgress(id: Long) {
        activeProgresses.remove(id)
        _liveProgressFlow.update { it - id }
    }

    fun clearAll() {
        activeProgresses.clear()
        _liveProgressFlow.value = emptyMap()
    }

    fun updateProgress(
        id: Long,
        videoTitle: String,
        progress: Float,
        sizeText: String,
        speedText: String,
        lastDbPersistTime: Long,
        lastNotificationUpdate: Long,
        onDbPersistDone: (Long) -> Unit,
        onNotificationDone: (Long) -> Unit,
        onEarlyStartTrigger: () -> Unit
    ) {
        val currentProgressInt = progress.toInt()
        val oldProgress = activeProgresses[id] ?: 0
        activeProgresses[id] = currentProgressInt

        val earlyThreshold = AppSettings.earlyStartThreshold
        if (earlyThreshold in 90..99 && oldProgress < earlyThreshold && currentProgressInt >= earlyThreshold) {
            onEarlyStartTrigger()
        }

        val cappedProgress = if (progress >= 100f) 99 else progress.toInt()
        val displaySpeed = if (progress >= 100f) Config.STATUS_FINALIZING else speedText
        val displaySize = sizeText

        // 1. Memoria RAM instantánea (StateFlow)
        _liveProgressFlow.update { currentMap ->
            currentMap + (id to LiveProgress(cappedProgress, displaySize, displaySpeed))
        }

        val currentTime = System.currentTimeMillis()

        // 2. Persistir en SQLite cada 5 segundos
        if (currentTime - lastDbPersistTime > 5000 || progress >= 100f) {
            onDbPersistDone(currentTime)
            scope.launch {
                val currentRecord = storageService.getDownloadById(id)
                if (currentRecord != null && !currentRecord.isPaused && !currentRecord.isCompleted) {
                    val finalSize = if (displaySize == Config.STATUS_DOWNLOADING) {
                        currentRecord.size.takeIf { it != Config.STATUS_ZERO_MB && it.isNotEmpty() } ?: Config.STATUS_DOWNLOADING
                    } else {
                        displaySize
                    }
                    storageService.updateDownloadProgressAndSizeAndSpeed(id, cappedProgress, finalSize, displaySpeed)
                }
            }
        }

        // 3. Notificaciones del sistema
        if (AppSettings.notificationsEnabled && (currentTime - lastNotificationUpdate > 1000 || progress >= 100f)) {
            onNotificationDone(currentTime)
            scope.launch {
                val currentRecord = storageService.getDownloadById(id)
                if (currentRecord != null && !currentRecord.isPaused && !currentRecord.isCompleted) {
                    val notifSize = if (displaySize == Config.STATUS_DOWNLOADING) {
                        currentRecord.size.takeIf { it != Config.STATUS_ZERO_MB && it.isNotEmpty() } ?: Config.STATUS_DOWNLOADING
                    } else {
                        displaySize
                    }
                    notificationService.showDownloadProgress(
                        id = id.toInt(),
                        title = videoTitle,
                        progress = cappedProgress,
                        thumbnailUrl = currentRecord.thumbnailUrl,
                        speed = displaySpeed,
                        size = notifSize
                    )
                }
            }
        }
    }
}
