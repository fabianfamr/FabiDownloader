package com.fabian.downloader.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Recibir los datos de entrada (URL y ruta de guardado)
        val videoUrl = inputData.getString("VIDEO_URL") ?: return Result.failure()
        val downloadDir = inputData.getString("DOWNLOAD_DIR") ?: return Result.failure()
        
        val targetFolder = File(downloadDir)
        if (!targetFolder.exists()) targetFolder.mkdirs()

        // 2. Configurar la petición de yt-dlp de forma pública y anónima
        val request = YoutubeDLRequest(videoUrl).apply {
            addOption("-o", "${targetFolder.absolutePath}/%(title)s.%(ext)s")
            addOption("-f", "bestvideo+bestaudio/best")
            addOption("--merge-output-format", "mp4")
            addOption("--remux-video", "mp4")
            addOption("--no-check-certificate") // Evita problemas de SSL comunes en Android
        }

        return try {
            println("📥 Iniciando descarga en segundo plano para: $videoUrl")
            
            // 3. Ejecutar la descarga y enviar el progreso a la UI
            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, _ ->
                // Publicar el progreso intermedio (0 a 100)
                setProgressAsync(
                    workDataOf(
                        "PROGRESS" to progress.toInt(),
                        "ETA" to etaInSeconds
                    )
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Retornamos fallo indicando qué salió mal
            Result.failure(workDataOf("ERROR_MSG" to e.localizedMessage))
        }
    }
}
