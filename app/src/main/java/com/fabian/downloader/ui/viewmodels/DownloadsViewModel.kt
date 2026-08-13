package com.fabian.downloader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.services.DownloadManagerService
import com.fabian.downloader.services.StorageService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DownloadsViewModel(private val database: AppDatabase) : ViewModel() {
    private val storageService = StorageService.getInstance(com.fabian.downloader.MyApplication.getInstance())
    val downloads: Flow<List<DownloadRecord>> = storageService.getAllDownloads()

    init {
        // No borramos silenciosamente registros de la BD al iniciar el ViewModel para evitar
        // pérdida de historial si un almacenamiento externo/SD está temporalmente desorganizado.
    }

    fun pauseDownload(id: Long) {
        DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            val record = storageService.getDownloadById(id)
            if (record != null) {
                DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).startDownload(
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
                DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).startDownload(
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
            DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).deleteDownload(id)
        }
    }
    
    fun deleteDownloadHistory(id: Long) {
        viewModelScope.launch {
            DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).deleteDownloadHistory(id)
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).clearCompletedDownloads()
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
