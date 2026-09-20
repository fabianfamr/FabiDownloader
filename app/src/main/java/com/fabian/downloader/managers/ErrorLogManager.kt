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

    private const val TAG = "ErrorLogManager"
    private const val LOG_FILE_NAME = "fabi_app_errors.log"
    private const val MAX_LOG_SIZE_BYTES = 200 * 1024 // 200 KB max

    /**
     * Inicializa el gestor de logs.
     *
     * Ya NO instala aquí el `UncaughtExceptionHandler` — ese se instala
     * centralizadamente en `MyApplication.onCreate()` para evitar duplicados.
     * Esta función queda como punto de entrada para futuras inicializaciones
     * (carga de logs previos, rotación, etc.).
     */
    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        // No-op: el handler se instala en MyApplication.onCreate().
        // Mantenemos la firma para compatibilidad con llamadas existentes.
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
            Log.e(TAG, "Failed to write error log", e)
        }
    }

    /**
     * Sanitiza una línea de log antes de copiarla al clipboard o exportarla.
     * Oculta tokens, cookies, passwords y URLs con tokens de sesión.
     */
    private fun sanitizeForClipboard(line: String): String {
        var sanitized = line
        // Tokens/cookies/auth/keys: `token=abc`, `Cookie: session=xyz`
        sanitized = Regex("(?i)(token|cookie|auth|key|password|secret|session)=[^\\s&]+",
            RegexOption.IGNORE_CASE)
            .replace(sanitized) { m ->
                val key = m.groupValues[1]
                "$key=***REDACTED***"
            }
        // Authorization: Bearer xxx
        sanitized = Regex("(?i)(authorization:\\s*)(bearer\\s+|basic\\s+)[^\\s]+",
            RegexOption.IGNORE_CASE)
            .replace(sanitized) { "${it.groupValues[1]}***REDACTED***" }
        // URLs con parámetros de sesión: ?token=xxx, ?session=yyy
        sanitized = Regex("(https?://[^\\s]+[?&](token|session|sid|auth)=[^\\s&]+)",
            RegexOption.IGNORE_CASE)
            .replace(sanitized) { "[URL_REDACTED]" }
        return sanitized
    }

    suspend fun getFormattedLogs(context: Context): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("FABI DOWNLOADER - REGISTRO DE ERRORES\n")
        sb.append("========================================\n")
        sb.append("Fecha del informe: ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        }\n")
        sb.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        sb.append("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} " +
            "(Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})\n")
        sb.append("========================================\n\n")

        // 1. Registros de archivo interno de errores
        sb.append("--- [1] ARCHIVO DE ERRORES REGISTRADOS DE LA APP ---\n")
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        if (logFile.exists() && logFile.length() > 0) {
            try {
                val lines = logFile.readLines()
                val tailLines = if (lines.size > 200) lines.takeLast(200) else lines
                sb.append(tailLines.joinToString("\n").lineSequence()
                    .map(::sanitizeForClipboard).joinToString("\n"))
                    .append("\n\n")
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
                    it.size.startsWith(Config.STATUS_FAILED_PREFIX) ||
                    it.progress < 0
            }.take(50)
            if (failedDownloads.isNotEmpty()) {
                failedDownloads.forEach { rec ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(rec.timestamp))
                    sb.append("• ID: ${rec.id} | Fecha: $dateStr\n")
                    sb.append("  Título/Estado: ${rec.title}\n")
                    sb.append("  URL: ${sanitizeForClipboard(rec.url)}\n")
                    sb.append("  Formato: ${rec.format} | Calidad: ${rec.quality} " +
                        "| Velocidad/Error: ${rec.speed}\n")
                    sb.append("  --------------------------------------\n")
                }
                sb.append("\n")
            } else {
                sb.append("No hay descargas en estado de error en la base de datos.\n\n")
            }
        } catch (e: Exception) {
            sb.append("Error al obtener descargas fallidas de la base de datos: ${e.message}\n\n")
        }

        // 3. Estado de descargas en curso o en cola
        sb.append("--- [3] ESTADO DE DESCARGAS ACTIVAS / EN COLA ---\n")
        try {
            val db = AppDatabase.getInstance(context)
            val allDownloads = db.downloadDao().getAllDownloadsDirect()
            val activeDownloads = allDownloads.filter {
                !it.isCompleted && !it.isPaused && it.speed != "FAILED" &&
                    !it.title.startsWith(Config.STATUS_FAILED_PREFIX)
            }
            if (activeDownloads.isNotEmpty()) {
                activeDownloads.forEach { rec ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(rec.timestamp))
                    sb.append("• ID: ${rec.id} | Fecha: $dateStr\n")
                    sb.append("  Título: ${rec.title}\n")
                    sb.append("  URL: ${sanitizeForClipboard(rec.url)}\n")
                    sb.append("  Progreso: ${rec.progress}% | Tamaño: ${rec.size} " +
                        "| Estado: ${rec.speed}\n")
                    sb.append("  --------------------------------------\n")
                }
                sb.append("\n")
            } else {
                sb.append("No hay descargas activas ni en cola actualmente.\n\n")
            }
        } catch (e: Exception) {
            sb.append("Error al obtener descargas activas: ${e.message}\n\n")
        }

        // 4. Extracto de Logcat (errores recientes del sistema/app)
        sb.append("--- [4] REGISTRO DE ERRORES DEL SISTEMA (LOGCAT *:E) ---\n")
        try {
            // Desde Android 13+ las apps solo pueden leer sus propias líneas
            // sin permiso READ_LOGS. Filtramos por PID propio con --pid.
            val pid = android.os.Process.myPid().toString()
            val cmd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf("logcat", "-d", "-v", "threadtime", "--pid=$pid", "*:E")
            } else {
                arrayOf("logcat", "-d", "-v", "threadtime", "*:E")
            }
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logcatLines = mutableListOf<String>()
            var line: String?
            val appPid = pid
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: break
                // Filtro adicional para pre-S: solo líneas propias
                val isOwn = l.contains(appPid) ||
                    l.contains("com.fabian.downloader") ||
                    l.contains("youtubedl") ||
                    l.contains("yt-dlp")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || isOwn) {
                    logcatLines.add(sanitizeForClipboard(l))
                }
            }
            reader.close()
            process.waitFor()
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
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
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
            Log.e(TAG, "Failed to clear error log file", e)
        }
    }
}
