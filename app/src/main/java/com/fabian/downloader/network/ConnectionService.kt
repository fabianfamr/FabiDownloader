package com.fabian.downloader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ConnectionService {
    
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val timeoutMs = 2500
            val connection = URL(com.fabian.downloader.utils.Config.PING_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            responseCode in 200..399
        } catch (e: Exception) {
            false
        }
    }
}
