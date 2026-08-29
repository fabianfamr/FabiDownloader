package com.fabian.downloader.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.fabian.downloader.R
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

data class BrandOptimizationGuide(
    val brand: DeviceBrand,
    val title: String,
    val steps: List<String>,
    val canOpenDirectly: Boolean,
    val actionIntentName: String
)

object DeviceOptimizationHelper {

    fun getCurrentBrand(): DeviceBrand = DeviceBrand.detectCurrent()

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getOptimizationGuide(context: Context, brand: DeviceBrand = getCurrentBrand()): BrandOptimizationGuide {
        return when (brand) {
            DeviceBrand.SAMSUNG -> BrandOptimizationGuide(
                brand = DeviceBrand.SAMSUNG,
                title = "Optimización en Samsung (One UI)",
                steps = listOf(
                    "1. Toca 'Abrir Ajustes' abajo.",
                    "2. En Batería, selecciona 'Sin restricciones' (o desactiva 'Poner en suspensión').",
                    "3. En 'Ajustes > Cuidado del dispositivo > Batería', asegúrate de no agregar FabiDownloader a 'Aplicaciones suspendidas'."
                ),
                canOpenDirectly = true,
                actionIntentName = "Ajustes de Batería Samsung"
            )
            DeviceBrand.XIAOMI -> BrandOptimizationGuide(
                brand = DeviceBrand.XIAOMI,
                title = "Optimización en Xiaomi / HyperOS / MIUI",
                steps = listOf(
                    "1. Toca 'Abrir Ajustes' abajo para entrar a la ficha de la app.",
                    "2. En 'Ahorro de batería', selecciona 'Sin restricciones'.",
                    "3. Activa 'Inicio automático' si deseas descargas desatendidas.",
                    "4. En la pantalla de apps recientes, mantén presionada la tarjeta de FabiDownloader y toca el candado 🔒 para bloquearla en memoria."
                ),
                canOpenDirectly = true,
                actionIntentName = "Ahorro de batería MIUI"
            )
            DeviceBrand.HUAWEI -> BrandOptimizationGuide(
                brand = DeviceBrand.HUAWEI,
                title = "Optimización en Huawei / Honor (EMUI / MagicOS)",
                steps = listOf(
                    "1. Toca 'Abrir Ajustes' abajo.",
                    "2. Dirígete a 'Batería > Inicio de aplicaciones'.",
                    "3. Busca FabiDownloader, desactiva 'Gestionar automáticamente' y activa 'Inicio automático', 'Inicio secundario' y 'Ejecutar en segundo plano'."
                ),
                canOpenDirectly = true,
                actionIntentName = "Inicio de apps EMUI"
            )
            DeviceBrand.OPPO -> BrandOptimizationGuide(
                brand = DeviceBrand.OPPO,
                title = "Optimización en OPPO / Realme / OnePlus (ColorOS)",
                steps = listOf(
                    "1. Toca 'Abrir Ajustes' abajo.",
                    "2. En 'Uso de batería de la app', activa 'Permitir actividad en segundo plano' y 'Permitir inicio automático'.",
                    "3. En 'Optimización de batería', selecciona 'No optimizar'."
                ),
                canOpenDirectly = true,
                actionIntentName = "Batería ColorOS"
            )
            DeviceBrand.VIVO -> BrandOptimizationGuide(
                brand = DeviceBrand.VIVO,
                title = "Optimización en Vivo / iQOO (FuntouchOS)",
                steps = listOf(
                    "1. Toca 'Abrir Ajustes' abajo.",
                    "2. En 'Gestión de consumo de batería en segundo plano', selecciona 'Permitir alto consumo en segundo plano'.",
                    "3. En 'Permisos', activa 'Inicio automático'."
                ),
                canOpenDirectly = true,
                actionIntentName = "Batería FuntouchOS"
            )
            DeviceBrand.GENERIC -> BrandOptimizationGuide(
                brand = DeviceBrand.GENERIC,
                title = "Optimización de Batería en Android",
                steps = listOf(
                    "1. Toca 'Abrir Ajustes' abajo.",
                    "2. En Batería / Uso de batería, selecciona 'Sin restricciones' (o 'No optimizar').",
                    "3. Esto asegura que tus descargas pesadas continúen aunque apagues la pantalla."
                ),
                canOpenDirectly = true,
                actionIntentName = "Ajustes de Batería"
            )
        }
    }

    fun openBrandBatterySettings(context: Context, brand: DeviceBrand = getCurrentBrand()): Boolean {
        val packageName = context.packageName

        // 1. Intentos específicos por fabricante
        val intents = when (brand) {
            DeviceBrand.XIAOMI -> listOf(
                Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                    .putExtra("package_name", packageName)
                    .putExtra("package_label", "FabiDownloader"),
                Intent("miui.intent.action.APP_PERM_EDITOR")
                    .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                    .putExtra("extra_pkgname", packageName),
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"))
            )
            DeviceBrand.SAMSUNG -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            )
            DeviceBrand.HUAWEI -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
            )
            DeviceBrand.OPPO -> listOf(
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.BatteryMainActivity"))
            )
            DeviceBrand.VIVO -> listOf(
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"))
            )
            DeviceBrand.GENERIC -> emptyList()
        }

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(Config.TAG_DOWNLOAD_MANAGER, "Fallback intent no soportado: ${intent.component}")
            }
        }

        // 2. Intent estándar de solicitud de excepción de batería (Android 6.0+)
        try {
            val reqOptIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(reqOptIntent)
            return true
        } catch (e: Exception) {
            Log.d(Config.TAG_DOWNLOAD_MANAGER, "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS fallo: ${e.message}")
        }

        // 3. Intent de Detalles de Aplicación
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appDetailsIntent)
            return true
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error abriendo detalles de app", e)
        }

        return false
    }
}
