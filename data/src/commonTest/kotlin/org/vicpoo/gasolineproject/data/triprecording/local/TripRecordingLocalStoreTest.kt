package org.vicpoo.gasolineproject.data.triprecording.local

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.data.local.LocalDocumentCache
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.startsWithMagic
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.testJson
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Puntos GPS en la tabla track_point, fotos cifradas y lo que dejó la versión anterior en archivos. */
class TripRecordingLocalStoreTest {

    private val storage = TestStorage()
    private val baseDir = storage.directories.filesDir.toPath() / "trip_recording"

    private fun newStore() = TripRecordingLocalStore(
        fileSystem = storage.fileSystem,
        directories = storage.directories,
        json = testJson,
        snapshotCache = LocalDocumentCache(
            storage.database, storage.fileSystem, storage.directories,
            "trip_recording.json", RecordingSnapshot.serializer(), testJson, RecordingSnapshot(),
        ),
        database = storage.database,
        fileCipher = storage.fileCipher,
    )

    private val store = newStore()

    private fun point(n: Int) = TrackPointEntity(lat = 16.0 + n, lng = -93.0, t = "2026-11-24T10:00:" + n.toString().padStart(2, '0'))

    @Test
    fun puntos_seLeenEnOrden_porLotes_ySeCuentan() = runTest {
        repeat(5) { store.appendPoint("viaje-1", point(it)) }
        store.appendPoint("viaje-2", point(9))

        assertEquals(5, store.countPoints("viaje-1"))
        assertEquals(listOf(point(2), point(3)), store.readPoints("viaje-1", from = 2, limit = 2))
        assertEquals(listOf(point(4)), store.readPoints("viaje-1", from = 4))
        assertEquals(listOf(point(9)), store.readPoints("viaje-2"))
    }

    @Test
    fun puntosDeLaVersionAnterior_sePasanUnaVez_yElArchivoSeBorra() = runTest {
        val legacy = baseDir / "points" / "viaje-1.jsonl"
        storage.fileSystem.createDirectories(legacy.parent!!)
        storage.fileSystem.write(legacy) {
            writeUtf8((0..2).joinToString("\n", postfix = "\n") { testJson.encodeToString(TrackPointEntity.serializer(), point(it)) })
        }

        // Llega un punto nuevo con la app ya actualizada: va después de los viejos
        store.appendPoint("viaje-1", point(3))

        assertEquals((0..3).map { point(it) }, store.readPoints("viaje-1"))
        assertFalse(storage.fileSystem.exists(legacy))
        assertEquals(4, newStore().countPoints("viaje-1"))
    }

    @Test
    fun importarTodosLosPuntosViejos_alAbrirLaApp() = runTest {
        val dir = baseDir / "points"
        storage.fileSystem.createDirectories(dir)
        storage.fileSystem.write(dir / "a.jsonl") { writeUtf8(testJson.encodeToString(TrackPointEntity.serializer(), point(1)) + "\n") }
        storage.fileSystem.write(dir / "b.jsonl") { writeUtf8(testJson.encodeToString(TrackPointEntity.serializer(), point(2)) + "\n") }

        store.importAllLegacyPoints()

        assertTrue(storage.fileSystem.list(dir).isEmpty())
        assertEquals(listOf(point(1)), store.readPoints("a"))
        assertEquals(listOf(point(2)), store.readPoints("b"))
    }

    @Test
    fun recibo_seGuardaCifrado_ySeLeeIgual() = runTest {
        val photo = byteArrayOf(1, 2, 3, 4, 5)

        val path = store.saveReceipt(photo, "jpg")

        val onDisk = storage.fileSystem.read(path.toPath()) { readByteArray() }
        assertTrue(onDisk.startsWithMagic(), "La foto debe quedar cifrada en el teléfono")
        assertFalse(onDisk.copyOfRange(4, onDisk.size).contentEquals(photo))
        assertContentEquals(photo, store.readReceipt(path))
    }

    @Test
    fun reciboViejoSinCifrar_seSigueLeyendo_yAlMigrarSeCifraConElMismoNombre() = runTest {
        val photo = byteArrayOf(9, 8, 7)
        val legacy = baseDir / "receipts" / "recibo-viejo.jpg"
        storage.fileSystem.createDirectories(legacy.parent!!)
        storage.fileSystem.write(legacy) { write(photo) }

        assertContentEquals(photo, store.readReceipt(legacy.toString()))

        store.encryptLegacyReceipts()
        store.encryptLegacyReceipts() // dos veces no lo cifra dos veces

        assertTrue(storage.fileSystem.read(legacy) { readByteArray() }.startsWithMagic())
        assertContentEquals(photo, store.readReceipt(legacy.toString()))
    }

    @Test
    fun siNoSePuedeDescifrar_laFotoCuentaComoFaltante() = runTest {
        val path = store.saveReceipt(byteArrayOf(1), "jpg")
        storage.fileCipher.failDecrypt = true

        assertNull(store.readReceipt(path))
    }

    @Test
    fun quitarViaje_borraSusPuntos_yCerrarSesionBorraTodo() = runTest {
        store.appendPoint("viaje-1", point(1))
        store.appendPoint("viaje-2", point(2))

        store.removeTrip("viaje-1")
        assertEquals(0, store.countPoints("viaje-1"))
        assertEquals(1, store.countPoints("viaje-2"))

        store.clearAll()
        assertEquals(0, store.countPoints("viaje-2"))
    }
}
