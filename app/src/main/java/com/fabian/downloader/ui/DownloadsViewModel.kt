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
        DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            val record = database.downloadDao().getDownloadById(id)
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

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).deleteDownload(id)
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            DownloadManagerService.getInstance(com.fabian.downloader.MyApplication.getInstance()).clearCompletedDownloads()
        }
    }
}
