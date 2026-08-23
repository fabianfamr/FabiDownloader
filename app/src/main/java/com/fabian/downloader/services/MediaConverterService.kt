package com.fabian.downloader.services

import android.content.Context
import android.util.Log
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.utils.PathUtils
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MediaConverterService(private val context: Context) {

    suspend fun convertRecord(
        record: DownloadRecord,
        targetFormat: String,
        database: AppDatabase
    ): Result<DownloadRecord> = withContext(Dispatchers.IO) {
        try {
            val inputFile = PathUtils.getDownloadFile(context, record.title, record.id, record.format)
            if (!inputFile.exists()) {
                return@withContext Result.failure(Exception(context.getString(com.fabian.downloader.R.string.downloads_convert_file_not_found)))
            }

            val targetExt = targetFormat.lowercase().trim()
            val outputFolder = PathUtils.getDownloadFolder(context, targetExt)
            val sanitizedTitle = PathUtils.sanitizeFileName(record.title).ifEmpty { "converted_${record.id}" }
            val fileNameWithoutExt = sanitizedTitle
            val expectedOutputFile = File(outputFolder, "$fileNameWithoutExt.$targetExt")

            if (expectedOutputFile.exists() && expectedOutputFile.absolutePath != inputFile.absolutePath) {
                expectedOutputFile.delete()
            }

            // Usar yt-dlp con su FFmpeg integrado para convertir el archivo local
            val request = YoutubeDLRequest("file://${inputFile.absolutePath}").apply {
                addOption("--enable-file-urls")
                addOption("--no-update")
                addOption("--no-warnings")
                when (targetExt) {
                    "mp3" -> {
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                        addOption("--audio-quality", "0")
                    }
                    "m4a" -> {
                        addOption("-x")
                        addOption("--audio-format", "m4a")
                    }
                    "aac" -> {
                        addOption("-x")
                        addOption("--audio-format", "aac")
                    }
                    "flac" -> {
                        addOption("-x")
                        addOption("--audio-format", "flac")
                    }
                    "opus" -> {
                        addOption("-x")
                        addOption("--audio-format", "opus")
                    }
                    "mp4" -> {
                        addOption("--recode-video", "mp4")
                    }
                    "mkv" -> {
                        addOption("--recode-video", "mkv")
                    }
                    else -> {
                        addOption("--recode-video", targetExt)
                    }
                }
                addOption("-o", "${outputFolder.absolutePath}/$fileNameWithoutExt.%(ext)s")
                addOption("--no-cache-dir")
            }

            val processId = UUID.randomUUID().toString()
            Log.d("MediaConverter", "Iniciando conversión con yt-dlp/FFmpeg para $targetExt...")

            try {
                YoutubeDL.getInstance().execute(request, processId) { progress, eta, line ->
                    Log.d("MediaConverter", "Progreso de conversión: $progress% - $line")
                }
            } finally {
                // Siempre destruir el proceso nativo (también en cancelación/excepción)
                // para evitar procesos FFmpeg huérfanos consumiendo CPU.
                try {
                    YoutubeDL.getInstance().destroyProcessById(processId)
                } catch (e: Exception) {
                    Log.w("MediaConverter", "Error al destruir el proceso de conversión $processId", e)
                }
            }

            var finalOutputFile = expectedOutputFile
            if (!finalOutputFile.exists()) {
                // Buscar si se generó con otra extensión de caso o sufijo
                val foundFile = outputFolder.listFiles { _, name ->
                    name.startsWith(fileNameWithoutExt) && name.endsWith(".$targetExt", ignoreCase = true)
                }?.firstOrNull()
                if (foundFile != null && foundFile.exists()) {
                    finalOutputFile = foundFile
                }
            }

            if (!finalOutputFile.exists() || finalOutputFile.length() <= 0L) {
                return@withContext Result.failure(Exception(context.getString(com.fabian.downloader.R.string.downloads_convert_failed, targetFormat)))
            }

            val sizeInBytes = finalOutputFile.length()
            val sizeMb = sizeInBytes / (1024.0 * 1024.0)
            val formattedSize = if (sizeMb >= 1024) {
                String.format(java.util.Locale.US, "%.2f GB", sizeMb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f MB", sizeMb)
            }

            // Actualizar registro en la base de datos
            database.downloadDao().updateDownloadFormatAndSize(
                id = record.id,
                format = targetExt.uppercase(),
                size = formattedSize
            )

            val updatedRecord = record.copy(
                format = targetExt.uppercase(),
                size = formattedSize
            )

            Log.i("MediaConverter", "Conversión completada con éxito para ${record.title} a $targetExt")
            Result.success(updatedRecord)
        } catch (e: Exception) {
            Log.e("MediaConverter", "Error en MediaConverterService: ${e.message}", e)
            Result.failure(e)
        }
    }
}
