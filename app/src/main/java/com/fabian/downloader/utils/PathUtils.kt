package com.fabian.downloader.utils

import android.content.Context
import android.os.Environment
import com.fabian.downloader.configs.Config
import java.io.File

@Suppress("DEPRECATION")
object PathUtils {
    private val cachedFolders = java.util.concurrent.ConcurrentHashMap<String, File>()

    fun clearFolderCache() {
        cachedFolders.clear()
    }

    fun ensureFabiDirectories(context: Context) {
        try {
            val root = getRootFolder(context)
            if (!root.exists()) root.mkdirs()
            
            val downloadsDir = File(root, "downloads")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            
            val videoDir = File(downloadsDir, "video")
            if (!videoDir.exists()) videoDir.mkdirs()
            
            val audioDir = File(downloadsDir, "audio")
            if (!audioDir.exists()) audioDir.mkdirs()
            
            val imageDir = File(downloadsDir, "image")
            if (!imageDir.exists()) imageDir.mkdirs()
            
            val dbDir = File(root, "db")
            if (!dbDir.exists()) dbDir.mkdirs()
        } catch (e: Exception) {
            android.util.Log.e(Config.TAG_PATH_UTILS, "Error creando estructura de carpetas FabiDownloader", e)
        }
    }

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

    fun getRootFolder(context: Context): File {
        // 1. Ubicación estándar en la carpeta pública de descargas: /storage/emulated/0/Download/FabiDownloader
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null) {
            val fabiDownloadRoot = File(publicDownloads, Config.PATH_ROOT_FOLDER)
            if (isWritableDir(fabiDownloadRoot)) {
                return fabiDownloadRoot
            }
        }

        // 2. Raíz de almacenamiento si está disponible y escribible
        val storageRoot = Environment.getExternalStorageDirectory()
        if (storageRoot != null) {
            val fabiRoot = File(storageRoot, Config.PATH_ROOT_FOLDER)
            if (isWritableDir(fabiRoot)) {
                return fabiRoot
            }
        }

