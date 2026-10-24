package com.fabian.downloader.services

import android.util.Log
import com.fabian.downloader.configs.Config
import com.fabian.downloader.network.NetworkClient
import com.fabian.downloader.utils.PathUtils
import com.fabian.downloader.utils.YtdlpParser
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.Locale

/**
 * Motor de descarga segmentada multihilo (Chunked Parallel Downloader) inspirado en la
 * arquitectura de SnapTube.
 *
 * Características:
 *  - Detección de soporte HTTP 'Range: bytes' vía HEAD / GET preliminar.
 *  - División dinámica del archivo en 4 a 8 segmentos concurrentes según tamaño.
 *  - Escritura directa por desplazamiento (seek offset) en RandomAccessFile.
 *  - Medición instantánea y suavizada de velocidad (MB/s o KB/s).
 *  - Soporte de cancelación inmediata y limpieza de recursos.
 *  - Fallback automático a descarga en flujo único con buffer de 64KB si el servidor no admite rangos.
 */
object SnapChunkDownloader {
    private const val TAG = "SnapChunkDownloader"
    private val client: OkHttpClient = NetworkClient.okHttpClient
    private val activeTasks = ConcurrentHashMap<String, AtomicBoolean>()

    fun cancelTask(processId: String) {
        activeTasks[processId]?.set(true)
    }

