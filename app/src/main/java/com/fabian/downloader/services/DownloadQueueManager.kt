package com.fabian.downloader.services

import android.app.Application
import android.util.Log
import com.fabian.downloader.configs.Config
import com.fabian.downloader.managers.BatteryOptimizerManager
import com.fabian.downloader.ui.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class DownloadQueueManager(
    private val application: Application,
    private val storageService: StorageService,
    private val progressTracker: DownloadProgressTracker,
    private val downloadExecutor: DownloadExecutor,
    private val serviceScope: CoroutineScope,
    private val activeJobs: ConcurrentHashMap<Long, Job>,
    private val activeCalls: ConcurrentHashMap<Long, Call>
) {
    val processingIds = ConcurrentSkipListSet<Long>()
    val forcedDownloadIds = ConcurrentHashMap.newKeySet<Long>()
    private val isQueueProcessorRunning = AtomicBoolean(false)
    private val queueTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    fun triggerQueue() {
        queueTrigger.tryEmit(Unit)
    }

    fun startQueueProcessor() {
        if (!isQueueProcessorRunning.compareAndSet(false, true)) return
        serviceScope.launch {
            try {
                triggerQueue()
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

                        if (forcedToProcess.isNotEmpty()) {
                            DownloadForegroundService.start(application)
                            forcedToProcess.forEach { record ->
                                val id = record.id
                                processingIds.add(id)
                                val job = serviceScope.launch {
                                    try {
                                        downloadExecutor.runDownload(id, this)
                                    } finally {
                                        releaseSlot(id)
                                    }
                                }
                                activeJobs[id] = job
                                job.invokeOnCompletion {
                                    releaseSlot(id)
                                }
                            }
                        }

                        var maxParallel = AppSettings.maxConcurrentDownloads
                        val batteryManager = BatteryOptimizerManager.getInstance(application)
                        if (AppSettings.batteryOptimizationEnabled && batteryManager.isBatteryLowAndNotCharging()) {
                            if (AppSettings.batteryLowAction == Config.BATTERY_ACTION_OPTIMIZE ||
                                AppSettings.batteryLowAction == Config.BATTERY_ACTION_LIMIT) {
                                maxParallel = 1
                            }
                        }

                        val threshold = AppSettings.earlyStartThreshold
                        val almostFinishedCount = if (threshold in 90..99) {
                            processingIds.filter { it !in forcedDownloadIds }.count { id ->
                                progressTracker.getProgress(id) in threshold..99
                            }
                        } else {
                            0
                        }

                        val activeNormalCount = processingIds.count { it !in forcedDownloadIds }
                        val slotsAvailable = maxParallel - (activeNormalCount - almostFinishedCount)
                        if (slotsAvailable > 0 && normalToProcess.isNotEmpty()) {
                            DownloadForegroundService.start(application)
                            normalToProcess.take(slotsAvailable).forEach { record ->
                                val id = record.id
                                processingIds.add(id)
                                val job = serviceScope.launch {
                                    try {
                                        downloadExecutor.runDownload(id, this)
                                    } finally {
                                        releaseSlot(id)
                                    }
                                }
                                activeJobs[id] = job
                                job.invokeOnCompletion {
                                    releaseSlot(id)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error in queue loop", e)
                    }
                }
            } finally {
                isQueueProcessorRunning.set(false)
            }
        }
    }

    fun releaseSlot(id: Long) {
        val removed = processingIds.remove(id)
        forcedDownloadIds.remove(id)
        activeJobs.remove(id)
        activeCalls.remove(id)
        progressTracker.removeProgress(id)
        if (processingIds.isEmpty()) {
            DownloadForegroundService.stop(application)
            System.gc()
        }
        if (removed) {
            triggerQueue()
        }
    }
}
