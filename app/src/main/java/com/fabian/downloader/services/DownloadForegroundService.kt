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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.fabian.downloader.configs.Config
import com.fabian.downloader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {

    private var serviceStartTimeMs: Long = 0L

    companion object {
        private const val NOTIFICATION_ID = 9999
        const val ACTION_START = "com.fabian.downloader.ACTION_START_FOREGROUND"
        const val ACTION_STOP = "com.fabian.downloader.ACTION_STOP_FOREGROUND"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        private var instance: DownloadForegroundService? = null

        fun start(context: Context) {
            try {
                val intent = Intent(context, DownloadForegroundService::class.java).apply {
                    action = ACTION_START
                    setClass(context, DownloadForegroundService::class.java)
                    component = android.content.ComponentName(context, DownloadForegroundService::class.java)
                    setPackage(context.packageName)
                }
                
                if (isRunning) {
                    context.startService(intent)
                    return
                }

                try {
                    context.startService(intent)
                } catch (e: IllegalStateException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            androidx.core.content.ContextCompat.startForegroundService(context, intent)
                        } catch (e2: Throwable) {
                            Log.w("DownloadService", "No se pudo iniciar startForegroundService desde background context", e2)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("DownloadService", "Error iniciando DownloadForegroundService", e)
            }
        }

        fun stop(context: Context) {
            try {
                if (!isRunning) return
                val srv = instance
                if (srv != null) {
                    srv.scheduleStopForegroundAndSelf()
                } else {
                    // Si no hay instancia, el servicio se detendrá por sí solo gracias al timeout de 3.5s en onStartCommand
                    // Evitamos llamar a stopService() para prevenir el RemoteServiceException en Android 12+
                }
            } catch (e: Throwable) {
                Log.e("DownloadService", "Error solicitando stop para DownloadForegroundService", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        serviceStartTimeMs = System.currentTimeMillis()
        promoteToForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        serviceStartTimeMs = System.currentTimeMillis()
        
        // Garantizar siempre la llamada a startForeground de inmediato para cumplir con el contrato del SO
        promoteToForeground()

        if (intent?.action == ACTION_STOP) {
            scheduleStopForegroundAndSelf()
            return START_NOT_STICKY
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(3500) // Verificar tras 3.5 segundos si aún hay descargas activas
            try {
                val hasActive = DownloadManagerService.getInstance(applicationContext).hasActiveDownloads()
                if (!hasActive) {
                    scheduleStopForegroundAndSelf()
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
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } catch (e: Throwable) {
                    Log.w("DownloadService", "Fallo startForeground con DATA_SYNC, fallback a estándar", e)
                    try {
                        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
                    } catch (e2: Throwable) {
                        Log.e("DownloadService", "Error en fallback startForeground", e2)
                        stopSelf()
                    }
                }
            } else {
                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
                } catch (e: Throwable) {
                    Log.e("DownloadService", "Error en startForeground pre-Q", e)
                    stopSelf()
                }
            }
        } catch (e: Throwable) {
            Log.e("DownloadService", "Error llamando promoteToForeground", e)
            stopSelf()
        }
    }

    private fun scheduleStopForegroundAndSelf() {
        val now = System.currentTimeMillis()
        val elapsed = now - serviceStartTimeMs
        val minDuration = 4000L // Garantizar al menos 4 segundos de vida activa para cumplir el contrato del SO
        val delayTime = if (elapsed < minDuration) minDuration - elapsed else 500L

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val hasActiveNow = try {
                    DownloadManagerService.getInstance(applicationContext).hasActiveDownloads()
                } catch (_: Exception) { false }

                // Si entre tanto hay nuevas descargas activas, cancelar la detención del servicio
                if (hasActiveNow) {
                    return@postDelayed
                }

                isRunning = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } catch (_: Throwable) {}
                } else {
                    @Suppress("DEPRECATION")
                    try {
                        stopForeground(true)
                    } catch (_: Throwable) {}
                }
                stopSelf()
            } catch (e: Throwable) {
                Log.e("DownloadService", "Error al detener DownloadForegroundService", e)
                try { stopSelf() } catch (_: Throwable) {}
            }
        }, delayTime)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
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
                Log.i("DownloadService", "La app se cerró de recientes pero hay descargas activas. El servicio en segundo plano continúa en ejecución.")
            }
        } catch (e: Exception) {
            Log.e("DownloadService", "Error comprobando descargas activas en onTaskRemoved", e)
        }
    }
}


