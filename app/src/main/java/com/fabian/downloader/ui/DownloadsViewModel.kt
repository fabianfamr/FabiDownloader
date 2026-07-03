package com.fabian.downloader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.services.DownloadManagerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DownloadsViewModel(private val database: AppDatabase) : ViewModel() {
    val downloads: Flow<List<DownloadRecord>> = database.downloadDao().getAllDownloads()

    fun pauseDownload(id: Long) {
        DownloadManagerService.instance?.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            val record = database.downloadDao().getDownloadById(id)
            if (record != null) {
                DownloadManagerService.instance?.startDownload(
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

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            val dm = DownloadManagerService.instance
            if (dm != null) {
                dm.deleteDownload(id)
            } else {
                database.downloadDao().deleteDownload(id)
            }
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            DownloadManagerService.instance?.clearCompletedDownloads()
        }
    }
}
