package com.fabian.downloader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.services.DownloadManagerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DownloadsViewModel(private val database: AppDatabase) : ViewModel() {
    val downloads: Flow<List<DownloadRecord>> = database.downloadDao().getAllDownloads()
        .map { list ->
            withContext(Dispatchers.IO) {
                list.filter { record ->
                    if (!record.isCompleted) {
                        true
                    } else {
                        val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(
                            com.fabian.downloader.MyApplication.getInstance(),
                            record.title,
                            record.id,
                            record.format
                        )
                        val exists = file.exists()
                        if (!exists) {
                            // Clean up orphan record from db asynchronously
                            viewModelScope.launch(Dispatchers.IO) {
                                database.downloadDao().deleteDownload(record.id)
                            }
                            false
                        } else {
                            true
                        }
                    }
                }
            }
        }

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
}
