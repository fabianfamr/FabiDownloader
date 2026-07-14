package com.fabian.downloader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.fabian.downloader.MyApplication

class ConnectionService {
    
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = MyApplication.getInstance()
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return@withContext false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withContext false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }
}
