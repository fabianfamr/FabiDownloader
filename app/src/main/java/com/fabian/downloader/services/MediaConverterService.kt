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

    /**
     * Convierte un archivo de medios local a otro formato y opcionalmente re-escala su resolución
     * de video o ajusta su tasa de bits de audio usando FFmpeg integrado (estilo Snaptube).
     */
    suspend fun convertFile(
        inputFile: File,
        outputFolder: File,
        fileNameWithoutExt: String,
        targetExt: String,
        targetHeight: Int? = null,
        targetBitrate: String? = null,
        processId: String = UUID.randomUUID().toString(),
        onProgress: ((progress: Float, line: String) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!inputFile.exists() || inputFile.length() <= 0L) {
                return@withContext Result.failure(Exception(context.getString(com.fabian.downloader.R.string.downloads_convert_file_not_found)))
            }

            val cleanTargetExt = targetExt.lowercase().trim()
            if (!outputFolder.exists()) {
                outputFolder.mkdirs()
            }

            val expectedOutputFile = File(outputFolder, "$fileNameWithoutExt.$cleanTargetExt")
            if (expectedOutputFile.exists() && expectedOutputFile.absolutePath != inputFile.absolutePath) {
                expectedOutputFile.delete()
            }

            val request = YoutubeDLRequest("file://${inputFile.absolutePath}").apply {
                addOption("--enable-file-urls")
                addOption("--no-update")
                addOption("--no-warnings")
                when (cleanTargetExt) {
                    "mp3" -> {
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                        val bitrateDigits = targetBitrate?.filter { it.isDigit() }
                        val qualityArg = if (!bitrateDigits.isNullOrEmpty()) "${bitrateDigits}k" else "0"
                        addOption("--audio-quality", qualityArg)
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
                        if (targetHeight != null && targetHeight > 0) {
                            addOption("--postprocessor-args", "FFmpegVideoConvertor:-vf scale=-2:$targetHeight -c:a copy")
                        }
                    }
                    "mkv" -> {
                        addOption("--recode-video", "mkv")
                        if (targetHeight != null && targetHeight > 0) {
                            addOption("--postprocessor-args", "FFmpegVideoConvertor:-vf scale=-2:$targetHeight -c:a copy")
                        }
                    }
                    else -> {
                        addOption("--recode-video", cleanTargetExt)
                        if (targetHeight != null && targetHeight > 0) {
                            addOption("--postprocessor-args", "FFmpegVideoConvertor:-vf scale=-2:$targetHeight -c:a copy")
                        }
                    }
                }
                addOption("-o", "${outputFolder.absolutePath}/$fileNameWithoutExt.%(ext)s")
                addOption("--no-cache-dir")
            }

            Log.d("MediaConverter", "Iniciando conversión de ${inputFile.name} a $cleanTargetExt (height=$targetHeight)...")

            try {
                YoutubeDL.getInstance().execute(request, processId) { progress, eta, line ->
                    onProgress?.invoke(progress, line ?: "")
                    Log.d("MediaConverter", "Progreso de conversión: $progress% - $line")
                }
            } finally {
                try {
                    YoutubeDL.getInstance().destroyProcessById(processId)
                } catch (e: Exception) {
                    Log.w("MediaConverter", "Error al destruir proceso de conversión $processId", e)
                }
            }

            var finalOutputFile = expectedOutputFile
            if (!finalOutputFile.exists()) {
                val foundFile = outputFolder.listFiles { _, name ->
                    name.startsWith(fileNameWithoutExt) && name.endsWith(".$cleanTargetExt", ignoreCase = true)
                }?.firstOrNull()
                if (foundFile != null && foundFile.exists()) {
                    finalOutputFile = foundFile
                }
            }

            if (!finalOutputFile.exists() || finalOutputFile.length() <= 0L) {
                return@withContext Result.failure(Exception(context.getString(com.fabian.downloader.R.string.downloads_convert_failed, targetExt)))
            }

            Log.i("MediaConverter", "Conversión exitosa: ${finalOutputFile.absolutePath} (${finalOutputFile.length()} bytes)")
            Result.success(finalOutputFile)
        } catch (e: Exception) {
            Log.e("MediaConverter", "Error convirtiendo archivo: ${e.message}", e)
            Result.failure(e)
        }
    }

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

            val fileResult = convertFile(
                inputFile = inputFile,
                outputFolder = outputFolder,
                fileNameWithoutExt = sanitizedTitle,
                targetExt = targetExt,
                targetBitrate = if (targetExt in listOf("mp3", "m4a", "aac", "flac", "opus")) record.quality else null,
                processId = UUID.randomUUID().toString()
            )

            if (fileResult.isFailure) {
                return@withContext Result.failure(fileResult.exceptionOrNull() ?: Exception("Error en conversión"))
            }

            val finalOutputFile = fileResult.getOrThrow()
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
