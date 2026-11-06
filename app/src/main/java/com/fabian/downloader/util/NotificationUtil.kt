package com.fabian.downloader.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.fabian.downloader.MainActivity
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.receivers.DownloadActionReceiver
import com.fabian.downloader.services.DownloadForegroundService

/**
 * SISTEMA DE NOTIFICACIONES (portado de Seal: `util/NotificationUtil.kt`)
 *
 * Arquitectura idéntica a la de Seal (objeto único + canal con grupo + acciones
 * sobre la notificación), adaptada a los canales, IDs e iconos de FabiDownloader:
 *
 *  - `createNotificationChannel()`: grupo de canales "Descargas" con canal de
 *    progreso y canal de servicio (como el `NotificationChannelGroup` de Seal).
 *  - `notifyProgress()`: progreso con **acción Cancelar** que mata el proceso
 *    yt-dlp por taskId (`destroyProcessById`), como `notifyProgress` de Seal.
 *  - `notifyError()`: error con **acción Copiar informe** al portapapeles,
 *    como `notifyError` de Seal.
 *  - `makeServiceNotification()` / `updateServiceNotificationForQueue()`:
 *    notificación persistente del servicio en primer plano con contador de
 *    descargas activas, como `makeServiceNotification` /
 *    `updateServiceNotificationForPlaylist` de Seal.
 *
 * El ID de notificación del servicio es el 9999 que ya usaba FabiDownloader
 * para su foreground service, de modo que la asignación de la primera descarga
 * activa al ID del servicio (reemplazo transparente del texto base) sigue
 * funcionando sin cambios.
 */
@SuppressLint("StaticFieldLeak")
object NotificationUtil {

    private const val TAG = "NotificationUtil"

    private val PROGRESS_MAX = 100
    private val PROGRESS_INITIAL = 0

    /** Canal de descargas (progreso) — mismo ID que ya usaba FabiDownloader. */
    private val CHANNEL_ID = Config.NOTIF_CHANNEL_PROGRESS

    /** Canal del servicio en primer plano (Seal separa progreso y servicio). */
    private val SERVICE_CHANNEL_ID = Config.NOTIF_CHANNEL_PROGRESS

    /** Grupo de canales (como el `NOTIFICATION_GROUP_ID` de Seal). */
    private val NOTIFICATION_GROUP_ID = Config.NOTIF_GROUP

    /** ID del servicio en primer plano (9999 en FabiDownloader). */
    const val SERVICE_NOTIFICATION_ID = 9999

    /**
     * Marca central que indica que la notificación de progreso de alguna
     * descarga está usando el ID del servicio (9999). Mientras esté activa,
     * `updateServiceNotificationForQueue` no debe sobreescribir ese slot.
     */
    @Volatile
    var isForegroundSlotOwnedByProgress: Boolean = false
        internal set

    private fun context(): Context = com.fabian.downloader.MyApplication.getInstance()

