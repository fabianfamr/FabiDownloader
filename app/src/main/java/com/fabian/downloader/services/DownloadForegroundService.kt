package com.fabian.downloader.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
        const val ACTION_START = "com.fabian.downloader.ACTION_START_FOREGROUND"
        const val ACTION_STOP = "com.fabian.downloader.ACTION_STOP_FOREGROUND"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            try {
                val intent = Intent(context, DownloadForegroundService::class.java).apply {
                    action = ACTION_START
                    setClass(context, DownloadForegroundService::class.java)
                    component = android.content.ComponentName(context, DownloadForegroundService::class.java)
                    setPackage(context.packageName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadService", "Error starting DownloadForegroundService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DownloadForegroundService::class.java).apply {
                    action = ACTION_STOP
                    setClass(context, DownloadForegroundService::class.java)
                    component = android.content.ComponentName(context, DownloadForegroundService::class.java)
                    setPackage(context.packageName)
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("DownloadService", "Error requesting stop for DownloadForegroundService", e)
                try {
                    val fallbackIntent = Intent(context, DownloadForegroundService::class.java)
                    context.stopService(fallbackIntent)
                } catch (_: Exception) {}
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
        // Garantizar siempre la llamada a startForeground de inmediato para cumplir con el contrato del SO
        promoteToForeground()

        if (intent?.action == ACTION_STOP) {
            stopForegroundAndSelf()
            return START_NOT_STICKY
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            delay(2000) // Dar tiempo de sincronización a las tareas recién encoladas
            try {
                val hasActive = DownloadManagerService.getInstance(applicationContext).hasActiveDownloads()
                if (!hasActive) {
                    stopForegroundAndSelf()
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
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e: Throwable) {
                    android.util.Log.w("DownloadService", "Fallo startForeground con DATA_SYNC, fallback a estándar", e)
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            android.util.Log.e("DownloadService", "Error calling startForeground in DownloadForegroundService", e)
        }
    }

    private fun stopForegroundAndSelf() {
        isRunning = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}
        stopSelf()
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
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val appIntent = Intent(this, com.fabian.downloader.MainActivity::class.java).apply {
            setClass(this@DownloadForegroundService, com.fabian.downloader.MainActivity::class.java)
            component = android.content.ComponentName(this@DownloadForegroundService, com.fabian.downloader.MainActivity::class.java)
            setPackage(packageName)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
            putExtra(Config.EXTRA_INITIAL_PAGE, 1)
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            9999,
            appIntent,
            flags
        )
        return NotificationCompat.Builder(this, channelIdToUse)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_foreground_service))
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
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

