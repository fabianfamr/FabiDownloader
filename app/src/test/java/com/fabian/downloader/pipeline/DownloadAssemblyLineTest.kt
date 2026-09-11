package com.fabian.downloader.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class DownloadAssemblyLineTest {

    @Test
    fun `station1_cleanUrl removes tracking parameters`() {
        // Given
        val rawUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=share&fbclid=12345"

        // When
        val cleanUrl = DownloadAssemblyLine.station1_cleanUrl(rawUrl, keepPlaylistParams = false)

        // Then
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", cleanUrl)
    }

    @Test
    fun `station1_cleanUrl keeps essential youtube parameters when playlist disabled`() {
        // Given
        val rawUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL123456"
        
        // When
        val cleanUrl = DownloadAssemblyLine.station1_cleanUrl(rawUrl, keepPlaylistParams = false)

        // Then
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", cleanUrl)
    }
    
    @Test
    fun `station1_cleanUrl keeps playlist parameters when requested`() {
        // Given
        val rawUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL123456"
        
        // When
        val cleanUrl = DownloadAssemblyLine.station1_cleanUrl(rawUrl, keepPlaylistParams = true)

        // Then
        assertTrue(cleanUrl.contains("list=PL123456"))
    }

    @Test
    fun `station1_cleanUrl handles shorts urls`() {
        // Given
        val rawUrl = "https://youtube.com/shorts/abcdefgh?feature=share"
        
        // When
        val cleanUrl = DownloadAssemblyLine.station1_cleanUrl(rawUrl)

        // Then
        assertEquals("https://youtube.com/shorts/abcdefgh", cleanUrl)
    }
}
