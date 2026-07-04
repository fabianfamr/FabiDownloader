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

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val action = intent.action
        val downloadId = intent.getLongExtra("EXTRA_DOWNLOAD_ID", -1L)
        if (downloadId == -1L) {
            Log.e("DownloadActionReceiver", "Received action $action without valid downloadId")
            return
        }
        
        Log.d("DownloadActionReceiver", "Action received: $action for ID $downloadId")
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    "com.fabian.downloader.ACTION_PAUSE" -> {
                        DownloadManagerService.getInstance(context).pauseDownload(downloadId)
                    }
                    "com.fabian.downloader.ACTION_OPEN" -> {
                        openFile(context, downloadId)
                    }
                    "com.fabian.downloader.ACTION_SHARE" -> {
                        shareFile(context, downloadId)
                    }
                    "com.fabian.downloader.ACTION_RETRY" -> {
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
                passedTitle = record.title.replace("Fallo: ", ""),
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
                    val mimeType = if (record.format.uppercase() == "MP3" || record.format.uppercase() == "M4A") {
                        "audio/*"
                    } else {
                        "video/*"
                    }
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("DownloadActionReceiver", "Failed to open file", e)
                    Toast.makeText(context, "No se encontró una aplicación para abrir el archivo", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "El archivo no existe o fue movido", Toast.LENGTH_SHORT).show()
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
                        type = if (record.format.uppercase() == "MP3" || record.format.uppercase() == "M4A") "audio/*" else "video/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    val chooser = Intent.createChooser(intent, "Compartir con").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Log.e("DownloadActionReceiver", "Failed to share file", e)
                    Toast.makeText(context, "Error al compartir archivo", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
