package org.vicpoo.gasolineproject.data.triprecording.sync

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.data.local.LocalDocumentCache
import org.vicpoo.gasolineproject.data.triprecording.local.AssignedVehicleEntity
import org.vicpoo.gasolineproject.data.triprecording.local.RecordingSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.TrackPointEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.triprecording.remote.KtorTripRecordingApi
import org.vicpoo.gasolineproject.domain.common.httpCode
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.respondJson
import org.vicpoo.gasolineproject.testing.respondProblem
import org.vicpoo.gasolineproject.testing.testJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Subida de puntos GPS frente a los códigos de la API.
 * N001 (APIGasoline): si dos envíos del mismo lote chocan en SQL Server, la API reintenta y, si no puede,
 * responde 503 con Retry-After. Eso es temporal: el lote no se descarta y se vuelve a mandar.
 */
class RoutePointsUploaderTest {

    private val api = FakeApiServer()
    private val storage = TestStorage()

    private val store = TripRecordingLocalStore(
        fileSystem = storage.fileSystem,
        directories = storage.directories,
        json = testJson,
        database = storage.database,
        fileCipher = storage.fileCipher,
        snapshotCache = LocalDocumentCache(
            storage.database,
            storage.fileSystem,
            storage.directories,
            "trip_recording.json",
            RecordingSnapshot.serializer(),
            testJson,
            RecordingSnapshot(),
        ),
    )

    private val uploader = RoutePointsUploader(store, KtorTripRecordingApi(api.client))

    private val trip = TripEntity(
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
        startedAt = "2026-10-20T08:00:00-06:00",
        startedAtMillis = 0,
        initialMileage = 1000.0,
        running = true,
    )

    private suspend fun seedTwoPoints() {
        store.update { it.copy(trips = listOf(trip)) }
        store.appendPoint("trip-1", TrackPointEntity(lat = 16.75, lng = -93.11, t = "2026-10-20T08:01:00-06:00"))
        store.appendPoint("trip-1", TrackPointEntity(lat = 16.76, lng = -93.12, t = "2026-10-20T08:02:00-06:00"))
    }

    private suspend fun saved(): TripEntity = assertNotNull(store.current().trips.firstOrNull { it.localId == "trip-1" })

    @Test
    fun loteConInterbloqueo503_quedaPendiente_yNoSeDescarta() = runTest {
        seedTwoPoints()
        api.handler = { respondProblem(HttpStatusCode.ServiceUnavailable, "Inténtalo de nuevo en unos segundos.") }

        val step = uploader.send(saved(), serverId = "v-1")

        val deferred = assertIs<Step.Deferred>(step)
        assertEquals(503, deferred.error.httpCode())
        assertEquals(0, saved().pointsSent)
        assertEquals(0, saved().pointsDiscarded)
    }

    @Test
    fun loteConInterbloqueo503_seReenviaEnLaSiguienteVuelta() = runTest {
        seedTwoPoints()
        api.handler = { respondProblem(HttpStatusCode.ServiceUnavailable, "Inténtalo de nuevo en unos segundos.") }
        uploader.send(saved(), serverId = "v-1")

        api.handler = { respondJson("""{"viajeId":"v-1","recibidos":2,"insertados":2}""") }
        val step = uploader.send(saved(), serverId = "v-1")

        assertEquals(Step.Done, step)
        assertEquals(2, saved().pointsSent)
        assertEquals(0, saved().pointsDiscarded)
        assertEquals(listOf("POST viajes/v-1/puntos-ruta", "POST viajes/v-1/puntos-ruta"), api.requests)
    }

    @Test
    fun loteRechazado400_seDescartaYSeCuenta() = runTest {
        seedTwoPoints()
        api.handler = { respondProblem(HttpStatusCode.BadRequest, "Fuera del periodo del viaje.") }

        val step = uploader.send(saved(), serverId = "v-1")

        assertEquals(Step.Done, step)
        assertEquals(2, saved().pointsSent)
        assertEquals(2, saved().pointsDiscarded)
    }
}
