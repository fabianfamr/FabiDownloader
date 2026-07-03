package com.fabian.downloader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [DownloadRecord::class, SearchHistoryRecord::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // To apply unique constraint and indices to search_history, 
                // we might need to handle duplicates if any exist.
                // For simplicity, we just create indices. If duplicates exist, it will fail, 
                // but usually there shouldn't be any if handled by the app.
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_timestamp` ON `search_history` (`timestamp`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_url` ON `download_records` (`url`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_isCompleted` ON `download_records` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_isPaused` ON `download_records` (`isPaused`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_timestamp` ON `download_records` (`timestamp`)")
            }
        }

        val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agregar tabla search_history si no existe
                db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                
                // Intentar agregar columnas nuevas a download_records.
                // Como no sabemos la versión exacta en la que se agregaron, capturamos la excepción si ya existen.
                val columns = listOf(
                    "quality" to "TEXT NOT NULL DEFAULT '720p'",
                    "format" to "TEXT NOT NULL DEFAULT 'MP4'",
                    "size" to "TEXT NOT NULL DEFAULT '0 MB'",
                    "timestamp" to "INTEGER NOT NULL DEFAULT 0",
                    "isPaused" to "INTEGER NOT NULL DEFAULT 0",
                    "thumbnailUrl" to "TEXT",
                    "speed" to "TEXT NOT NULL DEFAULT ''"
                )
                
                for ((colName, colDef) in columns) {
                    try {
                        db.execSQL("ALTER TABLE `download_records` ADD COLUMN `$colName` $colDef")
                    } catch (e: Exception) {
                        // La columna probablemente ya existe
                    }
                }
            }
        }
        
        val MIGRATION_1_7 = object : Migration(1, 7) { override fun migrate(db: SupportSQLiteDatabase) { MIGRATION_1_6.migrate(db); MIGRATION_6_7.migrate(db) } }
        val MIGRATION_2_7 = object : Migration(2, 7) { override fun migrate(db: SupportSQLiteDatabase) { MIGRATION_1_6.migrate(db); MIGRATION_6_7.migrate(db) } }
        val MIGRATION_3_7 = object : Migration(3, 7) { override fun migrate(db: SupportSQLiteDatabase) { MIGRATION_1_6.migrate(db); MIGRATION_6_7.migrate(db) } }
        val MIGRATION_4_7 = object : Migration(4, 7) { override fun migrate(db: SupportSQLiteDatabase) { MIGRATION_1_6.migrate(db); MIGRATION_6_7.migrate(db) } }
        val MIGRATION_5_7 = object : Migration(5, 7) { override fun migrate(db: SupportSQLiteDatabase) { MIGRATION_1_6.migrate(db); MIGRATION_6_7.migrate(db) } }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "downloader-database"
                )
                .addMigrations(MIGRATION_1_7, MIGRATION_2_7, MIGRATION_3_7, MIGRATION_4_7, MIGRATION_5_7, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
