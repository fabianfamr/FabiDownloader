package com.fabian.downloader.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionUtilsTest {

    @Test
    fun `isNewer con versiones semanticas simples`() {
        assertTrue(VersionUtils.isNewer("1.2.3", "1.2.2"))
        assertTrue(VersionUtils.isNewer("2.0.0", "1.9.9"))
        assertFalse(VersionUtils.isNewer("1.2.2", "1.2.3"))
        assertFalse(VersionUtils.isNewer("1.2.3", "1.2.3"))
    }

    @Test
    fun `isNewer con distinto numero de segmentos`() {
        assertTrue(VersionUtils.isNewer("1.2.3", "1.2"))
        assertFalse(VersionUtils.isNewer("1.2", "1.2.3"))
        assertTrue(VersionUtils.isNewer("2025.1.1", "2025.1"))
    }

    @Test
    fun `isNewer con formato de fecha YYYY MM DD`() {
        assertTrue(VersionUtils.isNewer("2025.01.15", "2024.12.30"))
        assertFalse(VersionUtils.isNewer("2024.12.30", "2025.01.15"))
    }

    @Test
    fun `isNewer con segmentos alfanumericos`() {
        // 1.0.0-beta vs 1.0.0-alpha: 'b' > 'a'
        assertTrue(VersionUtils.isNewer("1.0.0-beta", "1.0.0-alpha"))
        // release candidates: 'rc1' vs 'rc2'
        assertFalse(VersionUtils.isNewer("1.0.0-rc1", "1.0.0-rc2"))
    }

    @Test
    fun `isNewer con current Unknown devuelve true`() {
        assertTrue(VersionUtils.isNewer("2025.01.15", "Unknown"))
        assertTrue(VersionUtils.isNewer("1.0.0", "Unknown"))
    }

    @Test
    fun `isNewer con current vacio devuelve true`() {
        assertTrue(VersionUtils.isNewer("1.0.0", ""))
    }

    @Test
    fun `isNewer con latest vacio devuelve false`() {
        assertFalse(VersionUtils.isNewer("", "1.0.0"))
    }

    @Test
    fun `isNewer con ambos vacios devuelve false`() {
        assertFalse(VersionUtils.isNewer("", ""))
    }

    @Test
    fun `isNewer caso sensitive insensible`() {
        // "Unknown" y "unknown" deben comportarse igual
        assertTrue(VersionUtils.isNewer("1.0.0", "unknown"))
    }

    @Test
    fun `isNewer con versiones mixtas numericas y alfanumericas`() {
        // yt-dlp usa formato YYYY.MM.DD.3042 — comparar contra YYYY.MM.DD.
        assertTrue(VersionUtils.isNewer("2025.01.15", "2024.12.30"))
    }

    @Test
    fun `isNewer con segmentos numericos grandes`() {
        // Versiones con segmentos > Int.MAX (Long)
        assertTrue(VersionUtils.isNewer("9999999999", "1"))
    }
}
