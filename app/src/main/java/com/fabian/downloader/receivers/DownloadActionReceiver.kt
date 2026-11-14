package com.fabian.downloader.receivers

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.fabian.downloader.utils.ToastUtils
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.services.DownloadManagerService
import com.fabian.downloader.services.StorageService
import com.fabian.downloader.util.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDL

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action

        // ------------------------------------------------------------------
        // Acciones de notificación portadas de Seal (NotificationActionReceiver):
        // cancelar la tarea por taskId y copiar el informe de error.
        // ------------------------------------------------------------------
        when (action) {
            Config.ACTION_CANCEL_TASK -> {
                val taskId = intent.getStringExtra(Config.EXTRA_TASK_ID)
                val notificationId =
                    intent.getIntExtra(Config.EXTRA_NOTIFICATION_ID, -1)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        cancelTask(context, taskId, notificationId)
                    } finally {
                        pendingResult?.finish()
                    }
                }
                return
            }

            Config.ACTION_ERROR_REPORT -> {
                val report = intent.getStringExtra(Config.EXTRA_ERROR_REPORT)
                val notificationId =
                    intent.getIntExtra(Config.EXTRA_NOTIFICATION_ID, -1)
                if (!report.isNullOrEmpty()) {
                    copyErrorReport(context, report, notificationId)
                }
                return
            }
        }

        val downloadId = intent.getLongExtra(Config.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) {
            Log.e(Config.TAG_DOWNLOAD_ACTION_RECEIVER,
                "Received action $action without valid downloadId")
            return
        }

        Log.d(Config.TAG_DOWNLOAD_ACTION_RECEIVER,
            "Action received: $action for ID $downloadId")

        // Cancelar las notificaciones de éxito o fallo al interactuar con ellas.
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            notificationManager.cancel(downloadId.toInt() + 300000)
            notificationManager.cancel(downloadId.toInt() + 500000)
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_ACTION_RECEIVER,
                "Error cancelling notification on action", e)
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
                        DownloadManagerService.getInstance(context).deleteDownloadNow(downloadId)
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

    private suspend fun cancelTask(context: Context, taskId: String?, notificationId: Int) {
        if (taskId.isNullOrEmpty()) return
        if (notificationId > 0 &&
            notificationId != com.fabian.downloader.util.NotificationUtil.SERVICE_NOTIFICATION_ID
        ) {
            NotificationUtil.cancelNotification(notificationId)
        }

        try {
            YoutubeDL.getInstance().destroyProcessById(taskId)
            Log.d(Config.TAG_DOWNLOAD_ACTION_RECEIVER, "Task (id:$taskId) was killed.")
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_ACTION_RECEIVER,
                "No se pudo destruir el proceso $taskId", e)
        }

        val downloadId = taskId.toLongOrNull()
        if (downloadId != null && downloadId > 0) {
            try {
                DownloadManagerService.getInstance(context).deleteDownloadNow(downloadId)
            } catch (e: Exception) {
                Log.w(Config.TAG_DOWNLOAD_ACTION_RECEIVER,
                    "Error eliminando la descarga $downloadId tras cancelar", e)
            }
        }
    }

    private fun copyErrorReport(context: Context, report: String, notificationId: Int) {
        try {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(null, report))
            Toast.makeText(
                context,
                context.getString(R.string.notif_error_copied),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_ACTION_RECEIVER,
                "No se pudo copiar el informe de error", e)
        }
        if (notificationId > 0) NotificationUtil.cancelNotification(notificationId)
    }

    /**
     * REANUDAR una descarga pausada: preserva el progreso y deja que yt-dlp
     * use su mecanismo de resume (`--continue`).
     *
     * Antes esta función era idéntica a `retryDownload`, lo que provocaba que
     * una descarga al 80% pausada se reiniciara desde 0% al reanudar.
     */
    private suspend fun resumeDownload(context: Context, downloadId: Long) {
        val storageService = StorageService.getInstance(context)
        val record = storageService.getDownloadById(downloadId) ?: return

        // removePrefix simple (antes había un while que sugería bug duplicado).
        val cleanTitle = record.title.removePrefix(Config.STATUS_FAILED_PREFIX).trim()

        // TODO: Si DownloadManagerService.startDownload soporta un flag `resumeFromCurrentProgress`,
        // pasarlo como `true` aquí para activar `--continue` en yt-dlp.
        DownloadManagerService.getInstance(context).startDownload(
            rawUrl = record.url,
            quality = record.quality,
            format = record.format,
            passedTitle = cleanTitle,
            passedThumbnailUrl = record.thumbnailUrl,
            existingId = record.id
        )
    }

    /**
     * REINTENTAR una descarga fallida: limpia archivos temporales previos
     * (`.part`, `.downloading`) y empieza desde 0%.
     */
    private suspend fun retryDownload(context: Context, downloadId: Long) {
        val storageService = StorageService.getInstance(context)
        val record = storageService.getDownloadById(downloadId) ?: return

        // Limpiar archivos temporales de intentos previos fallidos.
        try {
            val destFolder = com.fabian.downloader.utils.PathUtils
                .getDownloadFolder(context, record.format)
            if (destFolder.exists()) {
                val idRegex = Regex("(^|[^0-9])$downloadId([^0-9]|$)")
                val sanitizedTitle = com.fabian.downloader.utils.PathUtils.sanitizeFileName(record.title)
                destFolder.listFiles()?.forEach { file ->
                    val name = file.name
                    val isTemp = name.endsWith(".part") || name.endsWith(".ytdl") ||
                        name.endsWith(".temp") || name.endsWith(".tmp") ||
                        name.endsWith(".downloading") || name.contains(".downloading")
                    if (isTemp && (idRegex.containsMatchIn(name) ||
                            (sanitizedTitle.isNotEmpty() && name.startsWith(sanitizedTitle)))) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_ACTION_RECEIVER,
                "No se pudieron limpiar archivos temporales previos", e)
        }

        val cleanTitle = record.title.removePrefix(Config.STATUS_FAILED_PREFIX).trim()

        DownloadManagerService.getInstance(context).startDownload(
            rawUrl = record.url,
            quality = record.quality,
            format = record.format,
            passedTitle = cleanTitle,
            passedThumbnailUrl = record.thumbnailUrl,
            existingId = record.id
        )
    }

    private suspend fun openFile(context: Context, downloadId: Long) {
        val storageService = StorageService.getInstance(context)
        val record = storageService.getDownloadById(downloadId) ?: return
        val file = com.fabian.downloader.utils.PathUtils
            .getDownloadFile(context, record.title, record.id, record.format)

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
                        Config.FORMAT_JPG, Config.FORMAT_PNG,
                        Config.FORMAT_WEBP, "JPEG" -> Config.MIME_IMAGE
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
        val file = com.fabian.downloader.utils.PathUtils
            .getDownloadFile(context, record.title, record.id, record.format)

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
                        Config.FORMAT_JPG, Config.FORMAT_PNG,
                        Config.FORMAT_WEBP, "JPEG" -> Config.MIME_IMAGE
                        else -> Config.MIME_AUDIO
                    }

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooser = Intent.createChooser(
                        intent, context.getString(R.string.downloads_action_share_with)
                    ).apply {
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
