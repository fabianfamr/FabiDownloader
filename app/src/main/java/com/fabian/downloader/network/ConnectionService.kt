package com.fabian.downloader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.fabian.downloader.MyApplication

class ConnectionService {

    companion object {
        // Caché estática compartida entre TODAS las instancias (se crean varias por descarga).
        // Antes era por-instancia y nunca acertaba, ejecutando un socket check por cada llamada.
        @Volatile
        private var lastSocketCheckTime = 0L
        @Volatile
        private var lastSocketCheckResult = false
    }

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = MyApplication.getInstance()
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return@withContext false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withContext false
            
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet) return@withContext false
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return@withContext true

            val now = System.currentTimeMillis()
            if (now - lastSocketCheckTime < 3000L) {
                return@withContext lastSocketCheckResult
            }

            // Fallback: comprobación directa de socket para redes sin validación inmediata o detrás de VPN
            val res = try {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress("1.1.1.1", 80), 1000)
                }
                true
            } catch (_: Exception) {
                try {
                    java.net.Socket().use { socket ->
                        socket.connect(java.net.InetSocketAddress("8.8.8.8", 53), 1000)
                    }
                    true
                } catch (_: Exception) {
                    hasInternet
                }
            }
            lastSocketCheckTime = now
            lastSocketCheckResult = res
            res
        } catch (e: Exception) {
            false
        }
    }
}
