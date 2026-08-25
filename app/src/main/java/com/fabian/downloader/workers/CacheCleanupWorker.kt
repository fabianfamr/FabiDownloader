package com.fabian.downloader.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fabian.downloader.configs.Config
import com.fabian.downloader.services.ExtractionService
import com.fabian.downloader.utils.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CacheCleanupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(Config.TAG_DOWNLOAD_MANAGER, "Ejecutando CacheCleanupWorker para liberar memoria y espacio en disco...")

            // Limpiar caché interno y externo de la aplicación
            cleanDirectory(appContext.cacheDir)
            appContext.externalCacheDir?.let { cleanDirectory(it) }

            // Limpiar archivos temporales huérfanos de descarga (.part, .ytdl, .temp)
            cleanTempDownloadFiles(appContext)

            // Limpiar cachés en memoria
            ExtractionService.clearCaches()

            Log.i(Config.TAG_DOWNLOAD_MANAGER, "CacheCleanupWorker completado con éxito.")
            Result.success()
        } catch (e: Exception) {
            Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error durante la ejecución de CacheCleanupWorker", e)
            Result.failure()
        }
    }

    private fun cleanDirectory(dir: File?, maxAgeMs: Long = 3_600_000L) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        val name = dir.name.lowercase()
        if (name == "no_backup" || name == "youtubedl-android" || name == "yt-dlp" ||
            name.contains("youtubedl") || name.contains("python") || name.contains("ffmpeg")) return
        try {
            val now = System.currentTimeMillis()
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val childName = file.name.lowercase()
                    if (childName == "no_backup" || childName == "youtubedl-android" || childName == "yt-dlp" ||
                        childName.contains("youtubedl") || childName.contains("python") || childName.contains("ffmpeg")) return@forEach
                    cleanDirectory(file, maxAgeMs)
                    if (file.listFiles()?.isEmpty() == true) {
                        file.delete()
                    }
                } else {
                    if (now - file.lastModified() > maxAgeMs) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(Config.TAG_DOWNLOAD_MANAGER, "Error limpiando directorio de caché: ${dir.absolutePath}", e)
        }
    }

    private suspend fun cleanTempDownloadFiles(context: Context) {
        val formats = listOf("MP4", "MP3", "M4A", "WEBM")
        val folders = formats.mapNotNull {
            try {
                PathUtils.getDownloadFolder(context, it)
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.absolutePath }

        val activeOrPausedIds = try {
            com.fabian.downloader.database.AppDatabase.getInstance(context).downloadDao()
                .getAllDownloadsDirect()
                .filter { !it.isCompleted }
                .map { it.id.toString() }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }

        folders.forEach { folder ->
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { file ->
                    val name = file.name.lowercase()
                    if (name.endsWith(".part") || name.endsWith(".ytdl") || name.endsWith(".temp") || name.endsWith(".tmp") || name.endsWith(".downloading") || name.contains(".downloading.")) {
                        val age = System.currentTimeMillis() - file.lastModified()
                        val isProtected = activeOrPausedIds.any { id -> name.contains("_$id.") || name.contains("_$id") }
                        // Si no está protegido y tiene más de 2 minutos, o si tiene más de 24 horas aunque esté "protegido", eliminar
                        if ((!isProtected && age > 120_000L) || (age > 86_400_000L)) {
                            try {
                                file.delete()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "cache_cleanup_work"

        fun scheduleCleanup(context: Context) {
            try {
                val request = OneTimeWorkRequestBuilder<CacheCleanupWorker>()
                    .setConstraints(Constraints.NONE)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al programar CacheCleanupWorker", e)
            }
        }

        fun performDirectCleanup(context: Context) {
            try {
                cleanDirectoryDirect(context.cacheDir, maxAgeMs = 0L)
                context.externalCacheDir?.let { cleanDirectoryDirect(it, maxAgeMs = 0L) }
                ExtractionService.clearCaches()
                PathUtils.clearFolderCache()
            } catch (e: Exception) {
                Log.e(Config.TAG_DOWNLOAD_MANAGER, "Error al realizar la limpieza directa de caché", e)
            }
        }

        private fun cleanDirectoryDirect(dir: File?, maxAgeMs: Long = 1_800_000L) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            if (dir.name == "no_backup" || dir.name == "youtubedl-android" || dir.name == "yt-dlp") return
            val now = System.currentTimeMillis()
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    cleanDirectoryDirect(file, maxAgeMs)
                    if (file.listFiles()?.isEmpty() == true && (now - file.lastModified() > maxAgeMs)) {
                        file.delete()
                    }
                } else {
                    if (now - file.lastModified() > maxAgeMs) {
                        file.delete()
                    }
                }
            }
        }
    }
}
