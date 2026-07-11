package com.fabian.downloader.utils

import android.util.Log
import okhttp3.OkHttpClient
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
    private val client = OkHttpClient()
    private val GITHUB_API_URL = Config.GITHUB_API_LATEST_RELEASE

    suspend fun checkForUpdates(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val json = JSONObject(body)
                val tagName = json.getString("tag_name").replace("v", "").trim()
                val htmlUrl = json.getString("html_url")
                val bodyText = json.optString("body", "")
                
                Result.success(UpdateInfo(tagName, htmlUrl, bodyText))
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error checking for updates", e)
            Result.failure(e)
        }
    }

    /**
     * Compares two version strings.
     * Returns true if v1 > v2.
     */
    fun isNewerVersion(latest: String, current: String): Boolean {
        val v1Parts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val v2Parts = current.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until length) {
            val v1 = v1Parts.getOrNull(i) ?: 0
            val v2 = v2Parts.getOrNull(i) ?: 0
            if (v1 > v2) return true
            if (v1 < v2) return false
        }
        return false
    }
}
