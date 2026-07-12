package com.fabian.downloader.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = com.fabian.downloader.utils.Config.DB_TABLE_DOWNLOADS,
    indices = [
        androidx.room.Index(value = ["url"]),
        androidx.room.Index(value = ["isCompleted"]),
        androidx.room.Index(value = ["isPaused"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
data class DownloadRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val isCompleted: Boolean,
    val progress: Int,
    val quality: String = "720p",
    val format: String = com.fabian.downloader.utils.Config.FORMAT_MP4,
    val size: String = com.fabian.downloader.utils.Config.STATUS_ZERO_MB,
    val timestamp: Long = System.currentTimeMillis(),
    val isPaused: Boolean = false,
    val thumbnailUrl: String? = null,
    val speed: String = ""
)
