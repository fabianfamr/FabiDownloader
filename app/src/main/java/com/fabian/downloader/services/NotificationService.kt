package com.fabian.downloader.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fabian.downloader.receivers.DownloadActionReceiver
import com.fabian.downloader.MainActivity
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelProgressId = Config.NOTIF_CHANNEL_PROGRESS
    private val channelStatusId = Config.NOTIF_CHANNEL_STATUS
    private val groupId = Config.NOTIF_GROUP
    
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pendingDismissRunnables = java.util.Collections.synchronizedMap(mutableMapOf<Int, Runnable>())

    private fun cancelPendingDismiss(id: Int) {
        val notificationId = synchronized(this) {
            if (foregroundDownloadId == id) 9999 else id
        }
        val runnable = pendingDismissRunnables.remove(notificationId)
        if (runnable != null) {
            mainHandler.removeCallbacks(runnable)
        }
        val rawRunnable = pendingDismissRunnables.remove(id)
        if (rawRunnable != null) {
            mainHandler.removeCallbacks(rawRunnable)
        }
    }
    
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

    private val thumbnailCache = android.util.LruCache<String, Bitmap>(20)
    private val shownSuccessIds: MutableSet<Int> = java.util.Collections.synchronizedSet(
        java.util.Collections.newSetFromMap(
            object : java.util.LinkedHashMap<Int, Boolean>(100, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Boolean>?): Boolean {
                    return size > 100
                }
            }
        )
    )
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

        cancelPendingDismiss(id)

        val localizedCtx = com.fabian.downloader.utils.LocaleHelper.applyLocale(context, com.fabian.downloader.ui.AppSettings.language)
        val text = buildString {
            if (progress >= 0) {
                append("$progress%")
            }
            if (!size.isNullOrEmpty() && size != Config.STATUS_UNKNOWN) {
                val cleanSize = com.fabian.downloader.utils.YtdlpParser.getLocalizedSize(localizedCtx, size)
                if (progress >= 0) {
                    append(" • $cleanSize")
                } else {
                    append(cleanSize)
                }
            }
            if (showSpeed && !speed.isNullOrEmpty() && speed != Config.STATUS_WAITING) {
                val cleanSpeed = com.fabian.downloader.utils.YtdlpParser.getLocalizedStatus(localizedCtx, speed)
                append(" • $cleanSpeed")
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

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Al presionar la descarga en curso ir a la pantalla de descargas en proceso (página index 1)
        val appIntent = Intent(context, MainActivity::class.java).apply {
            setClass(context, MainActivity::class.java)
            component = ComponentName(context, MainActivity::class.java)
            setPackage(context.packageName)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
            putExtra(Config.EXTRA_INITIAL_PAGE, 1)
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            id + 500000,
            appIntent,
            flags
        )

        val notification = NotificationCompat.Builder(context, channelProgressId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setProgress(100, if (progress < 0) 0 else progress, progress < 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(appPendingIntent)
            .build()
            
        notificationManager.notify(notificationId, notification)
    }

    suspend fun showDownloadPaused(
        id: Int,
        title: String,
        thumbnailUrl: String? = null
    ) {
        cancelPendingDismiss(id)

        val largeIcon = if (!thumbnailUrl.isNullOrEmpty()) {
            val bitmap = getBitmapFromUrl(thumbnailUrl)
            if (bitmap != null) {
                val density = context.resources.displayMetrics.density
                val sizePx = (64 * density).toInt()
                Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            } else null
        } else null

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Al presionar la notificación ir a la pantalla de descargas en proceso (página index 1)
        val appIntent = Intent(context, MainActivity::class.java).apply {
            setClass(context, MainActivity::class.java)
            component = ComponentName(context, MainActivity::class.java)
            setPackage(context.packageName)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
            putExtra(Config.EXTRA_INITIAL_PAGE, 1)
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            id + 510000,
            appIntent,
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
            .setContentIntent(appPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)

        // Programar el auto-descarte según la configuración
        val timeoutMs = com.fabian.downloader.ui.AppSettings.pausedNotificationTimeoutMs
        if (timeoutMs > 0) {
            val dismissRunnable = Runnable {
                cancelProgressNotification(id)
                pendingDismissRunnables.remove(notificationId)
            }
            pendingDismissRunnables[notificationId] = dismissRunnable
            mainHandler.postDelayed(dismissRunnable, timeoutMs)
        }
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

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Crear Acción Abrir (Explicit Intent with ComponentName)
        val openIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            setClass(context, DownloadActionReceiver::class.java)
            component = ComponentName(context, DownloadActionReceiver::class.java)
            setPackage(context.packageName)
            action = Config.ACTION_OPEN
            putExtra(Config.EXTRA_DOWNLOAD_ID, id.toLong())
        }
        val openPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 100000,
            openIntent,
            flags
        )

        // Crear Acción Compartir (Explicit Intent with ComponentName)
        val shareIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            setClass(context, DownloadActionReceiver::class.java)
            component = ComponentName(context, DownloadActionReceiver::class.java)
            setPackage(context.packageName)
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
            .addAction(R.drawable.ic_cloud_download, context.getString(R.string.notif_action_share), sharePendingIntent)
            .build()

        notificationManager.notify(id + 300000, notification) // ID diferente para no solapar con el progreso ya cancelado
    }

    fun showBatchCompleteNotification(count: Int) {
        if (!com.fabian.downloader.ui.AppSettings.notifyBatchComplete) return
        if (count <= 0) return

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val appIntent = Intent(context, MainActivity::class.java).apply {
            setClass(context, MainActivity::class.java)
            component = ComponentName(context, MainActivity::class.java)
            setPackage(context.packageName)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
            putExtra(Config.EXTRA_INITIAL_PAGE, 0)
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            9998,
            appIntent,
            flags
        )

        val notification = NotificationCompat.Builder(context, channelStatusId)
            .setContentTitle(context.getString(R.string.notif_title_completed))
            .setContentText(context.getString(R.string.notif_batch_completed, count))
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(appPendingIntent)
            .build()

        notificationManager.notify(9998, notification)
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

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Crear Acción Reintentar (Explicit Intent with ComponentName)
        val retryIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            setClass(context, DownloadActionReceiver::class.java)
            component = ComponentName(context, DownloadActionReceiver::class.java)
            setPackage(context.packageName)
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

        // Crear Intent para abrir la aplicación en la pestaña de progreso
        val appIntent = Intent(context, MainActivity::class.java).apply {
            setClass(context, MainActivity::class.java)
            component = ComponentName(context, MainActivity::class.java)
            setPackage(context.packageName)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
            putExtra(Config.EXTRA_INITIAL_PAGE, 1)
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            id + 520000,
            appIntent,
            flags
        )

        val channelIdToUse = if (com.fabian.downloader.MyApplication.getInstance().isAppInForeground) {
            channelProgressId // Silent channel when in foreground
        } else {
            channelStatusId // Default channel (with sound) when in background
        }

        val notification = NotificationCompat.Builder(context, channelIdToUse)
            .setContentTitle(context.getString(R.string.notif_title_failed))
            .setContentText("$cleanTitle\n$errorMsg")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$cleanTitle\n$errorMsg"))
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_cloud_download, context.getString(R.string.notif_action_retry), retryPendingIntent)
            .build()

        notificationManager.notify(id + 500000, notification)
    }

    private suspend fun getBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val cached = thumbnailCache.get(url)
        if (cached != null) return@withContext cached

        try {
            val imageLoader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(url)
                .size(256, 256)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            val drawable = result.drawable
            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap != null) {
                thumbnailCache.put(url, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }
    
    fun cancelNotification(id: Int) {
        cancelPendingDismiss(id)
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
        cancelPendingDismiss(id)
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

