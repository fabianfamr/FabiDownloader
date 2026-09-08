package com.fabian.downloader

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import com.fabian.downloader.configs.Config
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val applicationScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private var isInitialized = false
    private val initLatch = java.util.concurrent.CountDownLatch(1)

    var isAppInForeground = false
        private set

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("fabi_downloader_prefs", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "Sistema") ?: "Sistema"
        if (!lang.contains("Sistema")) {
            val locale = if (lang.contains("English")) java.util.Locale.forLanguageTag("en") else java.util.Locale.forLanguageTag("es")
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
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is OutOfMemoryError) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Out of Memory Error detected! Cleaning up...", throwable)
                com.fabian.downloader.services.ExtractionService.clearCaches()
                System.gc()
            }
            com.fabian.downloader.managers.ErrorLogManager.logError(this, "UncaughtException", "Uncaught exception", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        com.fabian.downloader.managers.ErrorLogManager.init(this)
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
                com.fabian.downloader.utils.PathUtils.migrateOldStructureIfNeeded(this@MyApplication)
            } catch (e: Exception) {
                Log.e(Config.TAG_PATH_UTILS, "Error migrating old structure", e)
            }
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
                Log.d(Config.TAG_YT_DLP, "Inicialización exitosa de componentes nativos desde APK assets")
            } catch (e: Exception) {
                Log.e(Config.TAG_YT_DLP, "Error crítico al inicializar binarios nativos", e)
                isInitialized = true
                initLatch.countDown() // Release even on error
            }
        }
    }

    private var lastForceUpdateTimestamp = 0L
    // Lock para impedir actualizaciones concurrentes del binario (varias extracciones en paralelo)
    private val ytdlpUpdateLock = java.util.concurrent.atomic.AtomicBoolean(false)
    // Lock para impedir resets concurrentes del directorio de yt-dlp
    private val ytdlpResetLock = java.util.concurrent.atomic.AtomicBoolean(false)

    fun resetAndReinitYtdlp(context: android.content.Context): Boolean {
        if (!ytdlpResetLock.compareAndSet(false, true)) {
            Log.d(Config.TAG_YT_DLP, "Reset de yt-dlp omitido (ya en curso)")
            return false
        }
        return try {
            Log.w(Config.TAG_YT_DLP, "Resetting corrupt yt-dlp directory and re-initializing from APK assets...")
            val dirsToClean = listOfNotNull(context.noBackupFilesDir, context.filesDir, context.cacheDir)
            for (parentDir in dirsToClean) {
                val ytdlDir = java.io.File(parentDir, "youtubedl-android")
                if (ytdlDir.exists()) {
                    ytdlDir.deleteRecursively()
                }
            }
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            Log.i(Config.TAG_YT_DLP, "yt-dlp directory cleanly re-initialized from APK assets")
            true
        } catch (e: Exception) {
            Log.e(Config.TAG_YT_DLP, "Error resetting and re-initializing yt-dlp", e)
            false
        } finally {
            ytdlpResetLock.set(false)
        }
    }

    fun forceUpdateYtdlpBinary(context: android.content.Context, ignoreThrottle: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(Config.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong("pref_last_ytdlp_update_check", 0L)

        // Protección de ancho de banda: mínimo 24 horas entre descargas de binario (25MB)
        if (!ignoreThrottle && (now - lastCheck < 24 * 60 * 60 * 1000L || now - lastForceUpdateTimestamp < 24 * 60 * 60 * 1000L)) {
            Log.d(Config.TAG_YT_DLP, "Actualización de yt-dlp omitida para proteger ancho de banda Wi-Fi (menos de 24h)")
            return false
        }
        // Si otra corrutina ya está actualizando, no duplicar el trabajo
        if (!ytdlpUpdateLock.compareAndSet(false, true)) {
            Log.d(Config.TAG_YT_DLP, "Actualización de yt-dlp omitida (ya en curso)")
            return false
        }
        lastForceUpdateTimestamp = now
        prefs.edit().putLong("pref_last_ytdlp_update_check", now).apply()

        return try {
            Log.i(Config.TAG_YT_DLP, "Descargando actualización de binario yt-dlp...")
            val updateResult = YoutubeDL.getInstance().updateYoutubeDL(context)
            Log.i(Config.TAG_YT_DLP, "Binario de yt-dlp actualizado exitosamente: $updateResult")
            true
        } catch (e: Exception) {
            Log.e(Config.TAG_YT_DLP, "Error al actualizar binario yt-dlp", e)
            false
        } finally {
            ytdlpUpdateLock.set(false)
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
