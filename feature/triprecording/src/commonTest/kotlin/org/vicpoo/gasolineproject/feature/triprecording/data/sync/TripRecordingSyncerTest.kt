package org.vicpoo.gasolineproject.feature.triprecording.data.sync

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.feature.triprecording.data.local.AssignedVehicleEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.RecordingSnapshot
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TrackPointEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.KtorTripRecordingApi
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
import kotlin.test.assertTrue

/**
 * Fija la ruta principal ACTUAL de TripRecordingSyncer antes del refactor
 * (y antes de dividirlo en piezas más chicas en la Fase 5).
 */
class TripRecordingSyncerTest {

    private val api = FakeApiServer()
    private val scheduler = RecordingSyncScheduler()
    private val storage = TestStorage()

    private val store = TripRecordingLocalStore(
        fileSystem = storage.fileSystem,
        directories = storage.directories,
        json = testJson,
        snapshotCache = JsonFileCache(
            storage.fileSystem,
            storage.directories,
            "trip_recording.json",
            RecordingSnapshot.serializer(),
            testJson,
            RecordingSnapshot(),
        ),
    )

    private val syncer = TripRecordingSyncer(
        store = store,
        api = KtorTripRecordingApi(api.client),
        appScope = AppScope(CoroutineScope(StandardTestDispatcher())), // requestSync() no corre aquí
        scheduler = scheduler,
    )

    private fun trip(running: Boolean) = TripEntity(
        localId = "trip-1",
        companyId = "company-1",
        employeeId = "emp-1",
        vehicle = AssignedVehicleEntity(
            reservationId = "res-1",
            vehicleId = "car-1",
            brand = "Nissan",
            model = "Versa",
            plates = "ABC-123",
        ),
        startedAt = "2026-10-20T08:00:00",
        startedAtMillis = 0,
        endedAt = if (running) null else "2026-10-20T09:00:00",
        endedAtMillis = if (running) null else 3_600_000,
        initialMileage = 1000.0,
        running = running,
        finalMileage = if (running) null else 1050.0,
    )

    private suspend fun seed(trip: TripEntity) {
        store.update { it.copy(trips = listOf(trip)) }
    }

    private suspend fun savedTrip(): TripEntity? = store.current().trips.firstOrNull { it.localId == "trip-1" }

    @Test
    fun viajeCompletoSinRed_quedaPendiente_yPideReintentarConRed() = runTest {
        seed(trip(running = false))
        api.handler = { noConnection() }

        val result = syncer.sync()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        val saved = assertNotNull(savedTrip())
        assertNull(saved.serverId)
        assertNull(saved.syncErrorCode)
        assertEquals(1, scheduler.whenOnlineCalls)
    }

    @Test
    fun viajeCompleto_conRed_seMandaDeUnaVez_ySeBorraDelTelefono() = runTest {
        seed(trip(running = false))
        api.handler = { respondJson("""{"viajeId":"v-1","estado":"finalizado"}""", HttpStatusCode.Created) }

        val result = syncer.sync()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("POST viajes/sincronizar"), api.requests)
        assertTrue(store.current().trips.isEmpty(), "Un viaje enviado completo ya no se guarda en el teléfono")
    }

    @Test
    fun viajeEnCurso_seCreaEnLaApi_ySubeSusPuntos() = runTest {
        seed(trip(running = true))
        store.appendPoint("trip-1", TrackPointEntity(lat = 16.75, lng = -93.11, t = "2026-10-20T08:01:00"))
        store.appendPoint("trip-1", TrackPointEntity(lat = 16.76, lng = -93.12, t = "2026-10-20T08:02:00"))
        api.handler = { request ->
            when (request.url.encodedPath.removePrefix("/api/")) {
                "viajes" -> respondJson("""{"viajeId":"v-1","estado":"enCurso"}""", HttpStatusCode.Created)
                "viajes/v-1/puntos-ruta" -> respondJson("""{"viajeId":"v-1","recibidos":2,"insertados":2}""")
                else -> error("Petición inesperada: ${request.url.encodedPath}")
            }
        }

        val result = syncer.sync()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("POST viajes", "POST viajes/v-1/puntos-ruta"), api.requests)
        val saved = assertNotNull(savedTrip(), "Un viaje en curso se queda en el teléfono")
        assertEquals("v-1", saved.serverId)
        assertEquals(2, saved.pointsSent)
        assertEquals(false, saved.closedOnServer)
    }

    @Test
    fun viajeRechazado400_guardaElError_yNoSeReenviaSolo() = runTest {
        seed(trip(running = false))
        api.handler = { respondProblem(HttpStatusCode.BadRequest, "El carro no tiene una reserva aprobada") }

        val first = syncer.sync()

        assertEquals(AppResult.Success(Unit), first)
        val saved = assertNotNull(savedTrip())
        assertEquals(400, saved.syncErrorCode)
        assertEquals("El carro no tiene una reserva aprobada", saved.syncErrorMessage)

        syncer.sync()
        assertEquals(1, api.requests.size, "Un rechazo 4xx espera a que el usuario lo corrija")
    }
}
