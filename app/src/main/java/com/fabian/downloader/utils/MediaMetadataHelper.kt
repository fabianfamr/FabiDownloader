package com.fabian.downloader.utils

import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File

object MediaMetadataHelper {
    private const val TAG = "MediaMetadataHelper"

    /**
     * Extrae la resolución real del video (ancho x alto), considerando la rotación.
     */
    fun getVideoResolution(file: File): Pair<Int, Int>? {
        if (!file.exists() || file.length() <= 0L) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (width != null && height != null && width > 0 && height > 0) {
                if (rotation == 90 || rotation == 270) {
                    Pair(height, width)
                } else {
                    Pair(width, height)
                }
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer resolución de video para ${file.name}: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Verifica si el archivo contiene una pista de video decodificable.
     */
    fun hasVideoStream(file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            hasVideo.equals("yes", ignoreCase = true)
        } catch (e: Exception) {
            false
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Verifica si el archivo contiene pista de audio decodificable.
     */
    fun hasAudioStream(file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            hasAudio.equals("yes", ignoreCase = true)
        } catch (e: Exception) {
            false
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
