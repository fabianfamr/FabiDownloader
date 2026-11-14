package com.fabian.downloader.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.fabian.downloader.configs.Config
import java.io.File

/**
 * Clase de utilidad para gestionar la verificación y solicitud de permisos de almacenamiento
 * con compatibilidad total para Scoped Storage en Android 10+ (API 29+), Android 11+ (API 30+) y Android 13+ (API 33+).
 */
object StoragePermissionUtils {

    /**
     * Indica si el dispositivo opera bajo Scoped Storage (Android 10 / API 29 o superior).
     */
    fun isScopedStorage(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * Indica si el dispositivo es Android 11 o superior (API 30+).
     */
    fun isAndroid11OrHigher(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    /**
     * Indica si el dispositivo es Android 13 o superior (API 33+).
     */
    fun isAndroid13OrHigher(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Comprueba si el usuario ha concedido los permisos necesarios para guardar archivos en el almacenamiento.
     * Desde Android 10 (API 29+) Scoped Storage / MediaStore / SAF no requiere permiso de escritura global.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return when {
            isScopedStorage() -> true
            else -> ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Obtiene el arreglo de permisos que deben solicitarse según la versión del sistema operativo.
     */
    fun getRequiredPermissions(): Array<String> {
        return when {
            isScopedStorage() -> emptyArray()
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Indica si la aplicación puede operar sin restricciones de acceso de archivos en el ámbito permitido.
     */
    fun isAllFilesAccessGranted(): Boolean {
        return if (isAndroid11OrHigher()) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Construye un Intent para dirigir al usuario a los ajustes de la aplicación.
     */
    fun getAllFilesAccessIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Verifica la capacidad de escritura en la carpeta destino probando la creación temporal de un archivo de prueba.
     */
    fun canWriteToFolder(folder: File): Boolean {
        if (!folder.exists()) {
            val created = try { folder.mkdirs() } catch (_: Exception) { false }
            if (!created && !folder.exists()) return false
        }
        val testFile = File(folder, ".test_write_${System.currentTimeMillis()}.tmp")
        return try {
            if (testFile.createNewFile()) {
                testFile.delete()
                true
            } else {
                folder.canWrite()
            }
        } catch (e: Exception) {
            android.util.Log.e(Config.TAG_PATH_UTILS, "Error al probar escritura en ${folder.absolutePath}: ${e.message}")
            false
        }
    }
}
