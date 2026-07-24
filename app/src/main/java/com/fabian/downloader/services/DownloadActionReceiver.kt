package com.fabian.downloader.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.services.DownloadManagerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fabian.downloader.R
import com.fabian.downloader.utils.Config

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
        val database = AppDatabase.getInstance(context)
        val record = database.downloadDao().getDownloadById(downloadId) ?: return
        
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
        val database = AppDatabase.getInstance(context)
        val record = database.downloadDao().getDownloadById(downloadId) ?: return
        
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
        val database = AppDatabase.getInstance(context)
        val record = database.downloadDao().getDownloadById(downloadId) ?: return
        val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(context, record.title, record.id, record.format)
        
        withContext(Dispatchers.Main) {
            if (file.exists()) {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val mimeType = if (record.format.uppercase() == Config.FORMAT_MP3 || record.format.uppercase() == Config.FORMAT_M4A) {
                        Config.MIME_AUDIO
                    } else {
                        Config.MIME_VIDEO
                    }
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_ACTION_RECEIVER, "Failed to open file", e)
                    Toast.makeText(context, context.getString(R.string.downloads_toast_no_app_to_open), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, context.getString(R.string.downloads_toast_file_not_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun shareFile(context: Context, downloadId: Long) {
        val database = AppDatabase.getInstance(context)
        val record = database.downloadDao().getDownloadById(downloadId) ?: return
        val file = com.fabian.downloader.utils.PathUtils.getDownloadFile(context, record.title, record.id, record.format)
        
        withContext(Dispatchers.Main) {
            if (file.exists()) {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (record.format.uppercase() == Config.FORMAT_MP3 || record.format.uppercase() == Config.FORMAT_M4A) Config.MIME_AUDIO else Config.MIME_VIDEO
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
                    Toast.makeText(context, context.getString(R.string.downloads_toast_share_error), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, context.getString(R.string.downloads_toast_file_not_found_short), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
