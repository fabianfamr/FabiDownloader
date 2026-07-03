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
import com.fabian.downloader.DownloadActionReceiver
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

        val largeIcon = if (!thumbnailUrl.isNullOrEmpty()) {
            val bitmap = getBitmapFromUrl(thumbnailUrl)
            if (bitmap != null) {
                val density = context.resources.displayMetrics.density
                val sizePx = (64 * density).toInt()
                Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            } else null
        } else null

        // Crear Intent de Acción Pausar/Cancelar
        val pauseIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = "com.fabian.downloader.ACTION_PAUSE"
            putExtra("EXTRA_DOWNLOAD_ID", id.toLong())
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            pauseIntent,
            flags
        )

        val summaryNotification = NotificationCompat.Builder(context, channelProgressId)
            .setContentTitle("Descargas activas")
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("Descargando archivos"))
            .setGroup(groupId)
            .setGroupSummary(true)
            .build()

        val isPreparing = progress < 0
        val contentText = StringBuilder()
        if (isPreparing) {
            contentText.append("Preparando descarga...")
        } else {
            contentText.append("Descargando... $progress%")
        }

        if (!speed.isNullOrEmpty() && speed != "Esperando..." && speed != "Conectando...") {
            contentText.append(" • ").append(speed)
        }
        if (!size.isNullOrEmpty() && size != "En cola..." && size != "Auto") {
            contentText.append(" • ").append(size)
        }

        val notification = NotificationCompat.Builder(context, channelProgressId)
            .setContentTitle(title)
            .setContentText(contentText.toString())
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setProgress(100, if (isPreparing) 0 else progress, isPreparing)
            .setGroup(groupId)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Pausar", pausePendingIntent)
            .build()

        notificationManager.notify(id, notification)
        notificationManager.notify(0, summaryNotification) // Summary ID is 0
    }

    suspend fun showDownloadSuccess(id: Int, title: String, thumbnailUrl: String? = null) {
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
        notificationManager.cancel(id)
        notificationManager.cancel(id + 300000)
        notificationManager.cancel(id + 500000)
    }
}
