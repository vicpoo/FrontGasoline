package org.vicpoo.gasolineproject.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.data.triprecording.local.AssignedVehicleEntity
import org.vicpoo.gasolineproject.data.triprecording.local.FuelEntity
import org.vicpoo.gasolineproject.data.triprecording.local.RecordingSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.TrackPointEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.vehicles.local.SqlVehicleLocalDataSource
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.startsWithMagic
import org.vicpoo.gasolineproject.testing.FakeAppLogger
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.testJson
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lo más importante de la fase 6: un chofer actualiza la app con un viaje SIN ENVIAR (puntos GPS y
 * una recarga con foto del recibo). Después de actualizar, el viaje, sus puntos y la foto siguen ahí,
 * dentro de la base cifrada, y no queda nada en archivos sin cifrar.
 */
class LegacyDataMigrationTest {

    private val storage = TestStorage()
    private val files = storage.directories.filesDir.toPath()
    private val receiptPhoto = byteArrayOf(10, 20, 30, 40)
    private val receiptPath = files / "trip_recording" / "receipts" / "recibo-1.jpg"

    private val trip = TripEntity(
        localId = "viaje-1",
        companyId = "company-1",
        employeeId = "emp-1",
        vehicle = AssignedVehicleEntity(reservationId = "res-1", vehicleId = "car-1", brand = "Nissan", model = "Versa", plates = "ABC-123"),
        startedAt = "2026-11-23T08:00:00",
        startedAtMillis = 1_000L,
        initialMileage = 32_000.0,
        running = false,
        finalMileage = 32_050.0,
    )
    private val fuel = FuelEntity(
        localId = "carga-1",
        tripLocalId = "viaje-1",
        number = 1,
        liters = 30.0,
        loadedAt = "2026-11-23T09:00:00",
        receiptPath = receiptPath.toString(),
        receiptMimeType = "image/jpeg",
    )
    private val points = (0..4).map { TrackPointEntity(lat = 16.75 + it / 1000.0, lng = -93.11, t = "2026-11-23T08:0$it:00") }

    private fun recordingCache() = LocalDocumentCache(
        storage.database, storage.fileSystem, storage.directories,
        "trip_recording.json", RecordingSnapshot.serializer(), testJson, RecordingSnapshot(),
    )

    private fun newStore(cache: LocalDocumentCache<RecordingSnapshot> = recordingCache()) = TripRecordingLocalStore(
        fileSystem = storage.fileSystem,
        directories = storage.directories,
        json = testJson,
        snapshotCache = cache,
        database = storage.database,
        fileCipher = storage.fileCipher,
    )

    private fun migration(cache: LocalDocumentCache<RecordingSnapshot>, store: TripRecordingLocalStore, logger: FakeAppLogger = FakeAppLogger()) =
        LegacyDataMigration(
            database = storage.database,
            caches = listOf(cache),
            tripStore = store,
            vehicles = SqlVehicleLocalDataSource(storage.database, storage.fileSystem, storage.directories, testJson, storage.fileCipher),
            appScope = AppScope(CoroutineScope(StandardTestDispatcher())),
            logger = logger,
            fileSystem = storage.fileSystem,
            directories = storage.directories,
        )

    /** Lo que la versión anterior dejó en el teléfono. */
    private fun writeLegacyFiles() {
        val fs = storage.fileSystem
        fs.createDirectories(files / "cache")
        fs.write(files / "cache" / "trip_recording.json") {
            writeUtf8(testJson.encodeToString(RecordingSnapshot.serializer(), RecordingSnapshot(trips = listOf(trip), fuel = listOf(fuel))))
        }
        fs.createDirectories(files / "trip_recording" / "points")
        fs.write(files / "trip_recording" / "points" / "viaje-1.jsonl") {
            writeUtf8(points.joinToString("\n", postfix = "\n") { testJson.encodeToString(TrackPointEntity.serializer(), it) })
        }
        fs.createDirectories(receiptPath.parent!!)
        fs.write(receiptPath) { write(receiptPhoto) }
    }

    @Test
    fun viajeSinEnviar_sobreviveALaActualizacion_cifradoYSinArchivosEnClaro() = runTest {
        writeLegacyFiles()
        val cache = recordingCache()
        val store = newStore(cache)

        migration(cache, store).run()

        // Otra instancia = la app abierta otra vez: todo sale de la base
        val reopened = newStore()
        val snapshot = reopened.current()
        assertEquals(listOf(trip), snapshot.trips)
        assertEquals(listOf(fuel), snapshot.fuel)
        assertEquals(points, reopened.readPoints("viaje-1"))
        assertContentEquals(receiptPhoto, reopened.readReceipt(receiptPath.toString()))

        // En el teléfono ya no queda nada en claro
        assertFalse(storage.fileSystem.exists(files / "cache" / "trip_recording.json"))
        assertFalse(storage.fileSystem.exists(files / "trip_recording" / "points" / "viaje-1.jsonl"))
        assertTrue(storage.fileSystem.read(receiptPath) { readByteArray() }.startsWithMagic())
    }

    @Test
    fun correrDosVeces_noDuplicaPuntosNiRecargas() = runTest {
        writeLegacyFiles()
        val cache = recordingCache()
        val store = newStore(cache)

        migration(cache, store).run()
        migration(cache, store).run()

        assertEquals(points.size, store.countPoints("viaje-1"))
        assertEquals(1, store.current().fuel.size)
        assertEquals(
            LegacyDataMigration.DONE_VALUE,
            storage.database.use { appMetaQueries.valueOf(LegacyDataMigration.DONE_KEY).executeAsOneOrNull() },
        )
    }

    @Test
    fun sinArchivosViejos_soloSeMarcaComoHecha() = runTest {
        val cache = recordingCache()
        val logger = FakeAppLogger()

        migration(cache, newStore(cache), logger).run()

        assertEquals(RecordingSnapshot(), cache.current())
        assertTrue(logger.entries.none { it.level.name == "ERROR" })
    }
}
