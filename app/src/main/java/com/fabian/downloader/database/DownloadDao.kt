package com.fabian.downloader.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_records")
    fun getAllDownloads(): Flow<List<DownloadRecord>>

    @Insert
    suspend fun insertDownload(record: DownloadRecord): Long

    @Query("UPDATE download_records SET progress = :progress WHERE id = :id")
    suspend fun updateDownloadProgress(id: Long, progress: Int)

    @Query("UPDATE download_records SET title = :title, size = :size WHERE id = :id")
    suspend fun updateDownloadInfo(id: Long, title: String, size: String)

    @Query("UPDATE download_records SET title = :title, size = :size, thumbnailUrl = :thumbnailUrl WHERE id = :id")
    suspend fun updateDownloadInfoWithThumbnail(id: Long, title: String, size: String, thumbnailUrl: String?)

    @Query("UPDATE download_records SET progress = :progress, size = :size, speed = :speed WHERE id = :id")
    suspend fun updateDownloadProgressSizeAndSpeed(id: Long, progress: Int, size: String, speed: String)

    @Query("UPDATE download_records SET progress = :progress, size = :size WHERE id = :id")
    suspend fun updateDownloadProgressAndSize(id: Long, progress: Int, size: String)

    @Query("UPDATE download_records SET isCompleted = 1 WHERE id = :id")
    suspend fun markAsCompleted(id: Long)

    @Query("UPDATE download_records SET isPaused = :isPaused WHERE id = :id")
    suspend fun updatePausedState(id: Long, isPaused: Boolean)

    @Query("UPDATE download_records SET format = :format WHERE id = :id")
    suspend fun updateDownloadFormat(id: Long, format: String)

    @Query("SELECT * FROM download_records WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadRecord?

    @Query("SELECT * FROM download_records WHERE url = :url")
    suspend fun getDownloadsByUrl(url: String): List<DownloadRecord>

    @Query("DELETE FROM download_records WHERE id = :id")
    suspend fun deleteDownload(id: Long)

    @Query("DELETE FROM download_records WHERE isCompleted = 1")
    suspend fun deleteCompletedDownloads()

    @Query("SELECT * FROM download_records WHERE isCompleted = 0")
    suspend fun getActiveDownloadsDirect(): List<DownloadRecord>

    @Query("SELECT * FROM download_records")
    suspend fun getAllDownloadsDirect(): List<DownloadRecord>
}
