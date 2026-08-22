package com.fabian.downloader.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.fabian.downloader.configs.Config
import com.fabian.downloader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 9999

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (isRunning) return
            try {
                val intent = Intent(context, DownloadForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        context.startForegroundService(intent)
                    } catch (e: Exception) {
                        android.util.Log.w("DownloadService", "Fallo startForegroundService, fallback a startService", e)
                        try {
                            context.startService(intent)
                        } catch (_: Exception) {}
                    }
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
            } finally {
                isRunning = false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        promoteToForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        promoteToForeground()
        
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500) // Dar tiempo de sincronización a las tareas recién encoladas
            try {
                val hasActive = DownloadManagerService.getInstance(applicationContext).hasActiveDownloads()
                if (!hasActive) {
                    stopSelf()
                }
            } catch (_: Exception) {}
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground() {
        try {
            createNotificationChannel()
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e: Exception) {
                    android.util.Log.w("DownloadService", "Fallback startForeground standard", e)
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        0
                    )
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            android.util.Log.e("DownloadService", "Error calling startForeground in DownloadForegroundService", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelIdToUse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Config.NOTIF_CHANNEL_PROGRESS else "default"
        return NotificationCompat.Builder(this, channelIdToUse)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_foreground_service))
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    Config.NOTIF_CHANNEL_PROGRESS,
                    getString(R.string.notif_channel_progress),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notif_channel_progress_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            } catch (_: Exception) {}
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val manager = DownloadManagerService.getInstance(applicationContext)
            if (!manager.hasActiveDownloads()) {
                manager.onAppClosed()
                stopSelf()
            } else {
                android.util.Log.i("DownloadService", "La app se cerró de recientes pero hay descargas activas. El servicio en segundo plano continúa en ejecución.")
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadService", "Error comprobando descargas activas en onTaskRemoved", e)
        }
    }
}

