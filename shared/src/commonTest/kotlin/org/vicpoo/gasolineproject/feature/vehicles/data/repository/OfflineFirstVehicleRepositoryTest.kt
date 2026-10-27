package org.vicpoo.gasolineproject.feature.vehicles.data.repository

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.di.AppScope
import org.vicpoo.gasolineproject.feature.vehicles.data.local.JsonFileVehicleLocalDataSource
import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleEntity
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.KtorVehicleApi
import org.vicpoo.gasolineproject.feature.vehicles.data.sync.VehicleSyncer
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.SyncState
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleDraft
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.RecordingSyncScheduler
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.noConnection
import org.vicpoo.gasolineproject.testing.respondJson
import org.vicpoo.gasolineproject.testing.respondProblem
import org.vicpoo.gasolineproject.testing.testJson
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fija el comportamiento offline-first ACTUAL del repositorio de vehículos.
 *
 * La sincronización que el repositorio lanza en segundo plano (scheduleSync) usa un AppScope que
 * nunca se ejecuta en estas pruebas: aquí solo se prueba qué queda guardado en el teléfono.
 * El envío a la API se prueba en VehicleSyncerTest.
 */
class OfflineFirstVehicleRepositoryTest {

    private val api = FakeApiServer()
    private val storage = TestStorage()
    private val session = FakeSessionReader(testSession())
    private val local = JsonFileVehicleLocalDataSource(storage.fileSystem, storage.directories, testJson)
    private val syncer = VehicleSyncer(local, KtorVehicleApi(api.client), RecordingSyncScheduler())

    /** Scope que nunca avanza: lo que se lance en segundo plano no corre. */
    private val idleAppScope = AppScope(CoroutineScope(StandardTestDispatcher()))

    private val repository = OfflineFirstVehicleRepository(
        local = local,
        api = KtorVehicleApi(api.client),
        syncer = syncer,
        sessionReader = session,
        appScope = idleAppScope,
    )

    private val draft = VehicleDraft(
        brand = "Nissan",
        model = "Versa",
        plates = "ABC-123",
        year = 2022,
        color = "Blanco",
        mileageKm = 1000.0,
        tankCapacityLiters = 41.0,
        newPhoto = null,
        removePhoto = false,
    )

    private fun synced(localId: String, serverId: String) = VehicleEntity(
        localId = localId,
        serverId = serverId,
        companyId = "company-1",
        brand = "Toyota",
        model = "Yaris",
        plates = "XYZ-$serverId",
        syncState = SyncState.SYNCED,
    )

    @Test
    fun crear_seGuardaEnElTelefonoAlInstante_comoPendiente() = runTest {
        val result = repository.create(draft)

        val vehicle = assertIs<AppResult.Success<*>>(result)
        assertNotNull(vehicle.data)
        val list = repository.observeVehicles().first()
        assertEquals(1, list.size)
        assertEquals(SyncState.PENDING_CREATE, list.single().syncState)
        assertNull(list.single().serverId)
        assertEquals(1, repository.observePendingSyncCount().first())
        assertTrue(api.requests.isEmpty(), "Crear no debe esperar a la API")
    }

    @Test
    fun crear_sinEmpresa_fallaConNoCompany() = runTest {
        session.state.value = testSession().copy(companyId = null)

        val result = repository.create(draft)

        assertEquals(AppResult.Failure(AppError.NoCompany), result)
        assertTrue(repository.observeVehicles().first().isEmpty())
    }

    @Test
    fun editar_unoYaSincronizado_quedaPendienteDeEditar() = runTest {
        local.upsert(synced("srv-1", "1"))

        repository.update("srv-1", draft.copy(color = "Rojo"))

        val saved = assertNotNull(local.get("srv-1"))
        assertEquals(SyncState.PENDING_UPDATE, saved.syncState)
        assertEquals("Rojo", saved.color)
    }

    @Test
    fun borrar_sinRed_seOcultaYQuedaPendiente() = runTest {
        local.upsert(synced("srv-1", "1"))
        api.handler = { noConnection() }

        val result = repository.delete("srv-1")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(SyncState.PENDING_DELETE, assertNotNull(local.get("srv-1")).syncState)
        assertTrue(repository.observeVehicles().first().isEmpty(), "Un borrado pendiente no se muestra")
    }

    @Test
    fun borrar_rechazado409_regresaElError_yNoSeBorra() = runTest {
        local.upsert(synced("srv-1", "1"))
        api.handler = { respondProblem(HttpStatusCode.Conflict, "El carro tiene historial") }

        val result = repository.delete("srv-1")

        assertEquals(AppResult.Failure(AppError.Server(409, "El carro tiene historial")), result)
        assertEquals(SyncState.SYNCED, assertNotNull(local.get("srv-1")).syncState)
        assertEquals(1, repository.observeVehicles().first().size)
    }

    @Test
    fun borrar_unoQueNuncaLlegoALaApi_seBorraSinLlamarla() = runTest {
        repository.create(draft)
        val id = repository.observeVehicles().first().single().id

        val result = repository.delete(id)

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(repository.observeVehicles().first().isEmpty())
        assertTrue(api.requests.isEmpty())
    }

    @Test
    fun refrescar_reemplazaLosSincronizados_respetaPendientes_yOcultaDadosDeBaja() = runTest {
        // Pendiente con rechazo definitivo: el syncer no lo reenvía y el refresh no debe borrarlo
        local.upsert(
            VehicleEntity(
                localId = "loc-1",
                companyId = "company-1",
                brand = "Nissan",
                model = "Versa",
                plates = "ABC-123",
                syncState = SyncState.PENDING_CREATE,
                lastSyncErrorCode = 409,
                lastSyncErrorMessage = "Las placas ya existen",
            ),
        )
        local.upsert(synced("srv-old", "old")) // ya no viene de la API
        api.handler = {
            respondJson(
                """
                [
                  {"carroId":"2","empresaId":"company-1","marca":"Kia","modelo":"Rio","placas":"KIA-2"},
                  {"carroId":"3","empresaId":"company-1","marca":"Ford","modelo":"Fiesta","placas":"FOR-3","dadoDeBaja":true}
                ]
                """.trimIndent(),
            )
        }

        val result = repository.refresh()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("GET carros"), api.requests)
        val ids = repository.observeVehicles().first().map { it.id }.toSet()
        assertEquals(setOf("loc-1", "srv-2"), ids)
    }

    @Test
    fun refrescar_sinRed_regresaNoConnection_yConservaLoLocal() = runTest {
        local.upsert(synced("srv-1", "1"))
        api.handler = { noConnection() }

        val result = repository.refresh()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(1, repository.observeVehicles().first().size)
    }
}
