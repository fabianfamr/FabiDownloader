package com.fabian.downloader.managers

import android.content.Context
import android.util.Log
import com.fabian.downloader.MyApplication
import com.fabian.downloader.configs.Config
import com.fabian.downloader.network.NetworkClient
import com.fabian.downloader.utils.VersionUtils
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

    /** Cooldown exclusivo del auto-update (24h). Ya NO compartido con el manual. */
    private const val PREF_LAST_AUTO_UPDATE_CHECK = "pref_last_ytdlp_auto_check"
    private const val AUTO_UPDATE_COOLDOWN_MS = 24L * 60 * 60 * 1000

    fun getLocalVersion(context: Context): String {
        return try {
            YoutubeDL.getInstance().version(context) ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local yt-dlp version", e)
            "Unknown"
        }
    }

    suspend fun checkYtdlpUpdate(context: Context): Result<YtdlpVersionInfo> =
        withContext(Dispatchers.IO) {
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

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(
                            Exception("Respuesta vacía de GitHub")
                        )
                    val json = JSONObject(body)
                    // removePrefix en lugar de replace("v", ""): antes "v1.2.3-video"
                    // se convertía en "1.2.3-ideo".
                    val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                    val publishedAt = json.optString("published_at", "").take(10)
                    val bodyNotes = json.optString("body", "").trim()

                    val hasUpdate = VersionUtils.isNewer(tagName, localVer)

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

    /**
     * Dispara una verificación inteligente y silenciosa del binario yt-dlp en segundo plano.
     *
     * Solo descarga el binario si GitHub realmente tiene una versión más reciente,
     * evitando descargas repetitivas de ~25MB.
     *
     * IMPORTANTE: el cooldown de 24h solo se actualiza si la verificación con GitHub
     * fue exitosa. Si GitHub está caído o no hay red, no se bloquea el auto-update
     * por 24h — se reintenta más tarde.
     */
    suspend fun autoUpdateSilentlyOnFailure(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences(
                    Config.PREFS_NAME, Context.MODE_PRIVATE
                )
                val lastCheck = prefs.getLong(PREF_LAST_AUTO_UPDATE_CHECK, 0L)
                val now = System.currentTimeMillis()

                if (now - lastCheck < AUTO_UPDATE_COOLDOWN_MS) {
                    Log.d(TAG, "Auto-actualización omitida (cooldown activo, " +
                        "última comprobación hace ${(now - lastCheck) / 1000}s)")
                    return@withContext false
                }

                Log.i(TAG, "Comprobando versión en GitHub antes de descargar binario...")
                val checkResult = checkYtdlpUpdate(context)

                // Solo actualizar cooldown si la verificación fue exitosa.
                // Antes se actualizaba siempre, lo que bloqueaba el auto-update
                // durante 24h incluso si GitHub estaba caído.
                if (checkResult.isSuccess) {
                    prefs.edit().putLong(PREF_LAST_AUTO_UPDATE_CHECK, now).apply()
                } else {
                    Log.w(TAG,
                        "Verificación falló (${checkResult.exceptionOrNull()?.message}). " +
                            "Cooldown NO actualizado; se reintentará pronto.")
                    return@withContext false
                }

                val info = checkResult.getOrNull()
                if (info != null && info.hasUpdate) {
                    Log.i(TAG,
                        "Nueva versión detectada en GitHub (${info.latestVersion} " +
                            "vs local ${info.currentVersion}). Descargando actualización...")
                    val appCtx = MyApplication.getInstance()
                    appCtx.forceUpdateYtdlpBinary(
                        context = context,
                        ignoreThrottle = true,
                        forcedSource = "auto"
                    )
                } else {
                    Log.i(TAG,
                        "yt-dlp ya está en la versión más reciente " +
                            "(${info?.currentVersion ?: "ok"}). Descarga omitida.")
                    false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error en auto-actualización silenciosa de yt-dlp: ${e.message}")
                false
            }
        }
}
