package org.vicpoo.gasolineproject.feature.vehicles.data.sync

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.feature.vehicles.data.local.JsonFileVehicleLocalDataSource
import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleEntity
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.KtorVehicleApi
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.SyncState
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.RecordingSyncScheduler
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.noConnection
import org.vicpoo.gasolineproject.testing.respondJson
import org.vicpoo.gasolineproject.testing.respondProblem
import org.vicpoo.gasolineproject.testing.testJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fija el comportamiento ACTUAL de VehicleSyncer antes del refactor multimódulo.
 * Si alguna falla después de mover código, se cambió el comportamiento sin querer.
 */
class VehicleSyncerTest {

    private val api = FakeApiServer()
    private val scheduler = RecordingSyncScheduler()
    private val storage = TestStorage()
    private val local = JsonFileVehicleLocalDataSource(storage.fileSystem, storage.directories, testJson)
    private val syncer = VehicleSyncer(local, KtorVehicleApi(api.client), scheduler)

    private fun pending(
        localId: String = "loc-1",
        serverId: String? = null,
        state: SyncState = SyncState.PENDING_CREATE,
    ) = VehicleEntity(
        localId = localId,
        serverId = serverId,
        companyId = "company-1",
        brand = "Nissan",
        model = "Versa",
        plates = "ABC-123",
        syncState = state,
    )

    private val carroCreado = """
        {"carroId":"srv-1","empresaId":"company-1","marca":"Nissan","modelo":"Versa",
         "placas":"ABC-123","kilometrajeActual":0.0,"estado":"disponible"}
    """.trimIndent()

    @Test
    fun sinRed_quedaPendiente_yPideReintentarConRed() = runTest {
        local.upsert(pending())
        api.handler = { noConnection() }

        val result = syncer.syncPending()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        val saved = assertNotNull(local.get("loc-1"))
        assertEquals(SyncState.PENDING_CREATE, saved.syncState)
        assertNull(saved.serverId)
        assertNull(saved.lastSyncErrorCode)
        assertEquals(1, scheduler.whenOnlineCalls)
    }

    @Test
    fun conRed_creaEnLaApi_yQuedaSincronizado() = runTest {
        local.upsert(pending())
        api.handler = { respondJson(carroCreado, HttpStatusCode.Created) }

        val result = syncer.syncPending()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("POST carros"), api.requests)
        val saved = assertNotNull(local.get("loc-1"))
        assertEquals(SyncState.SYNCED, saved.syncState)
        assertEquals("srv-1", saved.serverId)
        assertEquals(0, scheduler.whenOnlineCalls)
    }

    @Test
    fun rechazo409_guardaElError_yNoSeReenviaSolo() = runTest {
        local.upsert(pending())
        api.handler = { respondProblem(HttpStatusCode.Conflict, "Las placas ya existen") }

        val first = syncer.syncPending()

        // Un rechazo de la API no detiene la sincronización: se guarda en el vehículo
        assertEquals(AppResult.Success(Unit), first)
        val saved = assertNotNull(local.get("loc-1"))
        assertEquals(SyncState.PENDING_CREATE, saved.syncState)
        assertEquals(409, saved.lastSyncErrorCode)
        assertEquals("Las placas ya existen", saved.lastSyncErrorMessage)

        // 4xx es definitivo: la siguiente vuelta ya no lo manda
        syncer.syncPending()
        assertEquals(1, api.requests.size)
    }

    @Test
    fun error500_guardaElError_yNecesitaReintentarse() = runTest {
        local.upsert(pending())
        api.handler = { respondProblem(HttpStatusCode.InternalServerError, "Falla temporal") }

        syncer.syncPending()
        assertEquals(500, assertNotNull(local.get("loc-1")).lastSyncErrorCode)

        // 5xx es temporal: la siguiente vuelta lo vuelve a intentar
        api.handler = { respondJson(carroCreado, HttpStatusCode.Created) }
        syncer.syncPending()

        assertEquals(listOf("POST carros", "POST carros"), api.requests)
        val saved = assertNotNull(local.get("loc-1"))
        assertEquals(SyncState.SYNCED, saved.syncState)
        assertNull(saved.lastSyncErrorCode)
    }

    @Test
    fun borradoPendiente_404_seBorraLocalmente() = runTest {
        local.upsert(pending(serverId = "srv-1", state = SyncState.PENDING_DELETE))
        api.handler = { respondProblem(HttpStatusCode.NotFound, "No existe") }

        val result = syncer.syncPending()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("DELETE carros/srv-1"), api.requests)
        assertNull(local.get("loc-1"))
    }

    @Test
    fun borradoPendiente_409_seRestauraConElMensaje() = runTest {
        local.upsert(pending(serverId = "srv-1", state = SyncState.PENDING_DELETE))
        api.handler = { respondProblem(HttpStatusCode.Conflict, "El carro tiene historial") }

        syncer.syncPending()

        val saved = assertNotNull(local.get("loc-1"))
        assertEquals(SyncState.SYNCED, saved.syncState)
        assertEquals(409, saved.lastSyncErrorCode)
        assertEquals("El carro tiene historial", saved.lastSyncErrorMessage)
    }
}
