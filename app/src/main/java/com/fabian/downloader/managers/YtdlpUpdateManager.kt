package com.fabian.downloader.managers

import android.content.Context
import android.util.Log
import com.fabian.downloader.MyApplication
import com.fabian.downloader.configs.Config
import com.fabian.downloader.network.NetworkClient
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

data class YtdlpVersionInfo(
    val currentVersion: String,
    val latestVersion: String,
    val publishedDate: String,
    val releaseNotes: String,
    val hasUpdate: Boolean
)

object YtdlpUpdateManager {
    private const val TAG = "YtdlpUpdateManager"
    private const val YTDLP_GITHUB_API = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
    private val client = NetworkClient.okHttpClient

    fun getLocalVersion(context: Context): String {
        return try {
            YoutubeDL.getInstance().version(context) ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local yt-dlp version", e)
            "Unknown"
        }
    }

    suspend fun checkYtdlpUpdate(context: Context): Result<YtdlpVersionInfo> = withContext(Dispatchers.IO) {
        try {
            val localVer = getLocalVersion(context)

            val request = Request.Builder()
                .url(YTDLP_GITHUB_API)
                .header("User-Agent", Config.UA_DESKTOP)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Respuesta vacía de GitHub"))
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").replace("v", "").trim()
                val publishedAt = json.optString("published_at", "").take(10)
                val bodyNotes = json.optString("body", "").trim()

                val hasUpdate = isNewerVersion(tagName, localVer)

                Result.success(
                    YtdlpVersionInfo(
                        currentVersion = localVer,
                        latestVersion = if (tagName.isNotEmpty()) tagName else localVer,
                        publishedDate = publishedAt,
                        releaseNotes = bodyNotes,
                        hasUpdate = hasUpdate
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comprobando actualizaciones de yt-dlp", e)
            Result.failure(e)
        }
    }

    suspend fun updateYtdlp(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Iniciando descarga e instalación de actualización de yt-dlp...")
            val result = YoutubeDL.getInstance().updateYoutubeDL(context)
            val newVer = getLocalVersion(context)
            Log.i(TAG, "yt-dlp actualizado con éxito a la versión: $newVer (Resultado: $result)")
            Result.success(newVer)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando binario de yt-dlp", e)
            Result.failure(e)
        }
    }

    suspend fun resetEngine(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.w(TAG, "Restableciendo motor yt-dlp a la versión de los assets...")
            val success = MyApplication.getInstance().resetAndReinitYtdlp(context)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Fallo al re-extraer assets"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restableciendo motor yt-dlp", e)
            Result.failure(e)
        }
    }

    private const val PREF_LAST_AUTO_UPDATE_CHECK = "pref_last_ytdlp_update_check"
    private const val AUTO_UPDATE_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 horas para preservar ancho de banda

    /**
     * Dispara una verificación inteligente y silenciosa del binario yt-dlp en segundo plano.
     * Solo descarga el binario si GitHub realmente tiene una versión más reciente,
     * evitando descargas repetitivas de ~25MB que saturan la banda Wi-Fi.
     */
    suspend fun autoUpdateSilentlyOnFailure(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong(PREF_LAST_AUTO_UPDATE_CHECK, 0L)
            val now = System.currentTimeMillis()

            // Protección estricta de ancho de banda: no verificar más de una vez cada 24 horas
            if (now - lastCheck < AUTO_UPDATE_COOLDOWN_MS) {
                Log.d(TAG, "Auto-actualización omitida para proteger ancho de banda Wi-Fi (última comprobación hace menos de 24h)")
                return@withContext false
            }

            // Consultar únicamente metadatos ligeros de la API de GitHub (~1KB de datos)
            Log.i(TAG, "Comprobando versión en GitHub antes de descargar binario para ahorrar ancho de banda Wi-Fi...")
            val checkResult = checkYtdlpUpdate(context)
            prefs.edit().putLong(PREF_LAST_AUTO_UPDATE_CHECK, now).apply()

            if (checkResult.isSuccess) {
                val info = checkResult.getOrNull()
                if (info != null && info.hasUpdate) {
                    Log.i(TAG, "Nueva versión detectada en GitHub (${info.latestVersion} vs local ${info.currentVersion}). Descargando actualización...")
                    val appCtx = MyApplication.getInstance()
                    appCtx.forceUpdateYtdlpBinary(context, ignoreThrottle = true)
                } else {
                    Log.i(TAG, "yt-dlp ya está en la versión más reciente (${info?.currentVersion ?: "ok"}). Descarga de 25MB omitida para ahorrar Wi-Fi.")
                    false
                }
            } else {
                Log.w(TAG, "No se pudo comprobar la versión en GitHub. Omitiendo descarga pesada para no saturar la red.")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en auto-actualización silenciosa de yt-dlp: ${e.message}")
            false
        }
    }

    fun isNewerVersion(latest: String, current: String): Boolean {
        if (current == "Unknown" || current.isEmpty()) return true
        if (latest.isEmpty()) return false
        if (latest == current) return false

        val cleanLatest = latest.replace(".", "").replace("-", "").trim()
        val cleanCurrent = current.replace(".", "").replace("-", "").trim()

        val latestLong = cleanLatest.toLongOrNull()
        val currentLong = cleanCurrent.toLongOrNull()

        if (latestLong != null && currentLong != null) {
            return latestLong > currentLong
        }

        val lParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(lParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val lVal = lParts.getOrNull(i) ?: 0
            val cVal = cParts.getOrNull(i) ?: 0
            if (lVal > cVal) return true
            if (lVal < cVal) return false
        }
        return false
    }
}
