package com.fabian.downloader.services

/**
 * Excepción para errores relacionados con espacio de almacenamiento.
 *
 * Permite diferenciar en el catch block de `DownloadExecutor.runDownload`
 * entre "no hay espacio" y otros errores genéricos de descarga.
 */
class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Excepción para errores genéricos de descarga.
 */
class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Excepción para errores durante la conversión de formato (FFmpeg).
 */
class ConversionException(message: String, cause: Throwable? = null) : Exception(message, cause)
