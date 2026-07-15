package com.fabian.downloader.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fabian.downloader.R

class DownloadForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 9999
        private const val CHANNEL_ID = "download_background_service_channel"

        fun start(context: Context) {
            try {
                val intent = Intent(context, DownloadForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadService", "Error starting DownloadForegroundService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DownloadForegroundService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                android.util.Log.e("DownloadService", "Error stopping DownloadForegroundService", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelIdToUse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) CHANNEL_ID else "default"
        return NotificationCompat.Builder(this, channelIdToUse)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Descargando en segundo plano...")
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Descarga en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene las descargas activas en segundo plano"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
