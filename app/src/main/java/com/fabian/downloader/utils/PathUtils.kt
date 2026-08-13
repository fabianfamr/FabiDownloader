package com.fabian.downloader.utils

import android.content.Context
import android.os.Environment
import com.fabian.downloader.configs.Config
import java.io.File

@Suppress("DEPRECATION")
object PathUtils {
    private val cachedFolders = mutableMapOf<String, File>()

    private fun isWritableDir(dir: File): Boolean {
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (!dir.exists()) return false
            val testFile = File(dir, ".test_write_${System.currentTimeMillis()}.tmp")
            if (testFile.createNewFile()) {
                testFile.delete()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun saveThumbnail(context: Context, url: String?, id: Long): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (url.isNullOrEmpty()) return@withContext null
        if (url.startsWith("file://") || url.startsWith("/")) return@withContext url
        
        try {
            val thumbnailsDir = File(context.filesDir, "thumbnails")
            if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
            
            val destFile = File(thumbnailsDir, "thumb_$id.jpg")
            if (destFile.exists()) return@withContext destFile.absolutePath
 
            val connection = java.net.URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.getInputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext destFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e(Config.TAG_PATH_UTILS, "Error saving thumbnail", e)
            return@withContext url // fallback to url
        }
    }

    private fun resolvePhysicalPathFromUri(context: Context, uriString: String): File? {
        if (!uriString.startsWith("content://")) return null
        try {
            val uri = android.net.Uri.parse(uriString)
            if ("com.android.externalstorage.documents" == uri.authority) {
                val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val relativePath = java.net.URLDecoder.decode(split[1], "UTF-8")
                    val baseDir = if ("primary".equals(type, ignoreCase = true)) {
                        Environment.getExternalStorageDirectory()
                    } else {
                        File("/storage/$type")
                    }
                    return File(baseDir, relativePath)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(Config.TAG_PATH_UTILS, "Error resolving SAF Uri: ${e.message}", e)
        }
        return null
    }
 
    fun getDownloadFolder(context: Context, format: String): File {
        val isVideo = format.equals(Config.FORMAT_MP4, ignoreCase = true) || format.equals(Config.FORMAT_WEBM, ignoreCase = true)
        val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || format.equals(Config.FORMAT_PNG, ignoreCase = true) || format.equals(Config.FORMAT_WEBP, ignoreCase = true) || format.equals("JPEG", ignoreCase = true)
        val relativeSubfolder = when {
            isVideo -> "${Config.APP_NAME}/video"
            isImage -> "${Config.APP_NAME}/image"
            else -> "${Config.APP_NAME}/audio"
        }
        val subfolderName = when {
            isVideo -> "video"
            isImage -> "image"
            else -> "audio"
        }
        
        cachedFolders[relativeSubfolder]?.let {
            if (it.exists()) return it
        }
 
        // 1. Intentar usar la ubicación configurada por el usuario (SAF Uri o ruta física)
        val locationSetting = com.fabian.downloader.ui.AppSettings.downloadLocation
        var configuredDir: File? = null

        if (locationSetting.startsWith("content://")) {
            configuredDir = resolvePhysicalPathFromUri(context, locationSetting)
        } else if (locationSetting.isNotEmpty()) {
            configuredDir = if (locationSetting.startsWith("/")) {
                File(locationSetting)
            } else if (locationSetting.startsWith("Downloads/", ignoreCase = true) || locationSetting.startsWith("Download/", ignoreCase = true)) {
                val rel = locationSetting.substringAfter("/")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), rel)
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), locationSetting)
            }
        }

        if (configuredDir != null) {
            val finalFolder = if (configuredDir.name.equals(subfolderName, ignoreCase = true)) {
                configuredDir
            } else {
                File(configuredDir, subfolderName)
            }
            if (isWritableDir(finalFolder)) {
                android.util.Log.d(Config.TAG_PATH_UTILS, "Successfully verified configured folder: ${finalFolder.absolutePath}")
                cachedFolders[relativeSubfolder] = finalFolder
                return finalFolder
            } else {
                android.util.Log.e(Config.TAG_PATH_UTILS, "Configured folder ${finalFolder.absolutePath} is NOT writable")
            }
        }

        // 2. Fallback 1: Carpeta pública estándar de descargas: Downloads/FabiDownloader/video o audio
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null) {
            val downloadFabiFolder = File(publicDownloads, relativeSubfolder)
            if (isWritableDir(downloadFabiFolder)) {
                android.util.Log.d(Config.TAG_PATH_UTILS, "Successfully verified public Download folder: ${downloadFabiFolder.absolutePath}")
                cachedFolders[relativeSubfolder] = downloadFabiFolder
                return downloadFabiFolder
            } else {
                android.util.Log.e(Config.TAG_PATH_UTILS, "Public Download folder is NOT writable: ${downloadFabiFolder.absolutePath}")
            }

            // Fallback 2: Usar directamente la carpeta raíz pública Downloads (sin subcarpetas com.fabian.downloader)
            if (isWritableDir(publicDownloads)) {
                android.util.Log.d(Config.TAG_PATH_UTILS, "Using raw public Downloads directory: ${publicDownloads.absolutePath}")
                cachedFolders[relativeSubfolder] = publicDownloads
                return publicDownloads
            } else {
                android.util.Log.e(Config.TAG_PATH_UTILS, "Raw public Downloads directory is NOT writable: ${publicDownloads.absolutePath}")
            }
        }

        // 3. Recursos de última y absoluta emergencia (contienen el paquete de la app, pero evitan crasheos)
        val mediaDirs = context.externalMediaDirs
        for (mediaDir in mediaDirs) {
            if (mediaDir == null) continue
            val targetFolder = File(mediaDir, relativeSubfolder)
            if (isWritableDir(targetFolder)) {
                android.util.Log.w(Config.TAG_PATH_UTILS, "FALLBACK to externalMediaDirs: ${targetFolder.absolutePath}")
                cachedFolders[relativeSubfolder] = targetFolder
                return targetFolder
            }
        }
 
        val appExternalDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (appExternalDownloadDir != null) {
            val targetFolder = File(appExternalDownloadDir, relativeSubfolder)
            try {
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }
                val testFile = File(targetFolder, ".test_write_${System.currentTimeMillis()}")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    android.util.Log.w(Config.TAG_PATH_UTILS, "FALLBACK to appExternalDownloadDir: ${targetFolder.absolutePath}")
                    cachedFolders[relativeSubfolder] = targetFolder
                    return targetFolder
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        val fallbackFolder = File(context.filesDir, relativeSubfolder)
        if (!fallbackFolder.exists()) {
            fallbackFolder.mkdirs()
        }
        android.util.Log.w(Config.TAG_PATH_UTILS, "FALLBACK to private storage! ${fallbackFolder.absolutePath}")
        cachedFolders[relativeSubfolder] = fallbackFolder
        return fallbackFolder
    }

    fun sanitizeFileName(title: String): String {
        var sanitized = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        // Remove control characters and leading/trailing dots/spaces (Windows doesn't like them)
        sanitized = sanitized.replace(Regex("[\\x00-\\x1f]"), "").trim().trim('.')
        if (sanitized.isEmpty()) {
            sanitized = "download"
        }
        // Avoid Windows-reserved names (in case of cloud sync to Windows)
        val nameWithoutExt = sanitized.substringBeforeLast('.')
        if (Config.RESERVED_FILENAMES.contains(nameWithoutExt.uppercase())) {
            sanitized = "_" + sanitized
        }
        // Limit total length to avoid filesystem issues
        if (sanitized.length > Config.MAX_FILENAME_LENGTH) {
            val ext = sanitized.substringAfterLast('.', "")
            val namePart = sanitized.substringBeforeLast('.')
            sanitized = if (ext.isNotEmpty()) {
                namePart.take(Config.MAX_FILENAME_LENGTH - ext.length - 1) + "." + ext
            } else {
                namePart.take(Config.MAX_FILENAME_LENGTH)
            }
        }
        return sanitized
    }

    fun getDownloadFile(context: Context, title: String, id: Long, format: String): File {
        val baseFolder = getDownloadFolder(context, format)
        val sanitizedTitle = sanitizeFileName(title)
        
        // 1. Try new path with ID
        var file = File(baseFolder, "${sanitizedTitle}_$id.${format.lowercase()}")
        if (file.exists()) return file
        
        // 2. Try new path without ID
        file = File(baseFolder, "$sanitizedTitle.${format.lowercase()}")
        if (file.exists()) return file
        
        // 3. Fallback search in all possible historical locations (including legacy names)
        val storageRoot = Environment.getExternalStorageDirectory()
        val isVideo = format.equals(Config.FORMAT_MP4, ignoreCase = true) || format.equals(Config.FORMAT_WEBM, ignoreCase = true)
        val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || format.equals(Config.FORMAT_PNG, ignoreCase = true) || format.equals(Config.FORMAT_WEBP, ignoreCase = true) || format.equals("JPEG", ignoreCase = true)
        
        // We support both FabiDownloader and Fabidownloader
        val subFolders = when {
            isVideo -> listOf("${Config.APP_NAME}/video", "${Config.APP_NAME_LOWER.capitalize()}/video")
            isImage -> listOf("${Config.APP_NAME}/image", "${Config.APP_NAME_LOWER.capitalize()}/image")
            else -> listOf("${Config.APP_NAME}/audio", "${Config.APP_NAME_LOWER.capitalize()}/audio")
        }
        
        val folderList = mutableListOf<File>()
        
        subFolders.forEach { sub ->
            // Public downloads
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDir != null) {
                folderList.add(File(publicDir, sub))
            }
            // Android/media
            context.externalMediaDirs.forEach { dir ->
                if (dir != null) {
                    folderList.add(File(dir, sub))
                }
            }
            // Roots / Storage paths
            if (storageRoot != null) {
                folderList.add(File(storageRoot, "Android/$sub"))
                folderList.add(File(storageRoot, sub))
            }
            // App files directories
            folderList.add(File(context.getExternalFilesDir(null) ?: context.filesDir, sub))
        }
        
        val legacyFilesDir = context.getExternalFilesDir(null)
        if (legacyFilesDir != null) {
            folderList.add(legacyFilesDir)
        }
        folderList.add(context.filesDir)

        for (folder in folderList) {
            val fWithId = File(folder, "${sanitizedTitle}_$id.${format.lowercase()}")
            if (fWithId.exists()) return fWithId
            val fNoId = File(folder, "$sanitizedTitle.${format.lowercase()}")
            if (fNoId.exists()) return fNoId
        }
        
        // Default to the preferred folder
        return File(baseFolder, "${sanitizedTitle}_$id.${format.lowercase()}")
    }
}
