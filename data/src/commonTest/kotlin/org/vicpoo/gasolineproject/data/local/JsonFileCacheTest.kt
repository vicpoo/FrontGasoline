package org.vicpoo.gasolineproject.data.local

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.testJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Borrar el caché (cerrar sesión) no deja nada en el teléfono, ni el temporal de una escritura a medias. */
class JsonFileCacheTest {

    private val storage = TestStorage()
    private val cache = JsonFileCache(storage.fileSystem, storage.directories, "notas.json", String.serializer(), testJson, "")
    private val dir = storage.directories.filesDir.toPath() / "cache"

    @Test
    fun guardar_yLeer() = runTest {
        cache.save("hola")

        assertEquals("hola", cache.current())
    }

    @Test
    fun borrar_quitaElArchivoYElTemporal() = runTest {
        cache.save("datos de la cuenta")
        // La app se cerró a media escritura: quedó el temporal con datos
        storage.fileSystem.write(dir / "notas.json.tmp") { writeUtf8("\"datos a medias\"") }

        cache.clear()

        assertFalse(storage.fileSystem.exists(dir / "notas.json"))
        assertFalse(storage.fileSystem.exists(dir / "notas.json.tmp"))
        assertEquals("", cache.current())
    }
}
