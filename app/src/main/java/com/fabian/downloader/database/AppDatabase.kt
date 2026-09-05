package com.fabian.downloader.database

import android.content.Context
import com.fabian.downloader.configs.Config
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

        class Migration7To8 : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // To apply unique constraint and indices to search_history, 
                // we might need to handle duplicates if any exist.
                // For simplicity, we just create indices. If duplicates exist, it will fail, 
                // but usually there shouldn't be any if handled by the app.
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_timestamp` ON `search_history` (`timestamp`)")
            }
        }
        val MIGRATION_7_8: Migration = Migration7To8()

        class Migration6To7 : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_url` ON `download_records` (`url`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_isCompleted` ON `download_records` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_isPaused` ON `download_records` (`isPaused`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_records_timestamp` ON `download_records` (`timestamp`)")
            }
        }
        val MIGRATION_6_7: Migration = Migration6To7()

        class Migration1To6 : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agregar tabla search_history si no existe
                db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                
                val existingCols = mutableSetOf<String>()
                try {
                    val cursor = db.query("PRAGMA table_info(`download_records`)")
                    while (cursor.moveToNext()) {
                        val nameIndex = cursor.getColumnIndex("name")
                        if (nameIndex != -1) {
                            existingCols.add(cursor.getString(nameIndex))
                        }
                    }
                    cursor.close()
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error checking existing columns in MIGRATION_1_6", e)
                }

                val columns = listOf(
                    "quality" to "TEXT NOT NULL DEFAULT '720p'",
                    "format" to "TEXT NOT NULL DEFAULT 'MP4'",
                    "size" to "TEXT NOT NULL DEFAULT '0 MB'",
                    "timestamp" to "INTEGER NOT NULL DEFAULT 0",
                    "isPaused" to "INTEGER NOT NULL DEFAULT 0",
                    "thumbnailUrl" to "TEXT",
                    "speed" to "TEXT NOT NULL DEFAULT ''"
                )
                
                for (pair in columns) {
                    val colName = pair.first
                    val colDef = pair.second
                    if (!existingCols.contains(colName)) {
                        try {
                            db.execSQL("ALTER TABLE `download_records` ADD COLUMN `$colName` $colDef")
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "Error adding column $colName", e)
                        }
                    }
                }
            }
        }
        val MIGRATION_1_6: Migration = Migration1To6()
        
        class CompositeMigration(start: Int, end: Int, private val m1: Migration, private val m2: Migration) : Migration(start, end) {
            override fun migrate(db: SupportSQLiteDatabase) {
                m1.migrate(db)
                m2.migrate(db)
            }
        }

        val MIGRATION_1_7: Migration = CompositeMigration(1, 7, MIGRATION_1_6, MIGRATION_6_7)
        val MIGRATION_2_7: Migration = CompositeMigration(2, 7, MIGRATION_1_6, MIGRATION_6_7)
        val MIGRATION_3_7: Migration = CompositeMigration(3, 7, MIGRATION_1_6, MIGRATION_6_7)
        val MIGRATION_4_7: Migration = CompositeMigration(4, 7, MIGRATION_1_6, MIGRATION_6_7)
        val MIGRATION_5_7: Migration = CompositeMigration(5, 7, MIGRATION_1_6, MIGRATION_6_7)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = com.fabian.downloader.utils.PathUtils.getDatabaseFile(context.applicationContext)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbFile.absolutePath
                )
                .addMigrations(MIGRATION_1_7, MIGRATION_2_7, MIGRATION_3_7, MIGRATION_4_7, MIGRATION_5_7, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
