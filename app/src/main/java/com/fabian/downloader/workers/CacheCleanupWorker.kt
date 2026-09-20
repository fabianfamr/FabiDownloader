package com.fabian.downloader.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker de limpieza de archivos temporales (.part, .downloading, .ytdl, .temp).
 *
 * Issue 7.1: Antes `scheduleCleanup` se llamaba tras CADA descarga, lo que
 * encolaba N trabajos idénticos (uno por descarga). Ahora se usa
 * `enqueueUniqueWork` con `ExistingWorkPolicy.KEEP` para evitar duplicados.
 */
class CacheCleanupWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "CacheCleanupWorker"
        private const val UNIQUE_WORK_NAME = "cache_cleanup"

        /**
         * Programa la limpieza de caché. Idempotente: si ya hay un trabajo
         * encolado con el mismo nombre, se mantiene el existente (KEEP).
         */
        fun scheduleCleanup(context: Context) {
            val request = OneTimeWorkRequestBuilder<CacheCleanupWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    1,
                    TimeUnit.HOURS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,  // ← no duplicar
                request
            )
        }
    }

    override fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando limpieza de archivos temporales...")
            val cleaned = performCleanup()
            Log.d(TAG, "Limpieza completada. Archivos eliminados: $cleaned")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error durante limpieza de caché", e)
            Result.retry()
        }
    }

    /**
     * Implementación real de la limpieza. Adaptar según el PathUtils de tu app.
     * Devuelve el número de archivos eliminados.
     */
    private fun performCleanup(): Int {
        var count = 0
        try {
            val downloadDirs = listOf(
                java.io.File(applicationContext.getExternalFilesDir(null), "FabiDownloader/downloads/video"),
                java.io.File(applicationContext.getExternalFilesDir(null), "FabiDownloader/downloads/audio"),
                java.io.File(applicationContext.getExternalFilesDir(null), "FabiDownloader/downloads/image")
            )
            val tempSuffixes = setOf(".part", ".ytdl", ".temp", ".tmp", ".downloading")
            for (dir in downloadDirs) {
                if (!dir.exists() || !dir.isDirectory) continue
                dir.listFiles()?.forEach { file ->
                    val name = file.name
                    if (tempSuffixes.any { name.endsWith(it, ignoreCase = true) } ||
                        name.contains(".downloading", ignoreCase = true)
                    ) {
                        if (file.delete()) count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo acceder a algunos directorios: ${e.message}")
        }
        return count
    }
}
