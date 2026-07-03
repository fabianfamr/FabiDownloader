package com.fabian.downloader.utils

object FileUtils {
    fun formatSize(sizeInBytes: Long): String {
        return "${sizeInBytes / 1024 / 1024} MB"
    }
}
