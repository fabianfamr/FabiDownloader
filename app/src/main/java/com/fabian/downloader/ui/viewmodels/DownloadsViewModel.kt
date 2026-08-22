package com.fabian.downloader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.services.DownloadManagerService
import com.fabian.downloader.services.StorageService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.sample

class DownloadsViewModel(private val database: AppDatabase) : ViewModel() {
    private val app = com.fabian.downloader.MyApplication.getInstance()
    private val storageService = StorageService.getInstance(app)
    private val downloadManager = DownloadManagerService.getInstance(app)

    val downloads: Flow<List<DownloadRecord>> = combine(
        storageService.getAllDownloads(),
        downloadManager.liveProgressFlow.sample(250L)
    ) { records, liveMap ->
        records.map { record ->
            val live = liveMap[record.id]
            if (live != null && !record.isCompleted && !record.isPaused) {
                record.copy(
                    progress = live.progress,
                    size = if (live.sizeText.isNotEmpty()) live.sizeText else record.size,
                    speed = if (live.speedText.isNotEmpty()) live.speedText else record.speed
                )
            } else {
                record
            }
        }
    }

    init {
        // No borramos silenciosamente registros de la BD al iniciar el ViewModel para evitar
        // pérdida de historial si un almacenamiento externo/SD está temporalmente desorganizado.
    }

    fun pauseDownload(id: Long) {
        downloadManager.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            val record = storageService.getDownloadById(id)
            if (record != null) {
                downloadManager.startDownload(
                    rawUrl = record.url,
                    quality = record.quality,
                    format = record.format,
                    passedTitle = record.title,
                    passedThumbnailUrl = record.thumbnailUrl,
                    existingId = record.id
                )
            }
        }
    }

    fun forceDownload(id: Long) {
        viewModelScope.launch {
            val record = storageService.getDownloadById(id)
            if (record != null) {
                downloadManager.startDownload(
                    rawUrl = record.url,
                    quality = record.quality,
                    format = record.format,
                    passedTitle = record.title,
                    passedThumbnailUrl = record.thumbnailUrl,
                    existingId = record.id,
                    isForced = true
                )
            }
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.deleteDownload(id)
        }
    }
    
    fun deleteDownloadHistory(id: Long) {
        viewModelScope.launch {
            downloadManager.deleteDownloadHistory(id)
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            downloadManager.clearCompletedDownloads()
        }
    }

    fun convertDownloadFormat(
        record: DownloadRecord,
        targetFormat: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val converter = com.fabian.downloader.services.MediaConverterService(com.fabian.downloader.MyApplication.getInstance())
            val result = converter.convertRecord(record, targetFormat, database)
            result.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { onResult(false, it.localizedMessage) }
            )
        }
    }
}
