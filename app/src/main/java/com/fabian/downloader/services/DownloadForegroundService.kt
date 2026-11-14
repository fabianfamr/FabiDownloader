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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var idleCheckJob: Job? = null
    private var serviceStartTimeMs: Long = 0L
    private var isPromotedToForeground: Boolean = false
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

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
                if (isRunning) return

                val intent = Intent(context, DownloadForegroundService::class.java).apply {
                    action = ACTION_START
                    setClass(context, DownloadForegroundService::class.java)
                    component = android.content.ComponentName(context, DownloadForegroundService::class.java)
                    setPackage(context.packageName)
                }

                val isFg = try {
                    com.fabian.downloader.MyApplication.getInstance().isAppInForeground
                } catch (_: Exception) { true }

                if (isFg) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            androidx.core.content.ContextCompat.startForegroundService(context, intent)
                        } else {
                            context.startService(intent)
                        }
                    } catch (e: Throwable) {
                        try {
                            context.startService(intent)
                        } catch (e2: Throwable) {
                            Log.w("DownloadService", "Error iniciando foreground service: ${e2.message}")
                        }
                    }
                } else {
                    try {
                        context.startService(intent)
                    } catch (e: Exception) {
                        Log.w("DownloadService", "App en background, startService omitido: ${e.message}")
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
        val locks = com.fabian.downloader.utils.DeviceOptimizationHelper.acquireDownloadLocks(this)
        wakeLock = locks.first
        wifiLock = locks.second
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
        
        idleCheckJob?.cancel()
        idleCheckJob = serviceScope.launch {
            kotlinx.coroutines.delay(3500)
            try {
                val hasActive = DownloadManagerService.getInstance(applicationContext).hasActiveDownloads()
                if (!hasActive) {
                    scheduleStopForegroundAndSelf()
                }
            } catch (_: Exception) {}
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground(): Boolean {
        if (isPromotedToForeground) return true
        try {
            createNotificationChannel()
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    isPromotedToForeground = true
                    return true
                } catch (e: Throwable) {
                    Log.w("DownloadService", "Fallo startForeground con DATA_SYNC: ${e.message}, intentando sin flags...")
                    try {
                        startForeground(NOTIFICATION_ID, notification)
                        isPromotedToForeground = true
                        return true
                    } catch (e2: Throwable) {
                        Log.w("DownloadService", "startForeground no permitido por el sistema: ${e2.message}")
                        // IMPORTANTE: NO llamar a stopSelf() aquí. Llamar a stopSelf() antes de que startForeground
                        // sea aceptado por el SO dispara inmediatamente ForegroundServiceDidNotStartInTimeException.
                        return false
                    }
                }
            } else {
                try {
                    startForeground(NOTIFICATION_ID, notification)
                    isPromotedToForeground = true
                    return true
                } catch (e: Throwable) {
                    Log.w("DownloadService", "startForeground falló en pre-Q: ${e.message}")
                    return false
                }
            }
        } catch (e: Throwable) {
            Log.w("DownloadService", "Excepción en promoteToForeground: ${e.message}")
            return false
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
        isPromotedToForeground = false
        idleCheckJob?.cancel()
        serviceScope.cancel()
        try {
            com.fabian.downloader.utils.DeviceOptimizationHelper.releaseDownloadLocks(wakeLock, wifiLock)
            wakeLock = null
            wifiLock = null
        } catch (_: Throwable) {}
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
        return com.fabian.downloader.util.NotificationUtil.makeServiceNotification(this)
    }

    private fun createNotificationChannel() {
        NotificationService.createAllNotificationChannels(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val manager = DownloadManagerService.getInstance(applicationContext)
            if (!manager.hasActiveDownloads()) {
                serviceScope.launch {
                    manager.onAppClosed()
                    Handler(Looper.getMainLooper()).post {
                        try { stopSelf() } catch (_: Throwable) {}
                    }
                }
            } else {
                Log.i("DownloadService", "La app se cerró de recientes pero hay descargas activas. El servicio en segundo plano continúa en ejecución.")
            }
        } catch (e: Exception) {
            Log.e("DownloadService", "Error comprobando descargas activas en onTaskRemoved", e)
        }
    }
}


