package org.vicpoo.gasolineproject.platform.files

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.vicpoo.gasolineproject.domain.files.PickedFile
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Corre en un celular o emulador (necesita Bitmap y ExifInterface reales):
 *   ./gradlew :platform:connectedAndroidDeviceTest
 *
 * Revisa que la foto que se sube NUNCA lleve la ubicación GPS del EXIF (punto 4.6 del code review).
 */
@RunWith(AndroidJUnit4::class)
class AndroidImageCompressorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val compressor = AndroidImageCompressor()

    /** JPEG de color sólido con ubicación GPS y marca de cámara en el EXIF. */
    private fun jpegWithGps(width: Int, height: Int, quality: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 120, 40)) }
        val file = File.createTempFile("exif", ".jpg", context.cacheDir)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            ExifInterface(file.absolutePath).apply {
                setLatLong(16.7569, -93.1292)
                setAttribute(ExifInterface.TAG_MAKE, "CamaraDePrueba")
                saveAttributes()
            }
            return file.readBytes()
        } finally {
            file.delete()
        }
    }

    private fun ByteArray.exif() = ExifInterface(ByteArrayInputStream(this))

    @Test
    fun laFotoDePruebaSiTraeGps() {
        // Si esto falla, las demás pruebas no demostrarían nada
        assertNotNull(jpegWithGps(100, 100, 90).exif().latLong)
    }

    @Test
    fun fotoGrande_saleSinGpsNiMarcaDeCamara() = runTest {
        val original = PickedFile(name = "licencia.jpg", mimeType = "image/jpeg", bytes = jpegWithGps(2400, 1800, 95))

        val result = assertNotNull(compressor.compress(original))

        assertEquals("image/jpeg", result.mimeType)
        assertNull(result.bytes.exif().latLong)
        assertNull(result.bytes.exif().getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun fotoChicaQueYaPesaPoco_igualSeRecodificaYSaleSinGps() = runTest {
        // Antes: si la recodificada no pesaba menos, se regresaba la ORIGINAL (con su EXIF)
        val bytes = jpegWithGps(8, 8, 10)
        val original = PickedFile(name = "recibo.jpg", mimeType = "image/jpeg", bytes = bytes)

        val result = assertNotNull(compressor.compress(original))

        assertFalse(result.bytes.contentEquals(bytes), "Regresó la original")
        assertNull(result.bytes.exif().latLong)
    }

    @Test
    fun pngSeConvierteAJpeg() = runTest {
        val png = File.createTempFile("foto", ".png", context.cacheDir).let { file ->
            Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
                .let { bitmap -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            file.readBytes().also { file.delete() }
        }

        val result = assertNotNull(compressor.compress(PickedFile(name = "odometro.png", mimeType = "image/png", bytes = png)))

        assertEquals("image/jpeg", result.mimeType)
        assertEquals("odometro.jpg", result.name)
    }

    @Test
    fun imagenQueNoSePuedeLeer_regresaNullYNoLaOriginal() = runTest {
        val broken = PickedFile(name = "roto.jpg", mimeType = "image/jpeg", bytes = ByteArray(64) { it.toByte() })

        assertNull(compressor.compress(broken))
    }

    @Test
    fun pdf_seRegresaTalCual() = runTest {
        val pdf = PickedFile(name = "licencia.pdf", mimeType = "application/pdf", bytes = "%PDF-1.4".encodeToByteArray())

        val result = compressor.compress(pdf)

        assertSame(pdf, result)
        assertContentEquals(pdf.bytes, result?.bytes)
    }
}