        // Fallback 1: externalMediaDirs
        for (mediaDir in context.externalMediaDirs) {
            if (mediaDir != null) {
                val target = File(mediaDir, Config.PATH_ROOT_FOLDER)
                if (isWritableDir(target)) return target
            }
        }
        // Fallback 2: getExternalFilesDir
        val appExt = context.getExternalFilesDir(null)
        if (appExt != null) {
            val target = File(appExt, Config.PATH_ROOT_FOLDER)
            if (isWritableDir(target)) return target
        }
        // Fallback 3: filesDir
        val internalRoot = File(context.filesDir, Config.PATH_ROOT_FOLDER)
        if (!internalRoot.exists()) internalRoot.mkdirs()
        return internalRoot
    }

    fun getDbFolder(context: Context): File {
        val internalDbDir = context.getDatabasePath("temp").parentFile ?: File(context.filesDir, "databases")
        if (!internalDbDir.exists()) internalDbDir.mkdirs()
        return internalDbDir
    }

    fun getDatabaseFile(context: Context): File {
        val internalDbFile = context.getDatabasePath(Config.DB_NAME)
        val internalDbFolder = internalDbFile.parentFile ?: File(context.filesDir, "databases")
        if (!internalDbFolder.exists()) internalDbFolder.mkdirs()

        if (!internalDbFile.exists() || internalDbFile.length() == 0L) {
            // Attempt to migrate from legacy external storage locations to internal DB
            val legacyPaths = mutableListOf<File>()
            
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads != null) {
                legacyPaths.add(File(publicDownloads, "${Config.PATH_ROOT_FOLDER}/db/${Config.DB_NAME}"))
            }
            
            val storageRoot = Environment.getExternalStorageDirectory()
            if (storageRoot != null) {
                legacyPaths.add(File(storageRoot, "${Config.PATH_ROOT_FOLDER}/db/${Config.DB_NAME}"))
            }
            
            val appExt = context.getExternalFilesDir(null)
            if (appExt != null) {
                legacyPaths.add(File(appExt, "${Config.PATH_ROOT_FOLDER}/db/${Config.DB_NAME}"))
            }
            
            var externalLegacyDb: File? = null
            for (path in legacyPaths) {
                if (path.exists() && path.length() > 0) {
                    externalLegacyDb = path
                    break
                }
            }

            if (externalLegacyDb != null) {
                try {
                    externalLegacyDb.copyTo(internalDbFile, overwrite = true)
                    
                    val extWal = File(externalLegacyDb.parentFile, "${Config.DB_NAME}-wal")
                    if (extWal.exists()) extWal.copyTo(File(internalDbFolder, "${Config.DB_NAME}-wal"), overwrite = true)
                    
                    val extShm = File(externalLegacyDb.parentFile, "${Config.DB_NAME}-shm")
                    if (extShm.exists()) extShm.copyTo(File(internalDbFolder, "${Config.DB_NAME}-shm"), overwrite = true)
                    
                    android.util.Log.i(Config.TAG_PATH_UTILS, "Migrated legacy database from ${externalLegacyDb.absolutePath} to internal storage")
                } catch (e: Exception) {
                    android.util.Log.e(Config.TAG_PATH_UTILS, "Error migrating legacy database to internal storage", e)
                }
            }
        }
        
        return internalDbFile
    }

    fun migrateOldStructureIfNeeded(context: Context) {
        try {
            val root = getRootFolder(context)
            val targetDownloadsDir = File(root, "downloads")
            if (!targetDownloadsDir.exists()) {
                targetDownloadsDir.mkdirs()
            }

            val oldCandidateDirs = listOf(
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Config.APP_NAME),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Config.APP_NAME_LOWER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Download/${Config.APP_NAME}")
            )

            for (oldDir in oldCandidateDirs) {
                if (oldDir.exists() && oldDir.isDirectory && oldDir.absolutePath != targetDownloadsDir.absolutePath) {
                    moveDirectoryContents(oldDir, targetDownloadsDir)
                    if (oldDir.listFiles().isNullOrEmpty()) {
                        oldDir.delete()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(Config.TAG_PATH_UTILS, "Error migrating old download folder", e)
        }
    }

    private fun moveDirectoryContents(source: File, destination: File) {
        if (!destination.exists()) destination.mkdirs()
        source.listFiles()?.forEach { file ->
            val destFile = File(destination, file.name)
            if (file.isDirectory) {
                moveDirectoryContents(file, destFile)
                if (file.listFiles().isNullOrEmpty()) {
                    file.delete()
                }
            } else {
                if (!destFile.exists()) {
                    val renamed = file.renameTo(destFile)
                    if (!renamed) {
                        try {
                            file.copyTo(destFile, overwrite = true)
                            file.delete()
                        } catch (_: Exception) {}
                    }
                }
            }
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

    fun getDisplayDownloadLocation(locationSetting: String = com.fabian.downloader.ui.AppSettings.downloadLocation): String {
        if (locationSetting.isEmpty() || locationSetting == Config.PATH_DOWNLOAD_LOCATION_DEFAULT || locationSetting.equals("downloads", ignoreCase = true)) {
            return "Descargas > FabiDownloader"
        }
        if (locationSetting.startsWith("content://")) {
            try {
                val decoded = android.net.Uri.decode(locationSetting)
                val treePart = decoded.substringAfter("tree/", "").substringAfter("document/", "")
                if (treePart.contains(":")) {
                    val split = treePart.split(":", limit = 2)
                    val type = split[0]
                    val relPath = if (split.size > 1) split[1].trimStart('/') else ""
                    val prefix = if ("primary".equals(type, ignoreCase = true)) "Almacenamiento interno" else "Tarjeta SD"
                    return if (relPath.isNotEmpty()) "$prefix > $relPath" else prefix
                }
            } catch (e: Exception) {
                android.util.Log.e(Config.TAG_PATH_UTILS, "Error decoding SAF uri for display: ${e.message}")
            }
            return "Descargas > FabiDownloader"
        }
        if (locationSetting.startsWith("/storage/emulated/0/")) {
            val rel = locationSetting.removePrefix("/storage/emulated/0/").trim('/')
            return if (rel.isNotEmpty()) "Almacenamiento interno > $rel" else "Almacenamiento interno"
        }
        if (locationSetting.startsWith("/")) {
            return locationSetting
        }
        val cleanRel = locationSetting.trim('/')
        return "Descargas > FabiDownloader/$cleanRel"
    }
 
    fun getDownloadFolder(context: Context, format: String): File {
        val isVideo = format.equals(Config.FORMAT_MP4, ignoreCase = true) || format.equals(Config.FORMAT_WEBM, ignoreCase = true)
        val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || format.equals(Config.FORMAT_PNG, ignoreCase = true) || format.equals(Config.FORMAT_WEBP, ignoreCase = true) || format.equals("JPEG", ignoreCase = true)
        val subfolderName = when {
            isVideo -> "video"
            isImage -> "image"
            else -> "audio"
        }
        val relativeSubfolder = "${Config.PATH_ROOT_FOLDER}/downloads/$subfolderName"
        
        val locationSetting = com.fabian.downloader.ui.AppSettings.downloadLocation
        val cacheKey = "${locationSetting}_$relativeSubfolder"

        cachedFolders[cacheKey]?.let {
            if (it.exists()) return it
        }
 
        // 1. Intentar usar la ubicación configurada expresamente por el usuario (SAF Uri o ruta física)
        var configuredDir: File? = null

        if (locationSetting.startsWith("content://")) {
            configuredDir = resolvePhysicalPathFromUri(context, locationSetting)
        } else if (locationSetting.isNotEmpty() && locationSetting != Config.PATH_DOWNLOAD_LOCATION_DEFAULT) {
            configuredDir = if (locationSetting.startsWith("/")) {
                File(locationSetting)
            } else if (locationSetting.startsWith("Downloads/", ignoreCase = true) || locationSetting.startsWith("Download/", ignoreCase = true)) {
                val rel = locationSetting.substringAfter("/")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), rel)
            } else {
                File(Environment.getExternalStorageDirectory(), locationSetting)
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
                cachedFolders[cacheKey] = finalFolder
                return finalFolder
            } else {
                android.util.Log.e(Config.TAG_PATH_UTILS, "Configured folder ${finalFolder.absolutePath} is NOT writable")
            }
        }

        // 2. Ubicación principal estándar: /storage/emulated/0/Download/FabiDownloader/downloads/video o audio
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null) {
            val downloadFabiFolder = File(publicDownloads, relativeSubfolder)
            if (isWritableDir(downloadFabiFolder)) {
                android.util.Log.d(Config.TAG_PATH_UTILS, "Successfully verified public Download folder: ${downloadFabiFolder.absolutePath}")
                cachedFolders[cacheKey] = downloadFabiFolder
                return downloadFabiFolder
            }
        }

        // 3. Fallback: Raíz de almacenamiento si está disponible y escribible
        val storageRoot = Environment.getExternalStorageDirectory()
        if (storageRoot != null) {
            val fabiDownloadFolder = File(storageRoot, relativeSubfolder)
            if (isWritableDir(fabiDownloadFolder)) {
                android.util.Log.d(Config.TAG_PATH_UTILS, "Successfully verified FabiDownloader root folder: ${fabiDownloadFolder.absolutePath}")
                cachedFolders[cacheKey] = fabiDownloadFolder
                return fabiDownloadFolder
            }
        }

        // 4. Recursos de emergencia
        val mediaDirs = context.externalMediaDirs
        for (mediaDir in mediaDirs) {
            if (mediaDir == null) continue
            val targetFolder = File(mediaDir, relativeSubfolder)
            if (isWritableDir(targetFolder)) {
                android.util.Log.w(Config.TAG_PATH_UTILS, "FALLBACK to externalMediaDirs: ${targetFolder.absolutePath}")
                cachedFolders[cacheKey] = targetFolder
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
                    cachedFolders[cacheKey] = targetFolder
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
        cachedFolders[cacheKey] = fallbackFolder
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
        
        // 1. Try clean path without ID in the primary download folder
        var file = File(baseFolder, "$sanitizedTitle.${format.lowercase()}")
        if (file.exists()) return file

        // 2. Try legacy path with ID
        file = File(baseFolder, "${sanitizedTitle}_$id.${format.lowercase()}")
        if (file.exists()) return file
        
        // 3. Fallback search in all possible historical locations (including legacy names)
        val storageRoot = Environment.getExternalStorageDirectory()
        val isVideo = format.equals(Config.FORMAT_MP4, ignoreCase = true) || format.equals(Config.FORMAT_WEBM, ignoreCase = true)
        val isImage = format.equals(Config.FORMAT_JPG, ignoreCase = true) || format.equals(Config.FORMAT_PNG, ignoreCase = true) || format.equals(Config.FORMAT_WEBP, ignoreCase = true) || format.equals("JPEG", ignoreCase = true)
        
        val capitalizedAppName = Config.APP_NAME_LOWER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        val subFolders = when {
            isVideo -> listOf(
                "${Config.PATH_ROOT_FOLDER}/downloads/video",
                "${Config.PATH_ROOT_FOLDER}/video",
                "${Config.PATH_ROOT_FOLDER}/downloads",
                "${Config.APP_NAME}/video",
                "$capitalizedAppName/video"
            )
            isImage -> listOf(
                "${Config.PATH_ROOT_FOLDER}/downloads/image",
                "${Config.PATH_ROOT_FOLDER}/image",
                "${Config.PATH_ROOT_FOLDER}/downloads",
                "${Config.APP_NAME}/image",
                "$capitalizedAppName/image"
            )
            else -> listOf(
                "${Config.PATH_ROOT_FOLDER}/downloads/audio",
                "${Config.PATH_ROOT_FOLDER}/audio",
                "${Config.PATH_ROOT_FOLDER}/downloads",
                "${Config.APP_NAME}/audio",
                "$capitalizedAppName/audio"
            )
        }
        
        val folderList = mutableListOf<File>()
        
        subFolders.forEach { sub ->
            if (storageRoot != null) {
                folderList.add(File(storageRoot, sub))
                folderList.add(File(storageRoot, "Android/$sub"))
            }
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
            // App files directories
            folderList.add(File(context.getExternalFilesDir(null) ?: context.filesDir, sub))
        }
        
        val legacyFilesDir = context.getExternalFilesDir(null)
        if (legacyFilesDir != null) {
            folderList.add(legacyFilesDir)
        }
        folderList.add(context.filesDir)

        for (folder in folderList) {
            val fNoId = File(folder, "$sanitizedTitle.${format.lowercase()}")
            if (fNoId.exists()) return fNoId
            val fWithId = File(folder, "${sanitizedTitle}_$id.${format.lowercase()}")
            if (fWithId.exists()) return fWithId
        }
        
        // Default to the preferred folder without ID
        return File(baseFolder, "$sanitizedTitle.${format.lowercase()}")
    }
}

