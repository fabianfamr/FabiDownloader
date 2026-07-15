package com.fabian.downloader

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import com.fabian.downloader.utils.Config
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class MyApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
    companion object {
        private var instance: MyApplication? = null
        fun getInstance(): MyApplication {
            return instance ?: throw IllegalStateException("MyApplication not initialized yet")
        }
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false
    private val initLatch = java.util.concurrent.CountDownLatch(1)

    var isAppInForeground = false
        private set

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("fabi_downloader_prefs", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "Sistema") ?: "Sistema"
        if (lang != "Sistema") {
            val locale = if (lang == "English") java.util.Locale("en") else java.util.Locale("es")
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.fabian.downloader.ui.AppSettings.init(this)
        
        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    isAppInForeground = true
                }
            }
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    isAppInForeground = false
                }
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
        
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
                                Log.d(Config.TAG_YT_DLP, "Directorio de yt-dlp eliminado para forzar re-extracción limpia compatible")
                            }
                        }
                        prefs.edit { putBoolean("reset_ytdlp_python310_v10", true) }
                    } catch (e: Exception) {
                        Log.e(Config.TAG_YT_DLP, "Error al intentar resetear directorio de yt-dlp", e)
                    }
                }

                YoutubeDL.getInstance().init(this@MyApplication)
                FFmpeg.getInstance().init(this@MyApplication)
                
                isInitialized = true
                initLatch.countDown()
                Log.d(Config.TAG_YT_DLP, "Inicialización exitosa de componentes nativos")
                
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
                        Log.d(Config.TAG_YT_DLP, "Iniciando actualización de yt-dlp al arranque...")
                        YoutubeDL.getInstance().updateYoutubeDL(this@MyApplication)
                        Log.d(Config.TAG_YT_DLP, "Actualización de yt-dlp exitosa")
                    } else {
                        Log.d(Config.TAG_YT_DLP, "Sin conexión de red, se omite la actualización de yt-dlp al inicio")
                    }
                } catch (e: Exception) {
                    Log.e(Config.TAG_YT_DLP, "Fallo al actualizar yt-dlp (normal si no hay red o ya está actualizado)", e)
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_YT_DLP, "Error crítico al inicializar binarios nativos", e)
                isInitialized = true
                initLatch.countDown() // Release even on error
            }
        }
    }

    fun waitForInitialization() {
        if (!isInitialized) {
            val started = initLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)
            if (!started) {
                android.util.Log.w(Config.TAG_YT_DLP, "waitForInitialization timed out after 10s - proceeding anyway")
            }
        }
    }
}
