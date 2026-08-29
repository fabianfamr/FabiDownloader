package com.fabian.downloader.utils

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import com.fabian.downloader.configs.Config

enum class DeviceBrand(val displayName: String, val systemLayerName: String) {
    SAMSUNG("Samsung", "One UI"),
    XIAOMI("Xiaomi / Redmi / POCO", "MIUI / HyperOS"),
    HUAWEI("Huawei / Honor", "EMUI / MagicOS"),
    OPPO("OPPO / Realme / OnePlus", "ColorOS / OxygenOS"),
    VIVO("Vivo / iQOO", "FuntouchOS / OriginOS"),
    GENERIC("Android Estándar", "AOSP / Pixel");

    companion object {
        fun detectCurrent(): DeviceBrand {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val brand = Build.BRAND.lowercase()
            return when {
                manufacturer.contains("samsung") || brand.contains("samsung") -> SAMSUNG
                manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                        manufacturer.contains("redmi") || brand.contains("redmi") ||
                        manufacturer.contains("poco") || brand.contains("poco") -> XIAOMI
                manufacturer.contains("huawei") || brand.contains("huawei") ||
                        manufacturer.contains("honor") || brand.contains("honor") -> HUAWEI
                manufacturer.contains("oppo") || brand.contains("oppo") ||
                        manufacturer.contains("realme") || brand.contains("realme") ||
                        manufacturer.contains("oneplus") || brand.contains("oneplus") -> OPPO
                manufacturer.contains("vivo") || brand.contains("vivo") ||
                        manufacturer.contains("iqoo") || brand.contains("iqoo") -> VIVO
                else -> GENERIC
            }
        }
    }
}

data class DeviceDownloadTuning(
    val defaultConcurrentFragments: Int,
    val httpChunkSize: String,
    val bufferSize: String,
    val threadPriority: Int,
    val socketTimeoutMs: Int
)

object DeviceOptimizationHelper {

    val currentBrand: DeviceBrand by lazy { DeviceBrand.detectCurrent() }

    /**
     * Devuelve parámetros de descarga calibrados automáticamente de forma interna
     * según el fabricante, CPU y el comportamiento de gestión de memoria/I/O de cada marca.
     */
    fun getAutoTuning(): DeviceDownloadTuning {
        val brand = currentBrand
        val numCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val maxMemoryMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()

        return when (brand) {
            DeviceBrand.XIAOMI -> {
                // MIUI / HyperOS gestiona agresivamente procesos con alto número de hilos de red
                DeviceDownloadTuning(
                    defaultConcurrentFragments = if (numCores >= 8) 4 else 3,
                    httpChunkSize = if (maxMemoryMb > 256) "10M" else "5M",
                    bufferSize = "32K",
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 15000
                )
            }
            DeviceBrand.SAMSUNG -> {
                // Samsung One UI funciona con alto rendimiento en I/O con chunks grandes de 10-15M
                DeviceDownloadTuning(
                    defaultConcurrentFragments = if (numCores >= 8) 4 else 3,
                    httpChunkSize = "10M",
                    bufferSize = "64K",
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 12000
                )
            }
            DeviceBrand.HUAWEI -> {
                // EMUI restringe los sockets concurrentes; se usa chunk moderado y buffer estándar
                DeviceDownloadTuning(
                    defaultConcurrentFragments = 3,
                    httpChunkSize = "5M",
                    bufferSize = "16K",
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 15000
                )
            }
            DeviceBrand.OPPO -> {
                // ColorOS / RealmeOS tiene excelente estabilidad con fragmentación de 4 y buffer 32K
                DeviceDownloadTuning(
                    defaultConcurrentFragments = if (numCores >= 8) 4 else 3,
                    httpChunkSize = "10M",
                    bufferSize = "32K",
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 12000
                )
            }
            DeviceBrand.VIVO -> {
                // FuntouchOS / OriginOS
                DeviceDownloadTuning(
                    defaultConcurrentFragments = 3,
                    httpChunkSize = "5M",
                    bufferSize = "32K",
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 12000
                )
            }
            DeviceBrand.GENERIC -> {
                DeviceDownloadTuning(
                    defaultConcurrentFragments = if (numCores >= 8) 4 else 3,
                    httpChunkSize = "10M",
                    bufferSize = "16K",
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 10000
                )
            }
        }
    }

    /**
     * Aplica optimizaciones automáticas a la pantalla, ventana, tasa de refresco (90/120Hz)
     * y recorte de pantalla (notch/punch hole) adaptadas a la pantalla del dispositivo.
     */
    fun applyScreenAndWindowOptimizations(activity: Activity) {
        try {
            val window = activity.window

            // 1. Adaptación de Recorte de Pantalla (Punch Hole / Notch / Bordes Curvos)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val layoutParams = window.attributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    layoutParams.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    layoutParams.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                window.attributes = layoutParams
            }

            // 2. Activación automática de alta tasa de refresco (90Hz / 120Hz / 144Hz) en Samsung, Xiaomi, OnePlus, Vivo, etc.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display
                } else {
                    @Suppress("DEPRECATION")
                    window.windowManager.defaultDisplay
                }

                if (display != null) {
                    val supportedModes = display.supportedModes
                    val highestRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
                    if (highestRefreshRateMode != null && highestRefreshRateMode.refreshRate > 60f) {
                        val params = window.attributes
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            params.preferredDisplayModeId = highestRefreshRateMode.modeId
                            window.attributes = params
                            Log.d(Config.TAG_DOWNLOAD_MANAGER, "Pantalla adaptada automáticamente a ${highestRefreshRateMode.refreshRate}Hz")
                        }
                    }
                }
            }

            // 3. Flags de aceleración de hardware fluida
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        } catch (e: Throwable) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER, "Adaptación de pantalla omitida: ${e.message}")
        }
    }

    /**
     * Adquiere un WakeLock parcial y un WifiLock de alto rendimiento para garantizar
     * que marcas agresivas (MIUI, EMUI, ColorOS, FuntouchOS, OneUI) no suspendan la CPU ni el WiFi
     * con la pantalla apagada durante descargas activas.
     */
    fun acquireDownloadLocks(context: Context): Pair<PowerManager.WakeLock?, WifiManager.WifiLock?> {
        var wakeLock: PowerManager.WakeLock? = null
        var wifiLock: WifiManager.WifiLock? = null

        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "FabiDownloader:DownloadActiveWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    // Auto-liberación de seguridad máxima tras 60 minutos
                    acquire(60 * 60 * 1000L)
                }
            }
        } catch (e: Throwable) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo adquirir WakeLock: ${e.message}")
        }

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                val wifiMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager.createWifiLock(wifiMode, "FabiDownloader:DownloadWifiLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Throwable) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER, "No se pudo adquirir WifiLock: ${e.message}")
        }

        return Pair(wakeLock, wifiLock)
    }

    fun releaseDownloadLocks(wakeLock: PowerManager.WakeLock?, wifiLock: WifiManager.WifiLock?) {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        } catch (_: Throwable) {}

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock.release()
            }
        } catch (_: Throwable) {}
    }
}
