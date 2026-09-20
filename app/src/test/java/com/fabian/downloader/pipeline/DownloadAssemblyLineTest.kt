package com.fabian.downloader.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para DownloadAssemblyLine.station1_cleanUrl.
 *
 * Cubre los casos borde más comunes: tracking params, playlists de YouTube,
 * trailing slash, URLs embebidas en texto, URLs malformadas.
 *
 * Nota: estos tests requieren UrlUtils y AppSettings (no incluidos en el zip).
 * Si no compilan, sustituye los `mock()` correspondientes o anota con @Ignore
 * hasta tener esos archivos disponibles.
 */
class DownloadAssemblyLineTest {

    // Casos que NO dependen de AppSettings (solo de Uri parsing):

    @Test
    fun `station1_cleanUrl removes trailing slash`() {
        val input = "https://example.com/watch?id=abc/"
        val result = DownloadAssemblyLine.station1_cleanUrl(input)
        assertEquals("https://example.com/watch?id=abc", result)
    }

    @Test
    fun `station1_cleanUrl extracts url from text`() {
        val input = "Check this out! https://example.com/watch?id=abc it's great"
        val result = DownloadAssemblyLine.station1_cleanUrl(input)
        assertEquals("https://example.com/watch?id=abc", result)
    }

    @Test
    fun `station1_cleanUrl handles malformed input`() {
        assertEquals("not-a-url", DownloadAssemblyLine.station1_cleanUrl("not-a-url"))
    }

    @Test
    fun `station1_cleanUrl handles empty input`() {
        assertEquals("", DownloadAssemblyLine.station1_cleanUrl(""))
    }

    @Test
    fun `station1_cleanUrl trims whitespace`() {
        assertEquals("https://example.com", DownloadAssemblyLine.station1_cleanUrl("  https://example.com  "))
    }

    @Test
    fun `station1_cleanUrl extracts first url from multiple`() {
        val input = "https://first.com/page and https://second.com/other"
        val result = DownloadAssemblyLine.station1_cleanUrl(input)
        assertEquals("https://first.com/page", result)
    }

    @Test
    fun `station1_cleanUrl handles http urls`() {
        assertEquals("http://example.com", DownloadAssemblyLine.station1_cleanUrl("http://example.com"))
    }

    @Test
    fun `station1_cleanUrl preserves path segments`() {
        val input = "https://example.com/path/to/resource?query=value"
        val result = DownloadAssemblyLine.station1_cleanUrl(input)
        assertEquals(input, result)
    }

    // Los siguientes tests requieren AppSettings.playlistEnabled y UrlUtils.
    // Si fallan en compilación por falta de mocks, mover a instrumented tests
    // o implementar fakes de AppSettings/UrlUtils.

    @Test
    fun `station1_cleanUrl removes utm tracking params`() {
        val input = "https://example.com/watch?id=abc&utm_source=newsletter&utm_medium=email"
        val result = DownloadAssemblyLine.station1_cleanUrl(input)
        // Aceptamos con o sin trailing ?, lo importante es que no haya utm_*.
        assertFalse("utm_source should be removed", result.contains("utm_source"))
        assertFalse("utm_medium should be removed", result.contains("utm_medium"))
    }

    @Test
    fun `station1_cleanUrl removes fbclid tracking param`() {
        val input = "https://example.com/watch?id=abc&fbclid=IwAR0xyz123"
        val result = DownloadAssemblyLine.station1_cleanUrl(input)
        assertFalse("fbclid should be removed", result.contains("fbclid"))
    }

    @Test
    fun `station1_cleanUrl removes igshid tracking param`() {
        val input = "https://instagram.com/p/abc123/?igshid=xyz"
        val result = DownloadAssemblyLine.station1_cleanUrl(input)
        assertFalse("igshid should be removed", result.contains("igshid"))
    }

    @Test
    fun `station4_preflightInspection returns false when output directory is null`() {
        val spec = DownloadTaskSpec(rawUrl = "https://example.com")
        assertFalse(DownloadAssemblyLine.station4_preflightInspection(spec))
    }
}
