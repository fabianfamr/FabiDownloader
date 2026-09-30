package com.fabian.downloader.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PathUtilsMigrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFlattenDeeplyNestedDownloadsFolders() {
        val baseDownloads = tempFolder.newFolder("downloads")

        // Crear una cadena profunda de carpetas anidadas: downloads/downloads/downloads/...
        var current = baseDownloads
        for (i in 1..20) {
            current = File(current, "downloads")
            current.mkdirs()
        }

        // Colocar un video y un audio en el nivel más profundo
        val videoFile = File(current, "test_video.mp4")
        videoFile.writeText("sample video content")

        val audioFile = File(current, "test_audio.mp3")
        audioFile.writeText("sample audio content")

        // Colocar otro video a mitad de camino (nivel 10)
        var midLevel = baseDownloads
        for (i in 1..10) {
            midLevel = File(midLevel, "downloads")
        }
        val midVideo = File(midLevel, "mid_video.webm")
        midVideo.writeText("mid video content")

        // Ejecutar el aplanado de carpetas anidadas
        PathUtils.flattenNestedDownloads(baseDownloads, baseDownloads)

        // Verificar que los archivos hayan sido rescatados y colocados en sus carpetas correctas
        val targetVideo = File(baseDownloads, "video/test_video.mp4")
        val targetMidVideo = File(baseDownloads, "video/mid_video.webm")
        val targetAudio = File(baseDownloads, "audio/test_audio.mp3")

        assertTrue("El video profundo debe existir en video/", targetVideo.exists())
        assertEquals("sample video content", targetVideo.readText())

        assertTrue("El video medio debe existir en video/", targetMidVideo.exists())
        assertEquals("mid video content", targetMidVideo.readText())

        assertTrue("El audio profundo debe existir en audio/", targetAudio.exists())
        assertEquals("sample audio content", targetAudio.readText())

        // Verificar que la subcarpeta 'downloads' anidada dentro de baseDownloads fue eliminada
        val nestedDownloadsFolder = File(baseDownloads, "downloads")
        assertFalse("La carpeta downloads anidada debe haber sido eliminada", nestedDownloadsFolder.exists())
    }

    @Test
    fun testDuplicateFileHandlingInNestedRescue() {
        val baseDownloads = tempFolder.newFolder("downloads_dup")
        val targetVideoDir = File(baseDownloads, "video")
        targetVideoDir.mkdirs()

        // Archivo ya existente en destino
        val existingVideo = File(targetVideoDir, "my_video.mp4")
        existingVideo.writeText("existing original content")

        // Carpeta anidada con un archivo de igual nombre pero diferente contenido
        val nested = File(baseDownloads, "downloads")
        nested.mkdirs()
        val nestedVideo = File(nested, "my_video.mp4")
        nestedVideo.writeText("different content from nested folder")

        PathUtils.flattenNestedDownloads(baseDownloads, baseDownloads)

        assertTrue(existingVideo.exists())
        assertEquals("existing original content", existingVideo.readText())

        val rescuedDuplicate = File(targetVideoDir, "my_video_1.mp4")
        assertTrue("El archivo con nombre duplicado debe guardarse con sufijo único", rescuedDuplicate.exists())
        assertEquals("different content from nested folder", rescuedDuplicate.readText())
        assertFalse(nested.exists())
    }

    @Test
    fun testDbFolderRemainsUntouchedAndPreserved() {
        val rootFabi = tempFolder.newFolder("FabiDownloader")
        val dbDir = File(rootFabi, "db")
        dbDir.mkdirs()

        val dbFile = File(dbDir, "downloader-database")
        dbFile.writeText("database SQLite header and data")

        val downloadsDir = File(rootFabi, "downloads")
        downloadsDir.mkdirs()

        // Crear anidamiento dentro de downloads
        val nestedDownloads = File(downloadsDir, "downloads")
        nestedDownloads.mkdirs()
        val nestedVideo = File(nestedDownloads, "video.mp4")
        nestedVideo.writeText("video content")

        // Aplanar descargas
        PathUtils.flattenNestedDownloads(downloadsDir, downloadsDir)

        // Verificar que la carpeta db y la base de datos están intactas
        assertTrue("La carpeta db debe permanecer existente", dbDir.exists())
        assertTrue("El archivo de base de datos debe permanecer intacto", dbFile.exists())
        assertEquals("database SQLite header and data", dbFile.readText())

        // Y que el video fue rescatado a downloads/video
        val rescuedVideo = File(downloadsDir, "video/video.mp4")
        assertTrue("El video anidado debe haber sido rescatado", rescuedVideo.exists())
    }
}
