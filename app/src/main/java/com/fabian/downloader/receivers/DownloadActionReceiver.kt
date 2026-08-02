package com.fabian.downloader.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fabian.downloader.utils.ToastUtils
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.services.DownloadManagerService
import com.fabian.downloader.services.StorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val action = intent.action
        val downloadId = intent.getLongExtra(Config.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) {
            Log.e(Config.TAG_DOWNLOAD_ACTION_RECEIVER, "Received action $action without valid downloadId")
            return
        }
        
        Log.d(Config.TAG_DOWNLOAD_ACTION_RECEIVER, "Action received: $action for ID $downloadId")
        
        // Cancelar las notificaciones de éxito o fallo al interactuar con ellas para no dejarlas huérfanas
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(downloadId.toInt() + 300000)
            notificationManager.cancel(downloadId.toInt() + 500000)
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_ACTION_RECEIVER, "Error cancelling notification on action", e)
        }

        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    Config.ACTION_PAUSE -> {
                        DownloadManagerService.getInstance(context).pauseDownload(downloadId)
                    }
                    Config.ACTION_RESUME -> {
                        resumeDownload(context, downloadId)
                    }
                    Config.ACTION_CANCEL -> {
                        DownloadManagerService.getInstance(context).deleteDownload(downloadId)
                    }
                    Config.ACTION_OPEN -> {
                        openFile(context, downloadId)
                    }
                    Config.ACTION_SHARE -> {
                        shareFile(context, downloadId)
                    }
                    Config.ACTION_RETRY -> {
                        retryDownload(context, downloadId)
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private suspend fun retryDownload(context: Context, downloadId: Long) {
        val storageService = StorageService.getInstance(context)
        val record = storageService.getDownloadById(downloadId) ?: return
        
        withContext(Dispatchers.Main) {
            DownloadManagerService.getInstance(context).startDownload(
                rawUrl = record.url,
                quality = record.quality,
                format = record.format,
                passedTitle = record.title.removePrefix(Config.STATUS_FAILED_PREFIX),
                passedThumbnailUrl = record.thumbnailUrl,
                existingId = record.id
            )
        }
    }

    private suspend fun resumeDownload(context: Context, downloadId: Long) {
        val storageService = StorageService.getInstance(context)
        val record = storageService.getDownloadById(downloadId) ?: return
        
        withContext(Dispatchers.Main) {
            DownloadManagerService.getInstance(context).startDownload(
                rawUrl = record.url,
                quality = record.quality,
                format = record.format,
                passedTitle = record.title,
                passedThumbnailUrl = record.thumbnailUrl,
                existingId = record.id
            )
        }
    }

    private suspend fun openFile(context: Context, downloadId: Long) {
        val storageService = StorageService.getInstance(context)
        val record = storageService.getDownloadById(downloadId) ?: return
        val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(context, record.title, record.id, record.format)
        
        withContext(Dispatchers.Main) {
            if (file.exists()) {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val mimeType = when (record.format.uppercase()) {
                        Config.FORMAT_MP4, Config.FORMAT_WEBM -> Config.MIME_VIDEO
                        Config.FORMAT_JPG, Config.FORMAT_PNG, Config.FORMAT_WEBP, "JPEG" -> Config.MIME_IMAGE
                        else -> Config.MIME_AUDIO
                    }
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_ACTION_RECEIVER, "Failed to open file", e)
                    ToastUtils.showShort(context, R.string.downloads_toast_no_app_to_open)
                }
            } else {
                ToastUtils.showShort(context, R.string.downloads_toast_file_not_found)
            }
        }
    }

    private suspend fun shareFile(context: Context, downloadId: Long) {
        val storageService = StorageService.getInstance(context)
        val record = storageService.getDownloadById(downloadId) ?: return
        val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(context, record.title, record.id, record.format)
        
        withContext(Dispatchers.Main) {
            if (file.exists()) {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val mimeType = when (record.format.uppercase()) {
                        Config.FORMAT_MP4, Config.FORMAT_WEBM -> Config.MIME_VIDEO
                        Config.FORMAT_JPG, Config.FORMAT_PNG, Config.FORMAT_WEBP, "JPEG" -> Config.MIME_IMAGE
                        else -> Config.MIME_AUDIO
                    }
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    val chooser = Intent.createChooser(intent, context.getString(R.string.downloads_action_share_with)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_ACTION_RECEIVER, "Failed to share file", e)
                    ToastUtils.showShort(context, R.string.downloads_toast_share_error)
                }
            } else {
                ToastUtils.showShort(context, R.string.downloads_toast_file_not_found_short)
            }
        }
    }
}
