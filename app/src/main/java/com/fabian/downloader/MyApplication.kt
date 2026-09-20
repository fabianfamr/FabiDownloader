package com.fabian.downloader

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fabian.downloader.configs.Config
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        private const val TAG = "MyApplication"

        /** Throttle keys separados para auto/manual/forzado. */
        private const val PREF_LAST_YTDLP_AUTO_CHECK = "pref_last_ytdlp_auto_check"
        private const val PREF_LAST_YTDLP_MANUAL_UPDATE = "pref_last_ytdlp_manual_update"
        private const val PREF_LAST_YTDLP_FORCED_UPDATE = "pref_last_ytdlp_forced_update"

        /** Cooldowns por tipo de actualización. */
        private const val COOLDOWN_AUTO_MS = 24L * 60 * 60 * 1000     // 24h
        private const val COOLDOWN_MANUAL_MS = 1L * 60 * 60 * 1000    // 1h
        private const val COOLDOWN_FORCED_MS = 5L * 60 * 1000         // 5min

        private var instance: MyApplication? = null
        fun getInstance(): MyApplication {
            return instance ?: throw IllegalStateException("MyApplication not initialized yet")
        }
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isYoutubeDLReady = false

    private val initLatch = java.util.concurrent.CountDownLatch(1)

    @Volatile
    var isAppInForeground: Boolean = false
        private set

    private var lastForceUpdateTimestamp = 0L

    /** Lock para impedir actualizaciones concurrentes del binario. */
    private val ytdlpUpdateLock = java.util.concurrent.atomic.AtomicBoolean(false)
    /** Lock para impedir resets concurrentes del directorio de yt-dlp. */
    private val ytdlpResetLock = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("fabi_downloader_prefs",
            android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "Sistema") ?: "Sistema"
        if (!lang.contains("Sistema")) {
            val locale = if (lang.contains("English"))
                java.util.Locale.forLanguageTag("en")
            else
                java.util.Locale.forLanguageTag("es")
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

        installUncaughtExceptionHandler()
        com.fabian.downloader.managers.ErrorLogManager.init(this)
        com.fabian.downloader.ui.AppSettings.init(this)
        com.fabian.downloader.services.NotificationService.createAllNotificationChannels(this)
        installForegroundLifecycleObserver()

        // Observador de cambios de red para invalidar el cache de ConnectionService.
        try {
            com.fabian.downloader.network.ConnectionService.installNetworkChangeObserver(this)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo instalar NetworkChangeObserver", e)
        }

        applicationScope.launch {
            try {
                com.fabian.downloader.utils.PathUtils.migrateOldStructureIfNeeded(this@MyApplication)
            } catch (e: Exception) {
                Log.e(Config.TAG_PATH_UTILS, "Error migrating old structure", e)
            }
            try {
                // Revertir actualizaciones incompatibles de yt-dlp
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val hasResetYtdlp = prefs.getBoolean("reset_ytdlp_python310_v10", false)
                if (!hasResetYtdlp) {
                    try {
                        val noBackupDir = this@MyApplication.noBackupFilesDir
                        if (noBackupDir != null) {
                            val ytdlDir = java.io.File(noBackupDir, "youtubedl-android")
                            if (ytdlDir.exists()) {
                                ytdlDir.deleteRecursively()
                                Log.d(Config.TAG_YT_DLP,
                                    "Directorio de yt-dlp eliminado para forzar " +
                                    "re-extracción limpia compatible")
                            }
                        }
                        prefs.edit { putBoolean("reset_ytdlp_python310_v10", true) }
                    } catch (e: Exception) {
                        Log.e(Config.TAG_YT_DLP,
                            "Error al intentar resetear directorio de yt-dlp", e)
                    }
                }

                YoutubeDL.getInstance().init(this@MyApplication)
                FFmpeg.getInstance().init(this@MyApplication)

                isInitialized = true
                isYoutubeDLReady = true
                Log.d(Config.TAG_YT_DLP,
                    "Inicialización exitosa de componentes nativos desde APK assets")

                // AUTO-HEALING del motor: verifica silenciosamente en GitHub.
                launch(Dispatchers.IO) {
                    try {
                        com.fabian.downloader.managers.YtdlpUpdateManager
                            .autoUpdateSilentlyOnFailure(this@MyApplication)
                    } catch (e: Exception) {
                        Log.w(Config.TAG_YT_DLP,
                            "Verificación proactiva de versión de yt-dlp falló", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(Config.TAG_YT_DLP,
                    "Error al inicializar binarios nativos. Intentando reset limpio...", e)
                com.fabian.downloader.managers.ErrorLogManager.logError(
                    this@MyApplication,
                    Config.TAG_YT_DLP,
                    "Error en init inicial de YoutubeDL",
                    e
                )
                try {
                    val recovered = resetAndReinitYtdlp(this@MyApplication)
                    if (recovered) {
                        isInitialized = true
                        isYoutubeDLReady = true
                    }
                } catch (recEx: Exception) {
                    Log.e(Config.TAG_YT_DLP, "Reset de rescate falló", recEx)
                }
            } finally {
                initLatch.countDown()
            }
        }
    }

    /**
     * Instala UN único `UncaughtExceptionHandler` a nivel de proceso.
     * Antes había dos handlers (en `MyApplication` y en `ErrorLogManager.init`)
     * que duplicaban cada crash en el archivo de logs.
     */
    private fun installUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is OutOfMemoryError) {
                // No llamamos a System.gc(): en OOM real no hay memoria para el GC
                // y la llamada puede empeorar la latencia del crash o causar ANR.
                Log.e(Config.TAG_DOWNLOAD_MANAGER,
                    "Out of Memory Error detected! Cleaning up...", throwable)
                try {
                    com.fabian.downloader.services.ExtractionService.clearCaches()
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_MANAGER,
                        "No se pudo limpiar cachés tras OOM", e)
                }
            }
            com.fabian.downloader.managers.ErrorLogManager.logError(
                this,
                "UncaughtException",
                "Crash in thread ${thread.name}: ${throwable.message}",
                throwable
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Usa `ProcessLifecycleOwner` para detectar foreground/background.
     * La implementación anterior con `ActivityLifecycleCallbacks` tenía un bug:
     * tras una rotación, `isActivityChangingConfigurations` se quedaba pegado en
     * `true` y la app quedaba marcada como background aunque estuviera visible.
     */
    private fun installForegroundLifecycleObserver() {
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    isAppInForeground = true
                }

                override fun onStop(owner: LifecycleOwner) {
                    isAppInForeground = false
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "ProcessLifecycleOwner no disponible, fallback a ActivityCallbacks", e)
            installFallbackActivityLifecycleCallbacks()
        }
    }

    private fun installFallbackActivityLifecycleCallbacks() {
        var activityReferences = 0
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: android.app.Activity,
                savedInstanceState: android.os.Bundle?
            ) {}

            override fun onActivityStarted(activity: android.app.Activity) {
                if (++activityReferences == 1) {
                    isAppInForeground = true
                }
            }

            override fun onActivityResumed(activity: android.app.Activity) {}

            override fun onActivityPaused(activity: android.app.Activity) {}

            override fun onActivityStopped(activity: android.app.Activity) {
                val changingConfig = activity.isChangingConfigurations
                if (--activityReferences == 0 && !changingConfig) {
                    isAppInForeground = false
                }
                // NOTA: no persistimos `changingConfig` — solo es válido en onStop.
            }

            override fun onActivitySaveInstanceState(
                activity: android.app.Activity,
                outState: android.os.Bundle
            ) {}

            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    fun resetAndReinitYtdlp(context: android.content.Context): Boolean {
        if (!ytdlpResetLock.compareAndSet(false, true)) {
            Log.d(Config.TAG_YT_DLP, "Reset de yt-dlp omitido (ya en curso)")
            return false
        }
        return try {
            Log.w(Config.TAG_YT_DLP,
                "Resetting corrupt yt-dlp directory and re-initializing from APK assets...")
            val dirsToClean = listOfNotNull(
                context.noBackupFilesDir,
                context.filesDir,
                context.cacheDir
            )
            for (parentDir in dirsToClean) {
                val ytdlDir = java.io.File(parentDir, "youtubedl-android")
                if (ytdlDir.exists()) {
                    ytdlDir.deleteRecursively()
                }
            }
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            isInitialized = true
            isYoutubeDLReady = true
            Log.i(Config.TAG_YT_DLP, "yt-dlp directory cleanly re-initialized from APK assets")
            true
        } catch (e: Exception) {
            Log.e(Config.TAG_YT_DLP, "Error resetting and re-initializing yt-dlp", e)
            com.fabian.downloader.managers.ErrorLogManager.logError(
                context,
                Config.TAG_YT_DLP,
                "Error al resetear y re-inicializar yt-dlp",
                e
            )
            false
        } finally {
            ytdlpResetLock.set(false)
        }
    }

    /**
     * Fuerza la actualización del binario yt-dlp.
     *
     * @param ignoreThrottle si `true`, usa un cooldown corto (5min) en lugar del
     *                       cooldown manual (1h). Antes este flag usaba el mismo
     *                       cooldown que el auto-update (24h), lo que bloqueaba
     *                       actualizaciones manuales durante 24h tras un auto-check.
     * @param forcedSource   origen de la llamada, solo para logs.
     */
    fun forceUpdateYtdlpBinary(
        context: android.content.Context,
        ignoreThrottle: Boolean = false,
        forcedSource: String = "manual"
    ): Boolean {
        val prefs = context.getSharedPreferences(
            Config.PREFS_NAME, android.content.Context.MODE_PRIVATE
        )
        val now = System.currentTimeMillis()

        // Cooldown distinto según el origen. Ya NO compartimos clave con el auto-update.
        val cooldownMs = if (ignoreThrottle) COOLDOWN_FORCED_MS else COOLDOWN_MANUAL_MS
        val prefKey = if (ignoreThrottle) PREF_LAST_YTDLP_FORCED_UPDATE
                      else PREF_LAST_YTDLP_MANUAL_UPDATE
        val lastCheck = prefs.getLong(prefKey, 0L)

        if (now - lastCheck < cooldownMs ||
            (ignoreThrottle && now - lastForceUpdateTimestamp < COOLDOWN_FORCED_MS)
        ) {
            Log.d(Config.TAG_YT_DLP,
                "Actualización de yt-dlp omitida (origen=$forcedSource, " +
                    "cooldown restante=${(cooldownMs - (now - lastCheck)) / 1000}s)")
            return false
        }
        // Si otra corrutina ya está actualizando, no duplicar el trabajo
        if (!ytdlpUpdateLock.compareAndSet(false, true)) {
            Log.d(Config.TAG_YT_DLP,
                "Actualización de yt-dlp omitida (ya en curso, origen=$forcedSource)")
            return false
        }
        lastForceUpdateTimestamp = now
        prefs.edit().putLong(prefKey, now).apply()

        return try {
            Log.i(Config.TAG_YT_DLP,
                "Descargando actualización de binario yt-dlp (origen=$forcedSource)...")
            val updateResult = YoutubeDL.getInstance().updateYoutubeDL(context)
            Log.i(Config.TAG_YT_DLP,
                "Binario de yt-dlp actualizado exitosamente: $updateResult")
            true
        } catch (e: Exception) {
            Log.e(Config.TAG_YT_DLP, "Error al actualizar binario yt-dlp", e)
            false
        } finally {
            ytdlpUpdateLock.set(false)
        }
    }

    fun isYtdlpReady(context: android.content.Context = this): Boolean {
        if (isYoutubeDLReady) return true
        return try {
            YoutubeDL.getInstance().version(context)
            isYoutubeDLReady = true
            isInitialized = true
            true
        } catch (e: Exception) {
            // Cualquier excepción significa que NO está listo. Antes se devolvía
            // `true` para errores que no fueran "not initialized", lo que hacía
            // que se lanzaran descargas destinadas a fallar.
            Log.w(Config.TAG_YT_DLP, "isYtdlpReady check failed: ${e.message}")
            false
        }
    }

    fun waitForInitialization() {
        if (!isYoutubeDLReady) {
            val started = initLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)
            if (!started) {
                android.util.Log.w(Config.TAG_YT_DLP, "waitForInitialization timed out after 15s")
            }
        }
    }

    @Synchronized
    fun ensureInitialized(context: android.content.Context = this): Boolean {
        waitForInitialization()
        if (isYtdlpReady(context)) {
            return true
        }
        Log.w(Config.TAG_YT_DLP,
            "YoutubeDL no está inicializado. Ejecutando inicialización directa de rescate...")
        return try {
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            isYoutubeDLReady = true
            isInitialized = true
            Log.i(Config.TAG_YT_DLP, "Inicialización directa de rescate completada exitosamente")
            true
        } catch (e: Exception) {
            Log.e(Config.TAG_YT_DLP, "Fallo en init síncrono. Ejecutando reset limpio...", e)
            com.fabian.downloader.managers.ErrorLogManager.logError(
                context,
                Config.TAG_YT_DLP,
                "Fallo crítico en init de YoutubeDL",
                e
            )
            resetAndReinitYtdlp(context)
        }
    }
}
