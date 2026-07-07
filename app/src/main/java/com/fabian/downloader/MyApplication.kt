package com.fabian.downloader

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyApplication : Application() {
    companion object {
        private var instance: MyApplication? = null
        fun getInstance(): MyApplication {
            return instance ?: throw IllegalStateException("MyApplication not initialized yet")
        }
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false
    private val initLatch = java.util.concurrent.CountDownLatch(1)

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        applicationScope.launch {
            try {
                // Revertir actualizaciones incompatibles de yt-dlp (compatibilidad Python 3.10 vs Python 3.8 local)
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val hasResetYtdlp = prefs.getBoolean("reset_ytdlp_python310_v10", false)
                if (!hasResetYtdlp) {
                    try {
                        val noBackupDir = this@MyApplication.noBackupFilesDir
                        if (noBackupDir != null) {
                            val ytdlDir = java.io.File(noBackupDir, "youtubedl-android")
                            if (ytdlDir.exists()) {
                                ytdlDir.deleteRecursively()
                                Log.d("yt-dlp", "Directorio de yt-dlp eliminado para forzar re-extracción limpia compatible")
                            }
                        }
                        prefs.edit { putBoolean("reset_ytdlp_python310_v10", true) }
                    } catch (e: Exception) {
                        Log.e("yt-dlp", "Error al intentar resetear directorio de yt-dlp", e)
                    }
                }

                YoutubeDL.getInstance().init(this@MyApplication)
                FFmpeg.getInstance().init(this@MyApplication)
                
                isInitialized = true
                initLatch.countDown()
                Log.d("yt-dlp", "Inicialización exitosa de componentes nativos")
                
                try {
                    val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val isConnected = capabilities != null && (
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
                    
                    if (isConnected) {
                        Log.d("yt-dlp", "Iniciando actualización de yt-dlp al arranque...")
                        YoutubeDL.getInstance().updateYoutubeDL(this@MyApplication)
                        Log.d("yt-dlp", "Actualización de yt-dlp exitosa")
                    } else {
                        Log.d("yt-dlp", "Sin conexión de red, se omite la actualización de yt-dlp al inicio")
                    }
                } catch (e: Exception) {
                    Log.e("yt-dlp", "Fallo al actualizar yt-dlp (normal si no hay red o ya está actualizado)", e)
                }
            } catch (e: Exception) {
                Log.e("yt-dlp", "Error crítico al inicializar binarios nativos", e)
                isInitialized = true
                initLatch.countDown() // Release even on error
            }
        }
    }

    fun waitForInitialization() {
        if (!isInitialized) {
            initLatch.await(30, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
}
