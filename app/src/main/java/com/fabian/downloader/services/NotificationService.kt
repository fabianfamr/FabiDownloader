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
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelProgressId = "downloads_channel_progress"
    private val channelStatusId = "downloads_channel_status"
    private val groupId = "downloads_group"
    
    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para descargas en curso (Silencioso para que no vibre con cada porcentaje)
            val progressChannel = NotificationChannel(
                channelProgressId,
                "Descargas en Progreso",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el progreso en tiempo real de las descargas activas"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(progressChannel)

            // Canal para descargas finalizadas/fallidas (Con sonido y vibración)
            val statusChannel = NotificationChannel(
                channelStatusId,
                "Estado de Descargas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de descargas completadas con éxito o fallidas"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(statusChannel)
        }
    }

    private val thumbnailCache = mutableMapOf<String, Bitmap>()
    private val shownSuccessIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

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

        // Quitar la notificación de descargas activas (no mostrar progreso menor a 100%)
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
            action = "com.fabian.downloader.ACTION_OPEN"
            putExtra("EXTRA_DOWNLOAD_ID", id.toLong())
        }
        val openPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 100000,
            openIntent,
            flags
        )

        // Crear Acción Compartir
        val shareIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = "com.fabian.downloader.ACTION_SHARE"
            putExtra("EXTRA_DOWNLOAD_ID", id.toLong())
        }
        val sharePendingIntent = PendingIntent.getBroadcast(
            context,
            id + 200000,
            shareIntent,
            flags
        )

        val notification = NotificationCompat.Builder(context, channelStatusId)
            .setContentTitle("Descarga completada")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(android.R.drawable.ic_media_play, "Abrir", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Compartir", sharePendingIntent)
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
            action = "com.fabian.downloader.ACTION_RETRY"
            putExtra("EXTRA_DOWNLOAD_ID", id.toLong())
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 400000,
            retryIntent,
            flags
        )

        val cleanTitle = title.replace("Fallo: ", "")

        val notification = NotificationCompat.Builder(context, channelStatusId)
            .setContentTitle("Descarga fallida")
            .setContentText("$cleanTitle\n$errorMsg")
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(R.drawable.ic_cloud_download, "Reintentar", retryPendingIntent)
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
