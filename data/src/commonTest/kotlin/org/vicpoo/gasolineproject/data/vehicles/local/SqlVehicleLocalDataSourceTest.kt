package org.vicpoo.gasolineproject.data.vehicles.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.startsWithMagic
import org.vicpoo.gasolineproject.domain.vehicles.model.SyncState
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.testJson
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Vehículos en la tabla vehicle: filtros en SQL, fotos cifradas y el archivo de la versión anterior. */
class SqlVehicleLocalDataSourceTest {

    private val storage = TestStorage()
    private val baseDir = storage.directories.filesDir.toPath() / "vehicles"

    private fun newLocal() = SqlVehicleLocalDataSource(
        storage.database, storage.fileSystem, storage.directories, testJson, storage.fileCipher,
    )

    private val local = newLocal()

    private fun vehicle(localId: String, serverId: String? = null, state: SyncState = SyncState.SYNCED, model: String = "Versa") =
        VehicleEntity(
            localId = localId,
            serverId = serverId,
            companyId = "company-1",
            brand = "Nissan",
            model = model,
            plates = "ABC-$localId",
            syncState = state,
        )

    @Test
    fun laUiNoVeLosBorradosPendientes_yElConteoDePendientesSaleDeSql() = runTest {
        local.upsert(vehicle("a"))
        local.upsert(vehicle("b", state = SyncState.PENDING_UPDATE))
        local.upsert(vehicle("c", state = SyncState.PENDING_DELETE))

        assertEquals(setOf("a", "b"), local.observeVisible().first().map { it.localId }.toSet())
        assertNull(local.observeVisible("c").first())
        assertEquals("b", local.observeVisible("b").first()?.localId)
        assertEquals(2, local.observePendingCount().first())
        assertEquals(3, local.getAll().size)
    }

    @Test
    fun replaceSynced_respetaLosPendientes_yConservaElIdLocal() = runTest {
        local.upsert(vehicle("loc-1", serverId = "srv-1"))
        local.upsert(vehicle("loc-2", serverId = "srv-2", state = SyncState.PENDING_UPDATE, model = "Cambio sin enviar"))

        local.replaceSynced(
            listOf(
                vehicle("srv-1", serverId = "srv-1", model = "Versa 2025"),
                vehicle("srv-2", serverId = "srv-2", model = "Lo de la API"),
                vehicle("srv-3", serverId = "srv-3", model = "Nuevo"),
            ),
        )

        val all = local.getAll().associateBy { it.serverId }
        assertEquals("loc-1", all.getValue("srv-1").localId)
        assertEquals("Versa 2025", all.getValue("srv-1").model)
        assertEquals("Cambio sin enviar", all.getValue("srv-2").model)
        assertEquals("Nuevo", all.getValue("srv-3").model)
    }

    @Test
    fun foto_seGuardaCifrada_ySeLeeIgual() = runTest {
        val photo = byteArrayOf(4, 5, 6)

        val path = local.savePhoto("a", photo, "jpg")

        assertTrue(storage.fileSystem.read(path.toPath()) { readByteArray() }.startsWithMagic())
        assertContentEquals(photo, local.readPhoto(path))
    }

    @Test
    fun archivoDeLaVersionAnterior_sePasaALaTabla_ySusFotosSeCifran() = runTest {
        val photo = byteArrayOf(1, 1, 2, 3)
        val photoPath = baseDir / "photos" / "loc-1-vieja.jpg"
        storage.fileSystem.createDirectories(photoPath.parent!!)
        storage.fileSystem.write(photoPath) { write(photo) }
        val legacy = listOf(
            vehicle("loc-1", state = SyncState.PENDING_CREATE).copy(localPhotoPath = photoPath.toString(), photoPendingUpload = true),
            vehicle("loc-2", serverId = "srv-2"),
        )
        storage.fileSystem.write(baseDir / "vehicles.json") {
            writeUtf8(testJson.encodeToString(ListSerializer(VehicleEntity.serializer()), legacy))
        }

        assertEquals(legacy.toSet(), local.getAll().toSet())
        assertFalse(storage.fileSystem.exists(baseDir / "vehicles.json"))

        local.encryptLegacyPhotos()
        assertTrue(storage.fileSystem.read(photoPath) { readByteArray() }.startsWithMagic())
        assertContentEquals(photo, newLocal().readPhoto(photoPath.toString()))
    }

    @Test
    fun siLaTablaYaTieneDatos_elArchivoViejoNoDuplicaNiPisa() = runTest {
        local.upsert(vehicle("loc-1", model = "Ya en la base"))
        storage.fileSystem.createDirectories(baseDir)
        storage.fileSystem.write(baseDir / "vehicles.json") {
            writeUtf8(testJson.encodeToString(ListSerializer(VehicleEntity.serializer()), listOf(vehicle("loc-1", model = "Viejo"))))
        }

        val other = newLocal()
        assertEquals(listOf("Ya en la base"), other.getAll().map { it.model })
        assertFalse(storage.fileSystem.exists(baseDir / "vehicles.json"))
    }

    @Test
    fun cerrarSesion_borraVehiculosYFotos() = runTest {
        local.upsert(vehicle("a"))
        val path = local.savePhoto("a", byteArrayOf(1), "jpg")

        local.clearAll()

        assertTrue(local.getAll().isEmpty())
        assertFalse(storage.fileSystem.exists(path.toPath()))
    }
}
