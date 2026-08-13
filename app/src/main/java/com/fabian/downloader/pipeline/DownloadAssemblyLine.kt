package com.fabian.downloader.pipeline

import android.content.Context
import android.util.Log
import com.fabian.downloader.configs.Config
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.PathUtils
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/**
 * PLANTA DE MONTAJE DE DESCARGAS (Assembly Line Pattern)
 * 
 * Gestiona el flujo de trabajo de la descarga de forma secuencial y modular,
 * como una cinta transportadora donde en cada estación se añade o configura
 * una parte específica del proceso.
 */
object DownloadAssemblyLine {

    private const val TAG = "DownloadAssemblyLine"

    /**
     * ESTACIÓN 1: RECEPCIÓN Y LIMPIEZA DEL ENLACE
     * Extrae la URL válida de cualquier texto o enlace compartido recibido por la app.
     */
    fun station1_cleanUrl(rawUrl: String, keepPlaylistParams: Boolean = false): String {
        val trimmed = rawUrl.trim()
        val regex = Regex("""https?://[^\s]+""")
        var clean = regex.find(trimmed)?.value ?: trimmed
        
        try {
            val uri = android.net.Uri.parse(clean)
            if (uri.isHierarchical && uri.queryParameterNames.isNotEmpty()) {
                val trackingParams = setOf("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "si", "fbclid", "igshid", "feature")
                val isYoutube = com.fabian.downloader.utils.UrlUtils.isYoutubeUrl(clean)
                val ytParams = if (isYoutube && !keepPlaylistParams && !com.fabian.downloader.ui.AppSettings.playlistEnabled) setOf("t", "time_continue", "list", "index") else if (isYoutube) setOf("t", "time_continue") else emptySet()
                
                val builder = uri.buildUpon().clearQuery()
                for (param in uri.queryParameterNames) {
                    if (param != null && !trackingParams.contains(param.lowercase()) && !ytParams.contains(param.lowercase())) {
                        for (valStr in uri.getQueryParameters(param)) {
                            builder.appendQueryParameter(param, valStr)
                        }
                    }
                }
                clean = builder.build().toString()
            }
        } catch (_: Exception) {}
        
        if (clean.length > 8 && clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        
        Log.d(TAG, "Estación 1 (Recepción): Enlace procesado -> $clean")
        return clean
    }

    /**
     * ESTACIÓN 2: INYECCIÓN DE CONFIGURACIÓN Y PERSONALIZACIÓN (ENSAMBLADO)
     * Toma las preferencias del usuario guardadas en AppSettings y las adjunta
     * a la especificación de la descarga.
     */
    fun station2_assembleUserSettings(
        rawUrl: String,
        cleanUrl: String,
        requestedQuality: String,
        requestedFormat: String
    ): DownloadTaskSpec {
        val spec = DownloadTaskSpec(
            rawUrl = rawUrl,
            cleanUrl = cleanUrl,
            quality = requestedQuality.ifEmpty { AppSettings.selectedQuality },
            format = requestedFormat.ifEmpty { Config.FORMAT_MP4 },
            concurrentThreads = AppSettings.concurrentFragments,
            embedSubtitles = AppSettings.embedSubtitles,
            embedThumbnail = AppSettings.embedThumbnail,
            embedMetadata = AppSettings.embedMetadata,
            sponsorBlock = AppSettings.sponsorBlockEnabled,
            bypassGeo = AppSettings.bypassGeo,
            userAgent = AppSettings.customUserAgent,
            customArgs = AppSettings.customArguments
        )
        Log.d(TAG, "Estación 2 (Ensamblado de Configuración): $spec")
        return spec
    }

    /**
     * ESTACIÓN 3: ASIGNACIÓN DE ALMACENAMIENTO Y DESTINO
     * Prepara las rutas de salida en el almacenamiento local o tarjetas SD.
     */
    fun station3_assignDestination(
        context: Context,
        spec: DownloadTaskSpec,
        recordId: Long,
        title: String,
        thumbnailUrl: String?
    ): DownloadTaskSpec {
        val dir = PathUtils.getDownloadFolder(context, spec.format)
        val file = PathUtils.getDownloadFile(context, title, recordId, spec.format)
        
        val updatedSpec = spec.copy(
            recordId = recordId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            outputDirectory = dir,
            outputFile = file
        )
        Log.d(TAG, "Estación 3 (Asignación de Destino): Archivo de salida -> ${file.absolutePath}")
        return updatedSpec
    }

    /**
     * ESTACIÓN 5: CONTROL DE CALIDAD Y ESCANEO EN MEDIASTORE (ENTREGA FINAL)
     * Verifica que el archivo exista en disco y solicita el escaneo a Android MediaScanner.
     */
    fun station5_verifyAndDeliver(context: Context, outputFile: File?): Boolean {
        if (outputFile == null || !outputFile.exists()) {
            Log.e(TAG, "Estación 5 (Control de Calidad): El archivo no existe tras la descarga")
            return false
        }
        
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                null
            ) { path, uri ->
                Log.d(TAG, "Estación 5 (Entrega Final): Archivo escaneado $path -> Uri: $uri")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error en Estación 5 durante el escaneo de medios", e)
            return true // El archivo existe de todos modos
        }
    }
}
