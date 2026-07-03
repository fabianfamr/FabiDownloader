package com.fabian.downloader.services

import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageService(private val database: AppDatabase) {
    
    fun getActiveDownloads(): Flow<List<DownloadRecord>> {
        return database.downloadDao().getAllDownloads().map { list -> list.filter { !it.isCompleted } }
    }
    
    fun getCompletedDownloads(): Flow<List<DownloadRecord>> {
        return database.downloadDao().getAllDownloads().map { list -> list.filter { it.isCompleted } }
    }
    
    suspend fun insertDownload(record: DownloadRecord): Long {
        return database.downloadDao().insertDownload(record)
    }
    
    suspend fun updateDownloadProgress(id: Long, progress: Int) {
        database.downloadDao().updateDownloadProgress(id, progress)
    }

    suspend fun updateDownloadInfo(id: Long, title: String, size: String) {
        database.downloadDao().updateDownloadInfo(id, title, size)
    }

    suspend fun updateDownloadInfoWithThumbnail(id: Long, title: String, size: String, thumbnailUrl: String?) {
        database.downloadDao().updateDownloadInfoWithThumbnail(id, title, size, thumbnailUrl)
    }

    suspend fun updateDownloadProgressAndSizeAndSpeed(id: Long, progress: Int, size: String, speed: String) {
        database.downloadDao().updateDownloadProgressSizeAndSpeed(id, progress, size, speed)
    }

    suspend fun updateDownloadProgressAndSize(id: Long, progress: Int, size: String) {
        database.downloadDao().updateDownloadProgressAndSize(id, progress, size)
    }
    
    suspend fun markAsCompleted(id: Long) {
        database.downloadDao().markAsCompleted(id)
    }

    suspend fun updatePausedState(id: Long, isPaused: Boolean) {
        database.downloadDao().updatePausedState(id, isPaused)
    }

    suspend fun updateDownloadFormat(id: Long, format: String) {
        database.downloadDao().updateDownloadFormat(id, format)
    }

    suspend fun getDownloadsByUrl(url: String): List<DownloadRecord> {
        return database.downloadDao().getDownloadsByUrl(url)
    }

    suspend fun getActiveDownloadsDirect(): List<DownloadRecord> {
        return database.downloadDao().getActiveDownloadsDirect()
    }

    suspend fun getDownloadById(id: Long): DownloadRecord? {
        return database.downloadDao().getDownloadById(id)
    }
    
    suspend fun deleteDownload(id: Long) {
        database.downloadDao().deleteDownload(id)
    }

    suspend fun getAllDownloadsDirect(): List<DownloadRecord> {
        return database.downloadDao().getAllDownloadsDirect()
    }
    
    suspend fun deleteCompletedDownloads() {
        database.downloadDao().deleteCompletedDownloads()
    }
}
