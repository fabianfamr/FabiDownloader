package com.fabian.downloader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.fabian.downloader.MyApplication

class ConnectionService {

    companion object {
        @Volatile
        private var lastSocketCheckTime = 0L
        @Volatile
        private var lastSocketCheckResult = false
    }

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastSocketCheckTime < 3000L) {
                return@withContext lastSocketCheckResult
            }

            val context = MyApplication.getInstance()
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = if (network != null) connectivityManager.getNetworkCapabilities(network) else null
            
            val hasInternetCap = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

            if (hasInternetCap && isValidated) {
                lastSocketCheckTime = now
                lastSocketCheckResult = true
                return@withContext true
            }

            // Fallback: comprobación directa de socket para redes sin validación inmediata, VPNs,
            // o fallos en activeNetwork de MIUI.
            val res = try {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress("1.1.1.1", 80), 2000)
                }
                true
            } catch (_: Exception) {
                try {
                    java.net.Socket().use { socket ->
                        socket.connect(java.net.InetSocketAddress("8.8.8.8", 53), 2000)
                    }
                    true
                } catch (_: Exception) {
                    hasInternetCap
                }
            }
            lastSocketCheckTime = now
            lastSocketCheckResult = res
            res
        } catch (e: Exception) {
            // Último recurso en caso de excepción del sistema
            true 
        }
    }
}
