package org.vicpoo.gasolineproject.platform.files

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Fotos de cámara y reportes temporales: lo viejo se borra al abrir la app y todo al cerrar sesión. */
class AndroidTemporaryFilesTest {

    private val cacheDir: File = Files.createTempDirectory("cache").toFile()
    private val now = 10 * AndroidTemporaryFiles.MAX_AGE_MILLIS
    private val files = AndroidTemporaryFiles(cacheDir, now = { now })

    @AfterTest
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    private fun file(dir: File, name: String, ageMillis: Long): File =
        File(dir.apply { mkdirs() }, name).apply {
            writeText("x")
            setLastModified(now - ageMillis)
        }

    @Test
    fun alAbrirLaApp_soloSeBorraLoViejo() {
        val oldPhoto = file(files.cameraDir, "foto_vieja.jpg", ageMillis = 2 * AndroidTemporaryFiles.MAX_AGE_MILLIS)
        val newPhoto = file(files.cameraDir, "foto_recien_tomada.jpg", ageMillis = 60_000)
        val oldReport = file(files.reportsDir, "reporte.pdf", ageMillis = 3 * AndroidTemporaryFiles.MAX_AGE_MILLIS)
        val newReport = file(files.reportsDir, "reporte_nuevo.xlsx", ageMillis = 1_000)

        files.clearStale()

        assertFalse(oldPhoto.exists())
        assertTrue(newPhoto.exists(), "Si Android cerró la app mientras estaba en la cámara, la foto se lee al regresar")
        assertFalse(oldReport.exists())
        assertTrue(newReport.exists(), "La app con la que se compartió puede seguir leyéndolo")
    }

    @Test
    fun alCerrarSesion_seBorraTodo() {
        file(files.cameraDir, "foto.jpg", ageMillis = 0)
        file(files.reportsDir, "reporte.pdf", ageMillis = 0)

        files.clearAll()

        assertFalse(files.cameraDir.exists())
        assertFalse(files.reportsDir.exists())
    }

    @Test
    fun sinCarpetas_noFalla() {
        files.clearStale()
        files.clearAll()
    }

    @Test
    fun noTocaNadaFueraDeCameraYReports() {
        val other = File(cacheDir, "image_cache").apply { mkdirs() }
        val photo = File(other, "foto_de_coil").apply { writeText("x"); setLastModified(0) }

        files.clearStale()
        files.clearAll()

        assertTrue(photo.exists())
    }
}
