package com.fabian.downloader.pipeline

import android.content.Context
import com.fabian.downloader.configs.Config
import com.fabian.downloader.ui.AppSettings
import java.io.File

/**
 * Representa la especificación de una descarga ("El Chasis") a medida que recorre
 * las estaciones de la planta de montaje / cinta transportadora de descargas.
 */
data class DownloadTaskSpec(
    val rawUrl: String,
    val cleanUrl: String = "",
    val recordId: Long = 0L,
    
    // Configuración ensamblada (Estación 2)
    val quality: String = "720p",
    val format: String = Config.FORMAT_MP4,
    val concurrentThreads: String = "1",
    val embedSubtitles: Boolean = false,
    val embedThumbnail: Boolean = false,
    val embedMetadata: Boolean = false,
    val sponsorBlock: Boolean = false,
    val bypassGeo: Boolean = false,
    val userAgent: String = "",
    val customArgs: String = "",
    
    // Metadatos y Destino (Estación 3)
    val title: String = "Descargando...",
    val thumbnailUrl: String? = null,
    val outputDirectory: File? = null,
    val outputFile: File? = null
)
