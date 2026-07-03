package com.fabian.downloader.utils

import android.content.Context
import android.os.Environment
import java.io.File

object PathUtils {
    fun getDownloadFolder(context: Context, format: String): File {
        val isVideo = format.equals("MP4", ignoreCase = true) || format.equals("WEBM", ignoreCase = true)
        val relativeSubfolder = if (isVideo) "FabiDownloader/video" else "FabiDownloader/audio"
        
        // 1. Try Android/media/com.fabian.downloader/FabiDownloader/video or .../audio
        // This is writable without permissions in 10+ and scanned by some media scanners.
        val mediaDirs = context.externalMediaDirs
        val mediaDir = mediaDirs.firstOrNull()
        if (mediaDir != null) {
            val targetFolder = File(mediaDir, relativeSubfolder)
            try {
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }
                if (targetFolder.exists() && targetFolder.canWrite()) {
                    return targetFolder
                }
            } catch (e: Exception) {
                // ignore and try next
            }
        }

        // 2. Try App's external Download directory (Android/data/com.fabian.downloader/files/Download/...)
        val appExternalDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (appExternalDownloadDir != null) {
            val targetFolder = File(appExternalDownloadDir, relativeSubfolder)
            try {
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }
                if (targetFolder.exists() && targetFolder.canWrite()) {
                    return targetFolder
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        // 3. Fallback to standard Download/FabiDownloader/... (May fail in 10+ without legacy storage)
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null) {
            val downloadFabiFolder = File(publicDownloads, relativeSubfolder)
            try {
                if (!downloadFabiFolder.exists()) {
                    downloadFabiFolder.mkdirs()
                }
                if (downloadFabiFolder.exists() && downloadFabiFolder.canWrite()) {
                    return downloadFabiFolder
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        // 4. Ultimate fallback to app's private files dir
        val fallbackFolder = File(context.filesDir, relativeSubfolder)
        if (!fallbackFolder.exists()) {
            fallbackFolder.mkdirs()
        }
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
        val isVideo = format.equals("MP4", ignoreCase = true) || format.equals("WEBM", ignoreCase = true)
        
        // We support both FabiDownloader and Fabidownloader
        val subFolders = if (isVideo) {
            listOf("FabiDownloader/video", "Fabidownloader/video")
        } else {
            listOf("FabiDownloader/audio", "Fabidownloader/audio")
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
