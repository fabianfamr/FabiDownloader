package com.fabian.downloader.managers

import android.util.Log
import com.fabian.downloader.BuildConfig
import com.fabian.downloader.configs.Config
import com.fabian.downloader.network.NetworkClient
import com.fabian.downloader.utils.VersionUtils
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private val client = NetworkClient.okHttpClient
    private val GITHUB_API_URL = Config.GITHUB_API_LATEST_RELEASE

    suspend fun checkForUpdates(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header(
                    "User-Agent",
                    "FabiDownloader/${BuildConfig.VERSION_NAME} (${Config.GITHUB_URL})"
                )
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response body"))
                val json = JSONObject(body)
                // removePrefix en lugar de replace("v", ""): evita corromper tags
                // tipo "v1.2.3-video" → antes quedaba "1.2.3-ideo".
                val tagName = json.getString("tag_name").removePrefix("v").trim()
                val releaseUrl = json.getString("html_url")
                val bodyText = json.optString("body", "")

                // Buscar el APK en los assets. Antes se devolvía `html_url`
                // (la página web del release), no un enlace directo al APK.
                val apkUrl = runCatching {
                    val assets = json.optJSONArray("assets")
                    (0 until (assets?.length() ?: 0))
                        .asSequence()
                        .map { assets!!.getJSONObject(it) }
                        .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                        ?.optString("browser_download_url")
                }.getOrNull() ?: releaseUrl

                Result.success(UpdateInfo(tagName, apkUrl, bodyText))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            Result.failure(e)
        }
    }

    /**
     * Compara dos versiones semánticas. Delegado a [VersionUtils] para evitar
     * duplicación con `YtdlpUpdateManager.isNewerVersion`.
     */
    fun isNewerVersion(latest: String, current: String): Boolean =
        VersionUtils.isNewer(latest, current)
}