    private fun notificationManager(): NotificationManager =
        context().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // ------------------------------------------------------------------------
    // Canales (estructura de Seal: grupo + canal de descargas + canal de servicio)
    // ------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(context: Context) {
        try {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // Grupo de canales (portado de Seal: NotificationChannelGroup)
            val channelGroup = NotificationChannelGroup(
                NOTIFICATION_GROUP_ID,
                context.getString(R.string.notif_channel_group)
            )
            manager.createNotificationChannelGroup(channelGroup)

            val downloadChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_progress_desc)
                group = NOTIFICATION_GROUP_ID
                setShowBadge(false)
            }

            val statusChannel = NotificationChannel(
                Config.NOTIF_CHANNEL_STATUS,
                context.getString(R.string.notif_channel_status),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_status_desc)
                group = NOTIFICATION_GROUP_ID
                setShowBadge(true)
            }

            val failedChannel = NotificationChannel(
                Config.NOTIF_CHANNEL_FAILED,
                context.getString(R.string.notif_channel_failed),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_failed_desc)
                group = NOTIFICATION_GROUP_ID
                setShowBadge(true)
            }

            manager.createNotificationChannel(downloadChannel)
            manager.createNotificationChannel(statusChannel)
            manager.createNotificationChannel(failedChannel)
        } catch (_: Throwable) {
        }
    }

    // ------------------------------------------------------------------------
    // Acción Cancelar sobre la notificación de progreso (UX clave de Seal)
    // ------------------------------------------------------------------------

    /**
     * PendingIntent de difusión hacia [DownloadActionReceiver] con la acción
     * de cancelar por taskId, como hace Seal con `NotificationActionReceiver`:
     * el botón "Cancelar" de la notificación mata el proceso yt-dlp asociado
     * (`destroyProcessById(taskId)`) y borra la descarga de la cola.
     */
    fun makeCancelTaskPendingIntent(
        context: Context,
        taskId: String,
        downloadId: Long,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, DownloadActionReceiver::class.java).apply {
            setClass(context, DownloadActionReceiver::class.java)
            component = ComponentName(context, DownloadActionReceiver::class.java)
            setPackage(context.packageName)
            action = Config.ACTION_CANCEL_TASK
            putExtra(Config.EXTRA_TASK_ID, taskId)
            putExtra(Config.EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(Config.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId + 600000,
            intent,
            PendingIntent.FLAG_ONE_SHOT or
                PendingIntent.FLAG_IMMUTABLE or
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * PendingIntent de "Copiar informe de error" (como `notifyError` de Seal):
     * copia el informe al portapapeles y descarta la notificación.
     */
    fun makeCopyErrorReportPendingIntent(
        context: Context,
        report: String,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, DownloadActionReceiver::class.java).apply {
            setClass(context, DownloadActionReceiver::class.java)
            component = ComponentName(context, DownloadActionReceiver::class.java)
            setPackage(context.packageName)
            action = Config.ACTION_ERROR_REPORT
            putExtra(Config.EXTRA_ERROR_REPORT, report)
            putExtra(Config.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId + 700000,
            intent,
            PendingIntent.FLAG_ONE_SHOT or
                PendingIntent.FLAG_IMMUTABLE or
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // ------------------------------------------------------------------------
    // Notificación de progreso (estructura de `notifyProgress` de Seal)
    // ------------------------------------------------------------------------

    /**
     * Notificación de progreso con acción Cancelar, como la de Seal:
     * progreso 0-100 (indeterminado si progress <= 0), texto ampliado y
     * botón "Cancelar" ligado al taskId del proceso yt-dlp.
     */
    fun notifyProgress(
        title: String,
        notificationId: Int,
        progress: Int = PROGRESS_INITIAL,
        taskId: String? = null,
        downloadId: Long = -1L,
        text: String? = null,
        largeIcon: android.graphics.Bitmap? = null,
        contentIntent: PendingIntent? = null,
    ) {
        val context = context()
        val cancelPendingIntent = taskId?.let {
            makeCancelTaskPendingIntent(context, it, downloadId, notificationId)
        }

        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(largeIcon)
            .setProgress(PROGRESS_MAX, progress, progress <= 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .apply {
                contentIntent?.let { setContentIntent(it) }
                cancelPendingIntent?.let {
                    addAction(
                        R.drawable.ic_cloud_download,
                        context.getString(R.string.notif_action_cancel),
                        it
                    )
                }
            }
            .run { notificationManager().notify(notificationId, build()) }
    }

    // ------------------------------------------------------------------------
    // Notificación de fin de descarga (estructura de `finishNotification` de Seal)
    // ------------------------------------------------------------------------

    fun finishNotification(
        notificationId: Int,
        title: String? = null,
        text: String? = null,
        intent: PendingIntent? = null,
        largeIcon: android.graphics.Bitmap? = null,
    ) {
        val context = context()
        notificationManager().cancel(notificationId)

        val builder = NotificationCompat.Builder(context, Config.NOTIF_CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setContentText(text)
            .setLargeIcon(largeIcon)
            .setOngoing(false)
            .setAutoCancel(true)
        title?.let { builder.setContentTitle(it) }
        intent?.let { builder.setContentIntent(it) }
        notificationManager().notify(notificationId, builder.build())
    }

    // ------------------------------------------------------------------------
    // Notificación de error con acción "Copiar informe" (estructura de `notifyError` de Seal)
    // ------------------------------------------------------------------------

    fun notifyError(
        title: String,
        notificationId: Int,
        report: String,
        largeIcon: android.graphics.Bitmap? = null,
    ) {
        val context = context()
        val pendingIntent =
            makeCopyErrorReportPendingIntent(context, report, notificationId)

        NotificationCompat.Builder(context, Config.NOTIF_CHANNEL_FAILED)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_title_failed))
            .setLargeIcon(largeIcon)
            .setOngoing(false)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(report))
            .addAction(
                R.drawable.ic_cloud_download,
                context.getString(R.string.notif_action_copy_report),
                pendingIntent
            )
            .run {
                notificationManager().cancel(notificationId)
                notificationManager().notify(notificationId, build())
            }
    }

    // ------------------------------------------------------------------------
    // Notificación del servicio en primer plano (estructura de `makeServiceNotification`
    // y `updateServiceNotificationForPlaylist` de Seal)
    // ------------------------------------------------------------------------

    /** Intent que abre la app en la pestaña de descargas (como el de Seal apunta a su HomePage). */
    private fun makeAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            setClass(context, MainActivity::class.java)
            component = ComponentName(context, MainActivity::class.java)
            setPackage(context.packageName)
            setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
            putExtra(Config.EXTRA_INITIAL_PAGE, 1)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Notificación base del servicio en primer plano (como `makeServiceNotification` de Seal). */
    fun makeServiceNotification(context: Context, text: String? = null): Notification {
        val channelIdToUse =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) SERVICE_CHANNEL_ID else "default"
        return NotificationCompat.Builder(context, channelIdToUse)
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text ?: context.getString(R.string.notif_foreground_service))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(
                makeAppPendingIntent(context, SERVICE_NOTIFICATION_ID)
            )
            .build()
    }

    /**
     * Actualiza la notificación del servicio con el contador de descargas activas,
     * como `updateServiceNotificationForPlaylist(index, itemCount)` de Seal muestra
     * "Título (n/total)". No sobreescribe el slot si una notificación de progreso
     * de una descarga está usando el ID del servicio (mapeo de FabiDownloader).
     */
    fun updateServiceNotificationForQueue(activeCount: Int) {
        try {
            if (!DownloadForegroundService.isRunning) return
            if (activeCount <= 0) return
            // Si una descarga activa reclama el ID del servicio con su progreso,
            // no sobreescribir: el usuario ya ve el progreso real.
            if (isForegroundSlotOwnedByProgress) return
            val context = context()
            val text = context.getString(R.string.notif_queue_count, activeCount)
            val notification = makeServiceNotification(context, text)
            notificationManager().notify(SERVICE_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo actualizar la notificación de la cola: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------
    // Utilidades (estructura de Seal)
    // ------------------------------------------------------------------------

    fun cancelNotification(notificationId: Int) {
        try {
            notificationManager().cancel(notificationId)
        } catch (_: Exception) {
        }
    }

    fun cancelAllNotifications() {
        try {
            notificationManager().cancelAll()
        } catch (_: Exception) {
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT <= 24) true
        else try {
            notificationManager().areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
    }
}
