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
