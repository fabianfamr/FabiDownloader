package com.fabian.downloader.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.Config

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

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                if (level != -1 && scale != -1) {
                    currentLevel = (level * 100 / scale.toFloat()).toInt()
                }
                
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                Log.d(Config.TAG_DOWNLOAD_MANAGER, "Batería actualizada: $currentLevel%, cargando: $isCharging")
                
                // Evaluar si debemos aplicar restricciones
                evaluateBatteryStatus()
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun isBatteryLowAndNotCharging(): Boolean {
        if (!AppSettings.batteryOptimizationEnabled) return false
        return currentLevel <= AppSettings.batteryLowThresholdInt && !isCharging
    }

    fun evaluateBatteryStatus() {
        if (isBatteryLowAndNotCharging()) {
            val manager = DownloadManagerService.getInstance(context)
            if (AppSettings.batteryLowAction == "Optimizar recursos") {
                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Batería baja detectada ($currentLevel%). Optimizando recursos (concurrencia y hilos limitados).")
                manager.throttleActiveDownloads()
            } else if (AppSettings.batteryLowAction == "Limitar concurrencia") {
                Log.w(Config.TAG_DOWNLOAD_MANAGER, "Batería baja detectada ($currentLevel%). Limitando concurrencia a 1.")
                manager.throttleActiveDownloads()
            }
        } else {
            // Si la batería ya no está baja o está cargando, intentamos reanudar la cola
            val manager = DownloadManagerService.getInstance(context)
            manager.triggerQueue()
        }
    }
}
