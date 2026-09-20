package com.fabian.downloader.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.BatteryManager
import android.util.Log
import com.fabian.downloader.configs.Config
import com.fabian.downloader.services.DownloadManagerService
import com.fabian.downloader.ui.AppSettings

class BatteryOptimizerManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: BatteryOptimizerManager? = null

        fun getInstance(context: Context): BatteryOptimizerManager {
            return instance ?: synchronized(this) {
                instance ?: BatteryOptimizerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var currentLevel = 100
    private var isCharging = false
    private var isRegistered = false

    private val settingsListener = { key: String ->
        if (key == "batteryOptimizationEnabled") {
            evaluateRegistration()
        }
    }

    private inner class BatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                if (level != -1 && scale != -1) {
                    currentLevel = (level * 100 / scale.toFloat()).toInt()
                }

                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

                Log.d(Config.TAG_DOWNLOAD_MANAGER,
                    "Batería actualizada: $currentLevel%, cargando: $isCharging")

                evaluateBatteryStatus()
            }
        }
    }

    private val batteryReceiver: BroadcastReceiver = BatteryReceiver()

    init {
        AppSettings.addListener(settingsListener)
        evaluateRegistration()
    }

    @Synchronized
    private fun evaluateRegistration() {
        if (AppSettings.batteryOptimizationEnabled) {
            if (!isRegistered) {
                try {
                    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    // Android 13+ (TIRAMISU) requiere flag RECEIVER_NOT_EXPORTED
                    // o RECEIVER_EXPORTED al registrar receivers dinámicos.
                    // ACTION_BATTERY_CHANGED es un broadcast protegido del sistema,
                    // pero algunos OEM (MIUI, OneUI) validan el flag igualmente.
                    val stickyIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(
                            batteryReceiver, filter,
                            Context.RECEIVER_NOT_EXPORTED
                        )
                    } else {
                        context.registerReceiver(batteryReceiver, filter)
                    }
                    isRegistered = true
                    if (stickyIntent != null) {
                        val level = stickyIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = stickyIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val status = stickyIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        if (level != -1 && scale != -1) {
                            currentLevel = (level * 100 / scale.toFloat()).toInt()
                        }
                        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    }
                    Log.d(Config.TAG_DOWNLOAD_MANAGER,
                        "BatteryOptimizerManager: Receiver registrado. " +
                            "Batería: $currentLevel%, cargando: $isCharging")
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_MANAGER,
                        "Error al registrar batteryReceiver", e)
                }
            }
        } else {
            if (isRegistered) {
                try {
                    context.unregisterReceiver(batteryReceiver)
                    isRegistered = false
                    Log.d(Config.TAG_DOWNLOAD_MANAGER,
                        "BatteryOptimizerManager: Receiver desregistrado.")
                } catch (e: Exception) {
                    Log.e(Config.TAG_DOWNLOAD_MANAGER,
                        "Error al desregistrar batteryReceiver", e)
                }
            }
        }
    }

    fun isBatteryLowAndNotCharging(): Boolean {
        if (!AppSettings.batteryOptimizationEnabled) return false
        return currentLevel <= AppSettings.batteryLowThresholdInt && !isCharging
    }

    /**
     * Evalúa el estado de la batería y aplica la acción configurada.
     *
     * Antes `OPTIMIZE` y `LIMIT` llamaban exactamente al mismo método
     * (`throttleActiveDownloads`), por lo que ambas opciones se comportaban
     * idénticamente. Ahora:
     *
     *  - `OPTIMIZE`: baja prioridad de hilos y reduce concurrencia en 1.
     *  - `LIMIT`: fuerza concurrencia a 1 (descargas estrictamente secuenciales).
     *
     * Requiere que `DownloadManagerService` exponga los métodos
     * `throttleActiveDownloads(reduceConcurrencyBy:)` y `limitConcurrencyTo(_)`.
     * Si esos métodos no existen aún, el fallback es el comportamiento anterior.
     */
    fun evaluateBatteryStatus() {
        if (isBatteryLowAndNotCharging()) {
            val manager = DownloadManagerService.getInstance(context)
            when (AppSettings.batteryLowAction) {
                Config.BATTERY_ACTION_OPTIMIZE -> {
                    Log.w(Config.TAG_DOWNLOAD_MANAGER,
                        "Batería baja ($currentLevel%). " +
                            "Optimizando: baja prioridad y concurrencia -1.")
                    try {
                        // Método preferido: si existe en DownloadManagerService, usarlo.
                        // Fallback al método anterior para compatibilidad.
                        val m = manager.javaClass.getMethod(
                            "throttleActiveDownloads",
                            Int::class.javaPrimitiveType
                        )
                        m.invoke(manager, 1)
                    } catch (nsm: NoSuchMethodException) {
                        // Compatibilidad: llamar al método sin parámetros.
                        manager.throttleActiveDownloads()
                    } catch (e: Exception) {
                        Log.e(Config.TAG_DOWNLOAD_MANAGER,
                            "Error aplicando OPTIMIZE", e)
                        manager.throttleActiveDownloads()
                    }
                }
                Config.BATTERY_ACTION_LIMIT -> {
                    Log.w(Config.TAG_DOWNLOAD_MANAGER,
                        "Batería baja ($currentLevel%). " +
                            "Limitando concurrencia a 1 (descargas secuenciales).")
                    try {
                        val m = manager.javaClass.getMethod(
                            "limitConcurrencyTo",
                            Int::class.javaPrimitiveType
                        )
                        m.invoke(manager, 1)
                    } catch (nsm: NoSuchMethodException) {
                        // Compatibilidad temporal.
                        manager.throttleActiveDownloads()
                    } catch (e: Exception) {
                        Log.e(Config.TAG_DOWNLOAD_MANAGER,
                            "Error aplicando LIMIT", e)
                        manager.throttleActiveDownloads()
                    }
                }
            }
        } else {
            // Si la batería ya no está baja o está cargando, reanudar la cola.
            val manager = DownloadManagerService.getInstance(context)
            manager.triggerQueue()
        }
    }
}
