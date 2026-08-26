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
     */
    fun hasStoragePermission(context: Context): Boolean {
        return when {
            // Android 11+ (API 30+): Se usa el permiso de administración de archivos
            isAndroid11OrHigher() -> {
                isAllFilesAccessGranted()
            }
            // Android 10 y anteriores (API < 30)
            else -> {
                val writeStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                val readStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                writeStorage && readStorage
            }
        }
    }

    /**
     * Obtiene el arreglo de permisos que deben solicitarse según la versión del sistema operativo.
     */
    fun getRequiredPermissions(): Array<String> {
        return when {
            isAndroid11OrHigher() -> emptyArray()
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Verifica si la aplicación tiene permiso de administración de todos los archivos (MANAGE_EXTERNAL_STORAGE).
     */
    fun isAllFilesAccessGranted(): Boolean {
        return if (isAndroid11OrHigher()) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Construye un Intent para dirigir al usuario a la pantalla de configuración del permiso de administración de archivos.
     */
    fun getAllFilesAccessIntent(context: Context): Intent {
        return if (isAndroid11OrHigher()) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
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
