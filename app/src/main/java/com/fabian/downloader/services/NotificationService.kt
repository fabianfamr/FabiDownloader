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

        // Progress notifications are intentionally disabled (see kdoc above)
        return
    }

    suspend fun showDownloadSuccess(id: Int, title: String, thumbnailUrl: String? = null) {
        if (!shownSuccessIds.add(id)) {
            return
        }

        // Primero, cancelar la notificación del canal de progreso
        notificationManager.cancel(id)

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
            .addAction(android.R.drawable.ic_media_play, context.getString(R.string.notif_action_open), openPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, context.getString(R.string.notif_action_share), sharePendingIntent)
            .build()

        notificationManager.notify(id + 300000, notification) // ID diferente para no solapar con el progreso ya cancelado
    }

    suspend fun showDownloadFailed(id: Int, title: String, errorMsg: String, thumbnailUrl: String? = null) {
        shownSuccessIds.remove(id)
        // Cancelar el progreso primero
        notificationManager.cancel(id)

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
        try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            connection.getInputStream().use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun cancelNotification(id: Int) {
        shownSuccessIds.remove(id)
        notificationManager.cancel(id)
        notificationManager.cancel(id + 300000)
        notificationManager.cancel(id + 500000)
    }
}
