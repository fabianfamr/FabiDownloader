package com.fabian.downloader.managers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.fabian.downloader.BuildConfig
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorLogManager {

    private const val LOG_FILE_NAME = "fabi_app_errors.log"
    private const val MAX_LOG_SIZE_BYTES = 200 * 1024 // 200 KB max

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logError(context, "UncaughtException", "Crash in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    fun logError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
                try {
                    val lines = logFile.readLines()
                    if (lines.size > 500) {
                        val trimmedLines = lines.takeLast(250)
                        logFile.writeText(trimmedLines.joinToString("\n") + "\n")
                    } else {
                        logFile.delete()
                    }
                } catch (e: Exception) {
                    logFile.delete()
                }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val sb = StringBuilder()
            sb.append("[$timestamp] [$tag] $message\n")
            if (throwable != null) {
                sb.append(Log.getStackTraceString(throwable)).append("\n")
            }
            sb.append("----------------------------------------\n")

            logFile.appendText(sb.toString())
        } catch (e: Exception) {
            Log.e("ErrorLogManager", "Failed to write error log", e)
        }
    }

    suspend fun getFormattedLogs(context: Context): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("FABI DOWNLOADER - REGISTRO DE ERRORES\n")
        sb.append("========================================\n")
        sb.append("Fecha del informe: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        sb.append("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})\n")
        sb.append("========================================\n\n")

        // 1. Registros de archivo interno de errores
        sb.append("--- [1] ARCHIVO DE ERRORES REGISTRADOS DE LA APP ---\n")
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        if (logFile.exists() && logFile.length() > 0) {
            try {
                val lines = logFile.readLines()
                val tailLines = if (lines.size > 200) lines.takeLast(200) else lines
                sb.append(tailLines.joinToString("\n")).append("\n\n")
            } catch (e: Exception) {
                sb.append("Error leyendo archivo de logs: ${e.message}\n\n")
            }
        } else {
            sb.append("No hay registros guardados en el archivo de errores de la app.\n\n")
        }

        // 2. Errores de descargas fallidas en la base de datos
        sb.append("--- [2] HISTORIAL DE DESCARGAS FALLIDAS (ROOM DB) ---\n")
        try {
            val db = AppDatabase.getInstance(context)
            val allDownloads = db.downloadDao().getAllDownloadsDirect()
            val failedDownloads = allDownloads.filter { 
                it.title.startsWith(Config.STATUS_FAILED_PREFIX) || 
                it.speed == "FAILED" || 
                (!it.isCompleted && !it.isPaused && it.progress == 0)
            }
            if (failedDownloads.isNotEmpty()) {
                failedDownloads.forEach { rec ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(rec.timestamp))
                    sb.append("• ID: ${rec.id} | Fecha: $dateStr\n")
                    sb.append("  Título/Estado: ${rec.title}\n")
                    sb.append("  URL: ${rec.url}\n")
                    sb.append("  Formato: ${rec.format} | Calidad: ${rec.quality} | Velocidad/Error: ${rec.speed}\n")
                    sb.append("  --------------------------------------\n")
                }
                sb.append("\n")
            } else {
                sb.append("No hay descargas en estado de error en la base de datos.\n\n")
            }
        } catch (e: Exception) {
            sb.append("Error al obtener descargas fallidas de la base de datos: ${e.message}\n\n")
        }

        // 3. Extracto de Logcat (errores recientes del sistema/app)
        sb.append("--- [3] REGISTRO DE ERRORES DEL SISTEMA (LOGCAT *:E) ---\n")
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v threadtime *:E")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logcatLines = mutableListOf<String>()
            var line: String?
            val appPid = android.os.Process.myPid().toString()
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: break
                if (l.contains(appPid) || l.contains("com.fabian.downloader") || l.contains("youtubedl") || l.contains("yt-dlp")) {
                    logcatLines.add(l)
                }
            }
            reader.close()
            if (logcatLines.isNotEmpty()) {
                val lastLines = if (logcatLines.size > 150) logcatLines.takeLast(150) else logcatLines
                sb.append(lastLines.joinToString("\n")).append("\n\n")
            } else {
                sb.append("No se registraron líneas de error de la app en logcat reciente.\n\n")
            }
        } catch (e: Exception) {
            sb.append("No se pudo leer logcat: ${e.message}\n\n")
        }

        sb.toString()
    }

    suspend fun copyErrorsToClipboard(context: Context): Boolean = withContext(Dispatchers.IO) {
        val formattedLogs = getFormattedLogs(context)
        withContext(Dispatchers.Main) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("FabiDownloader_Error_Logs", formattedLogs)
            clipboard.setPrimaryClip(clip)
        }
        true
    }

    fun clearErrorLogs(context: Context) {
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.e("ErrorLogManager", "Failed to clear error log file", e)
        }
    }
}
