package com.fabian.downloader.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var downloadDao: DownloadDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        downloadDao = database.downloadDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetDownloadById() = runTest {
        val record = DownloadRecord(
            title = "Test Video",
            url = "https://example.com/video",
            isCompleted = false,
            progress = 0
        )
        val id = downloadDao.insertDownload(record)
        
        val loaded = downloadDao.getDownloadById(id)
        assertNotNull(loaded)
        assertEquals("Test Video", loaded?.title)
        assertEquals("https://example.com/video", loaded?.url)
        assertEquals(false, loaded?.isCompleted)
    }

    @Test
    fun updateDownloadProgress() = runTest {
        val record = DownloadRecord(
            title = "Test Progress",
            url = "https://example.com/progress",
            isCompleted = false,
            progress = 0
        )
        val id = downloadDao.insertDownload(record)
        
        downloadDao.updateDownloadProgress(id, 50)
        
        val loaded = downloadDao.getDownloadById(id)
        assertEquals(50, loaded?.progress)
    }

    @Test
    fun markAsCompleted() = runTest {
        val record = DownloadRecord(
            title = "Test Complete",
            url = "https://example.com/complete",
            isCompleted = false,
            progress = 90
        )
        val id = downloadDao.insertDownload(record)
        
        downloadDao.markAsCompleted(id)
        
        val loaded = downloadDao.getDownloadById(id)
        assertEquals(true, loaded?.isCompleted)
    }

    @Test
    fun deleteDownload() = runTest {
        val record = DownloadRecord(
            title = "Test Delete",
            url = "https://example.com/delete",
            isCompleted = false,
            progress = 0
        )
        val id = downloadDao.insertDownload(record)
        assertNotNull(downloadDao.getDownloadById(id))
        
        downloadDao.deleteDownload(id)
        
        val loaded = downloadDao.getDownloadById(id)
        assertTrue(loaded == null)
    }
}
