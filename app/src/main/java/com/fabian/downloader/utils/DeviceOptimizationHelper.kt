package com.fabian.downloader.utils

import android.os.Build
import android.util.Log
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
                // Bloques de 8MB y buffers optimizados para evitar bloqueos del garbage collector de MIUI
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
}
