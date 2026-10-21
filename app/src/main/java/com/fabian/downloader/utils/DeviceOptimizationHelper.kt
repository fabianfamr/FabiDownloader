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

enum class HardwareTier(
    val displayName: String,
    val baseFragments: Int,
    val maxSafeFragments: Int,
    val maxParallelDownloads: Int
) {
    LOW("Básica", 2, 3, 1),
    MID("Media", 4, 6, 2),
    HIGH("Alta", 6, 8, 3),
    ULTRA("Ultra", 8, 12, 4)
}

data class DeviceDownloadTuning(
    val defaultConcurrentFragments: Int,
    val maxSafeFragments: Int = 8,
    val httpChunkSize: String,
    val bufferSize: String,
    val threadPriority: Int,
    val socketTimeoutMs: Int
)

object DeviceOptimizationHelper {

    val currentBrand: DeviceBrand by lazy { DeviceBrand.detectCurrent() }

    /**
     * Detecta el perfil de hardware en tiempo de ejecución (núcleos de CPU y memoria de la JVM)
     * para balancear el número de fragmentos y colas simultáneas sin saturar el sistema.
     */
    fun detectHardwareTier(): HardwareTier {
        val numCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val maxMemoryMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()

        return when {
            numCores >= 8 && maxMemoryMb >= 384 -> HardwareTier.ULTRA
            numCores >= 6 && maxMemoryMb >= 256 -> HardwareTier.HIGH
            numCores >= 4 && maxMemoryMb >= 160 -> HardwareTier.MID
            else -> HardwareTier.LOW
        }
    }

    /**
     * Calcula de forma dinámica el número óptimo de descargas simultáneas en la cola,
     * considerando el tier del hardware y el estado de la batería.
     */
    fun getDynamicMaxConcurrentDownloads(context: Context): Int {
        val tier = detectHardwareTier()
        return tier.maxParallelDownloads
    }

    /**
     * Devuelve parámetros de descarga calibrados automáticamente de forma interna
     * según el fabricante, CPU y el comportamiento de gestión de memoria/I/O de cada marca.
     */
    fun getAutoTuning(): DeviceDownloadTuning {
        val brand = currentBrand
        val tier = detectHardwareTier()

        val baseFragments = when (tier) {
            HardwareTier.ULTRA -> 8
            HardwareTier.HIGH -> 6
            HardwareTier.MID -> 4
            HardwareTier.LOW -> 2
        }

        val maxFragments = when (tier) {
            HardwareTier.ULTRA -> 12
            HardwareTier.HIGH -> 8
            HardwareTier.MID -> 6
            HardwareTier.LOW -> 3
        }

        val chunkSize = when (tier) {
            HardwareTier.ULTRA, HardwareTier.HIGH -> "10M"
            HardwareTier.MID -> "8M"
            HardwareTier.LOW -> "4M"
        }

        val buffer = when (tier) {
            HardwareTier.ULTRA, HardwareTier.HIGH -> "64K"
            HardwareTier.MID -> "32K"
            HardwareTier.LOW -> "16K"
        }

        return when (brand) {
            DeviceBrand.XIAOMI -> {
                DeviceDownloadTuning(
                    defaultConcurrentFragments = minOf(baseFragments, 6),
                    maxSafeFragments = minOf(maxFragments, 8),
                    httpChunkSize = chunkSize,
                    bufferSize = buffer,
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 15000
                )
            }
            DeviceBrand.SAMSUNG -> {
                DeviceDownloadTuning(
                    defaultConcurrentFragments = baseFragments,
                    maxSafeFragments = maxFragments,
                    httpChunkSize = chunkSize,
                    bufferSize = buffer,
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 12000
                )
            }
            DeviceBrand.HUAWEI -> {
                DeviceDownloadTuning(
                    defaultConcurrentFragments = minOf(baseFragments, 4),
                    maxSafeFragments = minOf(maxFragments, 6),
                    httpChunkSize = chunkSize,
                    bufferSize = if (tier == HardwareTier.LOW) "16K" else "32K",
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 15000
                )
            }
            DeviceBrand.OPPO -> {
                DeviceDownloadTuning(
                    defaultConcurrentFragments = baseFragments,
                    maxSafeFragments = maxFragments,
                    httpChunkSize = chunkSize,
                    bufferSize = buffer,
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 12000
                )
            }
            DeviceBrand.VIVO -> {
                DeviceDownloadTuning(
                    defaultConcurrentFragments = minOf(baseFragments, 6),
                    maxSafeFragments = minOf(maxFragments, 8),
                    httpChunkSize = chunkSize,
                    bufferSize = buffer,
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 12000
                )
            }
            DeviceBrand.GENERIC -> {
                DeviceDownloadTuning(
                    defaultConcurrentFragments = baseFragments,
                    maxSafeFragments = maxFragments,
                    httpChunkSize = chunkSize,
                    bufferSize = buffer,
                    threadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND,
                    socketTimeoutMs = 12000
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
                val wifiMode = @Suppress("DEPRECATION") WifiManager.WIFI_MODE_FULL_HIGH_PERF
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
