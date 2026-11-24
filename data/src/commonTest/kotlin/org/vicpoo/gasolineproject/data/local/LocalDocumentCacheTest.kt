package org.vicpoo.gasolineproject.data.local

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.testJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Caché guardado en la base local; trae una sola vez el archivo JSON de la versión anterior. */
class LocalDocumentCacheTest {

    private val storage = TestStorage()
    private val dir = storage.directories.filesDir.toPath() / "cache"

    private fun newCache() = LocalDocumentCache(
        storage.database, storage.fileSystem, storage.directories, "notas.json", String.serializer(), testJson, "",
    )

    private fun writeLegacy(content: String, name: String = "notas.json") {
        storage.fileSystem.createDirectories(dir)
        storage.fileSystem.write(dir / name) { writeUtf8(content) }
    }

    @Test
    fun guardar_yLeer_inclusoDesdeOtraInstancia() = runTest {
        newCache().save("hola")

        // Otra instancia = la app se cerró y se volvió a abrir: el dato sale de la base
        assertEquals("hola", newCache().current())
    }

    @Test
    fun borrar_dejaElValorVacio() = runTest {
        val cache = newCache()
        cache.save("datos de la cuenta")

        cache.clear()

        assertEquals("", cache.current())
        assertEquals("", newCache().current())
    }

    @Test
    fun archivoDeLaVersionAnterior_sePasaALaBase_yElArchivoSeBorra() = runTest {
        writeLegacy("\"viaje sin enviar\"")

        assertEquals("viaje sin enviar", newCache().current())

        assertFalse(storage.fileSystem.exists(dir / "notas.json"))
        assertEquals("viaje sin enviar", newCache().current())
    }

    @Test
    fun siLaBaseYaTieneElDato_elArchivoViejoNoLoPisa() = runTest {
        // La app se cerró después de pasar el archivo a la base y antes de borrarlo
        newCache().save("nuevo")
        writeLegacy("\"viejo\"")

        assertEquals("nuevo", newCache().current())
        assertFalse(storage.fileSystem.exists(dir / "notas.json"))
    }

    @Test
    fun archivoViejoDanado_seDescarta_yQuedaVacio() = runTest {
        writeLegacy("{esto no es json")
        writeLegacy("\"escritura a medias\"", name = "notas.json.tmp")

        assertEquals("", newCache().current())

        assertFalse(storage.fileSystem.exists(dir / "notas.json"))
        assertFalse(storage.fileSystem.exists(dir / "notas.json.tmp"))
    }

    @Test
    fun borrar_tambienQuitaArchivosViejos() = runTest {
        val cache = newCache()
        cache.current()
        writeLegacy("\"a medias\"", name = "notas.json.tmp")

        cache.clear()

        assertFalse(storage.fileSystem.exists(dir / "notas.json.tmp"))
    }
}
