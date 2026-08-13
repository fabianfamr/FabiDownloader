package com.fabian.downloader.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.SearchHistoryRecord
import com.fabian.downloader.network.ConnectionService
import com.fabian.downloader.services.DownloadManagerService
import com.fabian.downloader.services.ExtractionService
import com.fabian.downloader.services.NotificationService
import com.fabian.downloader.services.StorageService
import kotlinx.coroutines.launch

class MainViewModel(application: Application, private val database: AppDatabase) : AndroidViewModel(application) {

    private val storageService = StorageService.getInstance(application)
    private val extractionService = ExtractionService()
    private val connectionService = ConnectionService()
    private val notificationService = NotificationService(application)

    private val downloadManager = DownloadManagerService.getInstance(application)

    suspend fun extractVideoInfo(url: String): ExtractionService.ExtractedVideo {
        return extractionService.extractVideoInfo(url)
    }

    suspend fun extractTitle(url: String): String {
        return extractionService.extractTitle(url)
    }

    suspend fun extractThumbnail(url: String): String? {
        return extractionService.extractThumbnail(url)
    }

    suspend fun extractFormatSizes(url: String): Map<String, Double> {
        return extractionService.extractFormatSizes(url)
    }

    suspend fun getRealSizeAndUrl(url: String, quality: String, format: String): Pair<String, String> {
        return extractionService.getRealSizeAndUrl(url, quality, format)
    }

    val searchHistory = database.searchHistoryDao().getRecentSearches()

    fun saveSearch(query: String) {
        viewModelScope.launch {
            database.searchHistoryDao().insertSearch(SearchHistoryRecord(query = query))
        }
    }

    suspend fun extractPlaylist(url: String): ExtractionService.ExtractedPlaylist? {
        return extractionService.extractPlaylist(url)
    }

    fun downloadVideo(url: String, quality: String = "720p", format: String = Config.FORMAT_MP4, title: String? = null, thumbnailUrl: String? = null) {
        downloadManager.startDownload(url, quality, format, title, thumbnailUrl)
    }

    fun downloadBatch(items: List<ExtractionService.PlaylistItem>, quality: String = "720p", format: String = Config.FORMAT_MP4) {
        viewModelScope.launch {
            items.forEach { item ->
                downloadManager.startDownload(
                    rawUrl = item.url,
                    quality = quality,
                    format = format,
                    passedTitle = item.title,
                    passedThumbnailUrl = item.thumbnailUrl.ifEmpty { null }
                )
            }
        }
    }
}
