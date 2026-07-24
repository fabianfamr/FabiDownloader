package com.fabian.downloader.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fabian.downloader.services.DownloadActionReceiver
import com.fabian.downloader.R
import com.fabian.downloader.utils.Config
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelProgressId = Config.NOTIF_CHANNEL_PROGRESS
    private val channelStatusId = Config.NOTIF_CHANNEL_STATUS
    private val groupId = Config.NOTIF_GROUP
    
    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para descargas en curso (Silencioso para que no vibre con cada porcentaje)
            val progressChannel = NotificationChannel(
                channelProgressId,
                context.getString(R.string.notif_channel_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_progress_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(progressChannel)

            // Canal para descargas finalizadas/fallidas (Con sonido y vibración)
            val statusChannel = NotificationChannel(
                channelStatusId,
                context.getString(R.string.notif_channel_status),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_status_desc)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(statusChannel)
        }
    }

    private val thumbnailCache = mutableMapOf<String, Bitmap>()
    private val shownSuccessIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private var foregroundDownloadId: Int? = null

    /**
     * Shows download progress in the notification bar.
     *
     * NOTE: By design, real-time progress notifications are DISABLED to avoid vibration/spam
     * on every percentage update. Only the final state (100%) triggers a notification via
     * [showDownloadSuccess]. This is intentional — the progress bar is shown only inside
     * the app UI, not in the system notification.
     *
     * If you need to re-enable progress notifications, replace the early `return` below
     * with a NotificationCompat.Builder progress bar update.
     */
    suspend fun showDownloadProgress(
        id: Int,
        title: String,
        progress: Int,
        thumbnailUrl: String? = null,
        speed: String? = null,
        size: String? = null
    ) {
        if (progress >= 100) {
            showDownloadSuccess(id, title, thumbnailUrl)
            return
        }

        val text = buildString {
            append("$progress%")
            if (!speed.isNullOrEmpty() && speed != Config.STATUS_UNKNOWN && speed != Config.STATUS_CONNECTING && speed != Config.STATUS_DOWNLOADING) {
                append(" • $speed")
            }
            if (!size.isNullOrEmpty() && size != Config.STATUS_UNKNOWN) {
                append(" • $size")
            }
        }

        val largeIcon = if (!thumbnailUrl.isNullOrEmpty()) {
            val bitmap = getBitmapFromUrl(thumbnailUrl)
            if (bitmap != null) {
                val density = context.resources.displayMetrics.density
                val sizePx = (64 * density).toInt()
                Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            } else null
        } else null

        // Mapear de forma transparente la primera descarga activa al ID 9999
        // para reemplazar el texto genérico del Foreground Service.
        val notificationId = synchronized(this) {
            if (foregroundDownloadId == null || foregroundDownloadId == id) {
                foregroundDownloadId = id
                9999
            } else {
                id
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Acción de Pausar
        val pauseIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = Config.ACTION_PAUSE
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            id + 600000,
            pauseIntent,
            flags
        )

        // Acción de Cancelar
        val cancelIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = Config.ACTION_CANCEL
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 700000,
            cancelIntent,
            flags
        )

        val notification = NotificationCompat.Builder(context, channelProgressId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, context.getString(R.string.notif_action_pause), pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.notif_action_cancel), cancelPendingIntent)
            .build()
            
        notificationManager.notify(notificationId, notification)
    }

    suspend fun showDownloadPaused(
        id: Int,
        title: String,
        thumbnailUrl: String? = null
    ) {
        val largeIcon = if (!thumbnailUrl.isNullOrEmpty()) {
            val bitmap = getBitmapFromUrl(thumbnailUrl)
            if (bitmap != null) {
                val density = context.resources.displayMetrics.density
                val sizePx = (64 * density).toInt()
                Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            } else null
        } else null

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Acción de Reanudar
        val resumeIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = Config.ACTION_RESUME
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            context,
            id + 800000,
            resumeIntent,
            flags
        )

        // Acción de Cancelar
        val cancelIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = Config.ACTION_CANCEL
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 900000,
            cancelIntent,
            flags
        )

        val notificationId = synchronized(this) {
            if (foregroundDownloadId == id) {
                9999
            } else {
                id
            }
        }

        val notification = NotificationCompat.Builder(context, channelProgressId)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.downloads_toast_paused))
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_play, context.getString(R.string.notif_action_resume), resumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.notif_action_cancel), cancelPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    suspend fun showDownloadSuccess(id: Int, title: String, thumbnailUrl: String? = null) {
        if (!shownSuccessIds.add(id)) {
            return
        }

        // Primero, cancelar la notificación del canal de progreso en el ID correcto
        synchronized(this) {
            if (foregroundDownloadId == id) {
                notificationManager.cancel(9999)
                foregroundDownloadId = null
            } else {
                notificationManager.cancel(id)
            }
        }

        val largeIcon = if (!thumbnailUrl.isNullOrEmpty()) {
            val bitmap = getBitmapFromUrl(thumbnailUrl)
            if (bitmap != null) {
                val density = context.resources.displayMetrics.density
                val sizePx = (64 * density).toInt()
                Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            } else null
        } else null

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Crear Acción Abrir
        val openIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = Config.ACTION_OPEN
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val openPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 100000,
            openIntent,
            flags
        )

        // Crear Acción Compartir
        val shareIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = Config.ACTION_SHARE
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val sharePendingIntent = PendingIntent.getBroadcast(
            context,
            id + 200000,
            shareIntent,
            flags
        )

        val channelIdToUse = if (com.fabian.downloader.MyApplication.getInstance().isAppInForeground) {
            channelProgressId // Silent channel when in foreground
        } else {
            channelStatusId // Default channel (with sound) when in background
        }

        val notification = NotificationCompat.Builder(context, channelIdToUse)
            .setContentTitle(context.getString(R.string.notif_title_completed))
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_play, context.getString(R.string.notif_action_open), openPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, context.getString(R.string.notif_action_share), sharePendingIntent)
            .build()

        notificationManager.notify(id + 300000, notification) // ID diferente para no solapar con el progreso ya cancelado
    }

    suspend fun showDownloadFailed(id: Int, title: String, errorMsg: String, thumbnailUrl: String? = null) {
        shownSuccessIds.remove(id)
        // Cancelar el progreso primero en el ID correcto
        synchronized(this) {
            if (foregroundDownloadId == id) {
                notificationManager.cancel(9999)
                foregroundDownloadId = null
            } else {
                notificationManager.cancel(id)
            }
        }

        val largeIcon = if (!thumbnailUrl.isNullOrEmpty()) {
            val bitmap = getBitmapFromUrl(thumbnailUrl)
            if (bitmap != null) {
                val density = context.resources.displayMetrics.density
                val sizePx = (64 * density).toInt()
                Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            } else null
        } else null

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Crear Acción Reintentar
        val retryIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = Config.ACTION_RETRY
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 400000,
            retryIntent,
            flags
        )

        val cleanTitle = title.removePrefix(Config.STATUS_FAILED_PREFIX)

        val channelIdToUse = if (com.fabian.downloader.MyApplication.getInstance().isAppInForeground) {
            channelProgressId // Silent channel when in foreground
        } else {
            channelStatusId // Default channel (with sound) when in background
        }

        val notification = NotificationCompat.Builder(context, channelIdToUse)
            .setContentTitle(context.getString(R.string.notif_title_failed))
            .setContentText("$cleanTitle\n$errorMsg")
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(R.drawable.ic_cloud_download, context.getString(R.string.notif_action_retry), retryPendingIntent)
            .build()

        notificationManager.notify(id + 500000, notification)
    }

    private suspend fun getBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val cached = thumbnailCache[url]
        if (cached != null) return@withContext cached

        try {
            val bitmap = if (url.startsWith("http://") || url.startsWith("https://")) {
                val connection = URL(url).openConnection()
                connection.doInput = true
                connection.connect()
                connection.getInputStream().use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                // Soporte para cargar miniaturas guardadas localmente en el dispositivo
                BitmapFactory.decodeFile(url)
            }
            if (bitmap != null) {
                thumbnailCache[url] = bitmap
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    fun cancelNotification(id: Int) {
        shownSuccessIds.remove(id)
        synchronized(this) {
            if (foregroundDownloadId == id) {
                notificationManager.cancel(9999)
                foregroundDownloadId = null
            } else {
                notificationManager.cancel(id)
            }
        }
        notificationManager.cancel(id + 300000)
        notificationManager.cancel(id + 500000)
    }

    fun cancelProgressNotification(id: Int) {
        synchronized(this) {
            if (foregroundDownloadId == id) {
                notificationManager.cancel(9999)
                foregroundDownloadId = null
            } else {
                notificationManager.cancel(id)
            }
        }
    }
}
