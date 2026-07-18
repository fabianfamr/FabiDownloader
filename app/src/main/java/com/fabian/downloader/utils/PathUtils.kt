package com.fabian.downloader.utils

import android.content.Context
import android.os.Environment
import java.io.File

@Suppress("DEPRECATION")
object PathUtils {
    private val cachedFolders = mutableMapOf<String, File>()

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

    fun getDownloadFolder(context: Context, format: String): File {
        val relativeSubfolder = Config.APP_NAME
        
        cachedFolders[relativeSubfolder]?.let {
            if (it.exists()) return it
        }

        // 1. Try standard public Download/FabiDownloader/... (This is what the user expects!)
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null) {
            val downloadFabiFolder = File(publicDownloads, relativeSubfolder)
            try {
                if (!downloadFabiFolder.exists()) {
                    downloadFabiFolder.mkdirs()
                }
                
                // On Android 10+, even if mkdirs returns true, we might not have permission to write files
                val testFile = File(downloadFabiFolder, ".test_write_${System.currentTimeMillis()}")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    android.util.Log.d(Config.TAG_PATH_UTILS, "Successfully verified public Download folder: ${downloadFabiFolder.absolutePath}")
                    cachedFolders[relativeSubfolder] = downloadFabiFolder
                    return downloadFabiFolder
                }
            } catch (e: Exception) {
                android.util.Log.e(Config.TAG_PATH_UTILS, "Public Download folder is NOT writable: ${e.message}")
            }
        }

        // 2. Try Android/media/com.fabian.downloader/FabiDownloader/video or .../audio
        // This is much better than private storage because it's scanned by Media Store and visible in Gallery.
        val mediaDirs = context.externalMediaDirs
        for (mediaDir in mediaDirs) {
            if (mediaDir == null) continue
            val targetFolder = File(mediaDir, relativeSubfolder)
            try {
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }
                val testFile = File(targetFolder, ".test_write_${System.currentTimeMillis()}")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    android.util.Log.d(Config.TAG_PATH_UTILS, "Using externalMediaDirs folder: ${targetFolder.absolutePath}")
                    cachedFolders[relativeSubfolder] = targetFolder
                    return targetFolder
                }
            } catch (e: Exception) {
                // ignore and try next
            }
        }

        // 3. Try App's external Download directory (Android/data/com.fabian.downloader/files/Download/...)
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
                    android.util.Log.d(Config.TAG_PATH_UTILS, "Using appExternalDownloadDir folder: ${targetFolder.absolutePath}")
                    cachedFolders[relativeSubfolder] = targetFolder
                    return targetFolder
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        // 4. Ultimate fallback to app's private files dir (User doesn't want this, but we need something)
        val fallbackFolder = File(context.filesDir, relativeSubfolder)
        if (!fallbackFolder.exists()) {
            fallbackFolder.mkdirs()
        }
        android.util.Log.w(Config.TAG_PATH_UTILS, "FALLBACK to private storage! This is what the user wants to avoid: ${fallbackFolder.absolutePath}")
        cachedFolders[relativeSubfolder] = fallbackFolder
        return fallbackFolder
    }

    fun getDownloadFile(context: Context, title: String, id: Long, format: String): File {
        val baseFolder = getDownloadFolder(context, format)
        val sanitizedTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        
        // 1. Try new path with ID
        var file = File(baseFolder, "${sanitizedTitle}_$id.${format.lowercase()}")
        if (file.exists()) return file
        
        // 2. Try new path without ID
        file = File(baseFolder, "$sanitizedTitle.${format.lowercase()}")
        if (file.exists()) return file
        
        // 3. Fallback search in all possible historical locations (including legacy names)
        val storageRoot = Environment.getExternalStorageDirectory()
        val isVideo = format.equals(Config.FORMAT_MP4, ignoreCase = true) || format.equals(Config.FORMAT_WEBM, ignoreCase = true)
        
        // We support both FabiDownloader and Fabidownloader
        val subFolders = if (isVideo) {
            listOf("${Config.APP_NAME}/video", "${Config.APP_NAME_LOWER.capitalize()}/video")
        } else {
            listOf("${Config.APP_NAME}/audio", "${Config.APP_NAME_LOWER.capitalize()}/audio")
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