    /**
     * Descarga un archivo multimedia directamente desde una URL HTTP/HTTPS utilizando
     * múltiples conexiones paralelas aceleradas.
     */
    suspend fun downloadDirectMedia(
        mediaUrl: String,
        destFolder: File,
        fileNameWithoutExt: String,
        format: String,
        processId: String,
        customHeaders: Map<String, String> = emptyMap(),
        onProgress: (progress: Float, sizeText: String, speedText: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val isCancelled = AtomicBoolean(false)
        activeTasks[processId] = isCancelled

        val targetExt = format.lowercase().ifEmpty { "mp4" }
        val tempFile = File(destFolder, "$fileNameWithoutExt.downloading")
        val finalFile = File(destFolder, "$fileNameWithoutExt.$targetExt")

        try {
            if (!destFolder.exists()) {
                destFolder.mkdirs()
            }

            // Paso 1: Petición de sondeo para conocer tamaño y soporte de rangos
            val probeRequest = Request.Builder()
                .url(mediaUrl)
                .head()
                .apply {
                    customHeaders.forEach { (k, v) -> addHeader(k, v) }
                    if (!customHeaders.containsKey("User-Agent")) {
                        addHeader("User-Agent", Config.UA_DESKTOP)
                    }
                }
                .build()

            var totalBytes = -1L
            var acceptsRanges = false

            try {
                client.newCall(probeRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        totalBytes = response.header("Content-Length")?.toLongOrNull() ?: -1L
                        val rangeHeader = response.header("Accept-Ranges")
                        acceptsRanges = rangeHeader?.equals("bytes", ignoreCase = true) == true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sondeo HEAD falló para $mediaUrl: ${e.message}. Probando GET directo...")
            }

            // Si HEAD no devolvió Content-Length, probar con GET Range 0-0
            if (totalBytes <= 0L) {
                try {
                    val testRangeReq = Request.Builder()
                        .url(mediaUrl)
                        .addHeader("Range", "bytes=0-0")
                        .apply {
                            customHeaders.forEach { (k, v) -> addHeader(k, v) }
                            if (!customHeaders.containsKey("User-Agent")) {
                                addHeader("User-Agent", Config.UA_DESKTOP)
                            }
                        }
                        .build()
                    client.newCall(testRangeReq).execute().use { response ->
                        if (response.code == 206) {
                            acceptsRanges = true
                            val contentRange = response.header("Content-Range")
                            if (contentRange != null && contentRange.contains("/")) {
                                totalBytes = contentRange.substringAfterLast("/").trim().toLongOrNull() ?: -1L
                            }
                        } else if (response.isSuccessful) {
                            totalBytes = response.header("Content-Length")?.toLongOrNull() ?: -1L
                        }
                    }
                } catch (_: Exception) {}
            }

            // Si soporta rangos y el tamaño es considerable (> 2 MB), usamos descarga multihilo
            val canUseMultiThreading = acceptsRanges && totalBytes > 2 * 1024 * 1024
            val numThreads = when {
                totalBytes > 60 * 1024 * 1024 -> 8
                totalBytes > 15 * 1024 * 1024 -> 6
                canUseMultiThreading -> 4
                else -> 1
            }

            Log.i(TAG, "Iniciando descarga con SnapChunkDownloader: hilos=$numThreads, totalBytes=$totalBytes, ranges=$acceptsRanges")

            val success = if (canUseMultiThreading && numThreads > 1) {
                downloadMultiThreaded(
                    mediaUrl = mediaUrl,
                    totalBytes = totalBytes,
                    numThreads = numThreads,
                    tempFile = tempFile,
                    customHeaders = customHeaders,
                    isCancelled = isCancelled,
                    onProgress = onProgress
                )
            } else {
                downloadSingleStream(
                    mediaUrl = mediaUrl,
                    knownTotalBytes = totalBytes,
                    tempFile = tempFile,
                    customHeaders = customHeaders,
                    isCancelled = isCancelled,
                    onProgress = onProgress
                )
            }

            if (!success || isCancelled.get()) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext false
            }

            // Finalización exitosa: mover archivo temporal al destino
            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = tempFile.renameTo(finalFile)
            if (!renamed) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(100f, YtdlpParser.formatSize(formatBytes(finalFile.length())), Config.STATUS_FINALIZING)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error en SnapChunkDownloader: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            false
        } finally {
            activeTasks.remove(processId)
        }
    }

    private suspend fun downloadMultiThreaded(
        mediaUrl: String,
        totalBytes: Long,
        numThreads: Int,
        tempFile: File,
        customHeaders: Map<String, String>,
        isCancelled: AtomicBoolean,
        onProgress: (Float, String, String) -> Unit
    ): Boolean = coroutineScope {
        val raf = RandomAccessFile(tempFile, "rw")
        raf.setLength(totalBytes)

        val chunkSize = totalBytes / numThreads
        val downloadedBytes = AtomicLong(0L)
        val lastProgressBytes = AtomicLong(0L)
        var lastUpdateTime = System.currentTimeMillis()

        val jobs = mutableListOf<Job>()
        val fileLock = Any()

        for (i in 0 until numThreads) {
            val startByte = i * chunkSize
            val endByte = if (i == numThreads - 1) totalBytes - 1 else (startByte + chunkSize - 1)

            val job = launch(Dispatchers.IO) {
                var currentStart = startByte
                val request = Request.Builder()
                    .url(mediaUrl)
                    .addHeader("Range", "bytes=$currentStart-$endByte")
                    .apply {
                        customHeaders.forEach { (k, v) -> addHeader(k, v) }
                        if (!customHeaders.containsKey("User-Agent")) {
                            addHeader("User-Agent", Config.UA_DESKTOP)
                        }
                    }
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw IllegalStateException("HTTP ${response.code} en segmento $i")
                    }
                    val body = response.body ?: throw IllegalStateException("Cuerpo vacío en segmento $i")
                    val inputStream = body.byteStream()
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled.get() || !isActive) {
                            throw CancellationException("Cancelado por usuario")
                        }
                        synchronized(fileLock) {
                            raf.seek(currentStart)
                            raf.write(buffer, 0, bytesRead)
                        }
                        currentStart += bytesRead
                        downloadedBytes.addAndGet(bytesRead.toLong())

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime >= 250) {
                            val elapsedSec = (now - lastUpdateTime) / 1000.0
                            val currentTotal = downloadedBytes.get()
                            val deltaBytes = currentTotal - lastProgressBytes.get()
                            lastProgressBytes.set(currentTotal)
                            lastUpdateTime = now

                            val speedBps = if (elapsedSec > 0) (deltaBytes / elapsedSec).toLong() else 0L
                            val speedStr = formatSpeed(speedBps)
                            val pct = ((currentTotal.toDouble() / totalBytes) * 100.0).toFloat().coerceIn(0f, 99.5f)
                            val sizeStr = "${formatBytes(currentTotal)} / ${formatBytes(totalBytes)}"

                            onProgress(pct, sizeStr, speedStr)
                        }
                    }
                }
            }
            jobs.add(job)
        }

        try {
            jobs.joinAll()
            raf.close()
            !isCancelled.get()
        } catch (e: Exception) {
            try { raf.close() } catch (_: Exception) {}
            jobs.forEach { it.cancel() }
            false
        }
    }

    private suspend fun downloadSingleStream(
        mediaUrl: String,
        knownTotalBytes: Long,
        tempFile: File,
        customHeaders: Map<String, String>,
        isCancelled: AtomicBoolean,
        onProgress: (Float, String, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(mediaUrl)
            .apply {
                customHeaders.forEach { (k, v) -> addHeader(k, v) }
                if (!customHeaders.containsKey("User-Agent")) {
                    addHeader("User-Agent", Config.UA_DESKTOP)
                }
            }
            .build()

        var downloadedBytes = 0L
        var lastUpdateTime = System.currentTimeMillis()
        var lastProgressBytes = 0L

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val totalBytes = if (knownTotalBytes > 0) knownTotalBytes else body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = tempFile.outputStream().buffered(64 * 1024)

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int

                outputStream.use { out ->
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled.get() || !isActive) {
                            return@withContext false
                        }
                        out.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime >= 250) {
                            val elapsedSec = (now - lastUpdateTime) / 1000.0
                            val deltaBytes = downloadedBytes - lastProgressBytes
                            lastProgressBytes = downloadedBytes
                            lastUpdateTime = now

                            val speedBps = if (elapsedSec > 0) (deltaBytes / elapsedSec).toLong() else 0L
                            val speedStr = formatSpeed(speedBps)
                            val pct = if (totalBytes > 0) {
                                ((downloadedBytes.toDouble() / totalBytes) * 100.0).toFloat().coerceIn(0f, 99.5f)
                            } else {
                                -1f
                            }
                            val sizeStr = if (totalBytes > 0) {
                                "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
                            } else {
                                formatBytes(downloadedBytes)
                            }

                            onProgress(pct, sizeStr, speedStr)
                        }
                    }
                    out.flush()
                }
            }
            !isCancelled.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error en flujo único: ${e.message}", e)
            false
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0.0 KB/s"
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB/s", mb)
        } else {
            String.format(Locale.US, "%.0f KB/s", kb)
        }
    }
}
