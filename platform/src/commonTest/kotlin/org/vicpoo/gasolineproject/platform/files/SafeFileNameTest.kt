package org.vicpoo.gasolineproject.platform.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** El nombre que manda la API nunca puede sacar el archivo de su carpeta (path traversal). */
class SafeFileNameTest {

    @Test
    fun nombreNormal_quedaIgual() {
        assertEquals("Reporte flota noviembre.pdf", safeFileName("Reporte flota noviembre.pdf"))
        assertEquals("reporte_2026-11.xlsx", safeFileName("reporte_2026-11.xlsx"))
    }

    @Test
    fun rutaConPuntoPunto_seQuedaSoloElUltimoSegmento() {
        assertEquals("sesion.preferences_pb", safeFileName("../../files/datastore/sesion.preferences_pb"))
        assertEquals("x.pdf", safeFileName("/data/data/org.vicpoo.gasolineproject/files/x.pdf"))
    }

    @Test
    fun separadoresDeWindowsYMezclados_seQuitan() {
        assertEquals("c.pdf", safeFileName("a/b\\c.pdf"))
        assertEquals("evil.pdf", safeFileName("..\\..\\evil.pdf"))
    }

    @Test
    fun soloPuntosOVacio_usaElNombrePorDefecto() {
        assertEquals(DEFAULT_FILE_NAME, safeFileName(".."))
        assertEquals(DEFAULT_FILE_NAME, safeFileName("."))
        assertEquals(DEFAULT_FILE_NAME, safeFileName(""))
        assertEquals(DEFAULT_FILE_NAME, safeFileName("   "))
        assertEquals(DEFAULT_FILE_NAME, safeFileName("../"))
        assertEquals("reporte.pdf", safeFileName("", fallback = "reporte.pdf"))
    }

    @Test
    fun archivoOculto_pierdeElPuntoInicial() {
        assertEquals("bashrc", safeFileName(".bashrc"))
    }

    @Test
    fun caracteresDeControlYProhibidos_seQuitan() {
        assertEquals("reporte.pdf", safeFileName("repo\u0000rte\n.pdf"))
        assertEquals("reporte.pdf", safeFileName("re<po>rte:|?*\".pdf"))
        assertEquals("reporte.pdf", safeFileName("\treporte.pdf\r\n"))
    }

    @Test
    fun puntosOEspaciosAlFinal_seQuitan() {
        assertEquals("reporte.pdf", safeFileName("reporte.pdf. . "))
    }

    @Test
    fun nombreMuyLargo_seRecortaConservandoLaExtension() {
        val result = safeFileName("a".repeat(500) + ".pdf")
        assertEquals(MAX_FILE_NAME_LENGTH, result.length)
        assertTrue(result.endsWith(".pdf"))
    }

    @Test
    fun elResultadoNuncaTieneSeparadores() {
        listOf("../../x", "..\\..\\x", "a/../../b", "/", "\\", "....//....//x").forEach { raw ->
            val result = safeFileName(raw)
            assertFalse('/' in result, "\"$raw\" -> \"$result\"")
            assertFalse('\\' in result, "\"$raw\" -> \"$result\"")
            assertFalse(result.startsWith("."), "\"$raw\" -> \"$result\"")
        }
    }
}
