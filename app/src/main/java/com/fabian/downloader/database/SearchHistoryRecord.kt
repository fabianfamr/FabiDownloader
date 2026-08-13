package com.fabian.downloader.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fabian.downloader.configs.Config

@Entity(
    tableName = Config.DB_TABLE_SEARCH_HISTORY,
    indices = [
        Index(value = ["query"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class SearchHistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
