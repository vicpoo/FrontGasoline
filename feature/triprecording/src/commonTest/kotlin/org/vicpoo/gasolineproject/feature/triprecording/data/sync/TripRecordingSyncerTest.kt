package org.vicpoo.gasolineproject.feature.triprecording.data.sync

import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.feature.triprecording.data.local.AssignedVehicleEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelSyncState
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

    private fun trip(running: Boolean, localId: String = "trip-1") = TripEntity(
        localId = localId,
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

    private suspend fun savedTrip(localId: String = "trip-1"): TripEntity? =
        store.current().trips.firstOrNull { it.localId == localId }

    /** 409 VIAJE_EN_CURSO tal como lo manda APIGasoline (ExceptionHandlingMiddleware). */
    private fun viajeEnCursoBody(operador: String, fechaInicio: String) =
        """{"status":409,"title":"Conflicto","detail":"El carro ABC-123 ya tiene otro viaje en curso.",""" +
            """"codigo":"VIAJE_EN_CURSO","viajeId":"v-9","empleadoOperadorId":"$operador","fechaInicio":"$fechaInicio"}"""

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

    // ---------------- Offline-first: el mismo UUID en el teléfono y en la API ----------------

    @Test
    fun viajeEnCurso_mandaElUuidDelTelefonoComoViajeId() = runTest {
        val uuid = "3f2b8c1e-9d4a-4e6b-8f1a-2c7d5e9b0a11"
        seed(trip(running = true, localId = uuid))
        var body = ""
        api.handler = { request ->
            body = request.body.toByteArray().decodeToString()
            respondJson("""{"viajeId":"$uuid","estado":"enCurso"}""", HttpStatusCode.Created)
        }

        syncer.sync()

        assertTrue(body.contains(""""viajeId":"$uuid""""), "El viaje se manda con el UUID del teléfono: $body")
        assertEquals(uuid, assertNotNull(savedTrip(uuid)).serverId)
    }

    @Test
    fun viajeConIdLocalQueNoEsUuid_noMandaViajeId() = runTest {
        seed(trip(running = true))
        var body = ""
        api.handler = { request ->
            body = request.body.toByteArray().decodeToString()
            respondJson("""{"viajeId":"v-1","estado":"enCurso"}""", HttpStatusCode.Created)
        }

        syncer.sync()

        assertTrue(!body.contains("viajeId"), "Un id que no es UUID no se manda (la API lo rechazaría): $body")
    }

    @Test
    fun reintentoDeViajeCompleto_laApiLoTieneEnCurso_mandaElFinalizar() = runTest {
        // Se perdió la respuesta de POST /viajes y el viaje se terminó sin red: /sincronizar regresa
        // el viaje que ya existía EN CURSO, así que todavía hay que finalizarlo.
        seed(trip(running = false))
        api.handler = { request ->
            when (request.url.encodedPath.removePrefix("/api/")) {
                "viajes/sincronizar" -> respondJson("""{"viajeId":"v-1","estado":"enCurso"}""")
                "viajes/v-1/finalizar" -> respondJson("""{"viajeId":"v-1","estado":"finalizado"}""")
                else -> error("Petición inesperada: ${request.url.encodedPath}")
            }
        }

        val result = syncer.sync()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("POST viajes/sincronizar", "POST viajes/v-1/finalizar"), api.requests)
        assertTrue(store.current().trips.isEmpty(), "Ya cerrado en la API, se borra del teléfono")
    }

    @Test
    fun viajeEnCurso409_esElMismoViaje_seLigaAEseIdYYaNoQuedaError() = runTest {
        // Viaje creado por una versión anterior de la app (sin UUID) cuya respuesta se perdió
        seed(trip(running = true))
        api.handler = {
            respondJson(viajeEnCursoBody(operador = "emp-1", fechaInicio = "2026-10-20T08:00:30"), HttpStatusCode.Conflict)
        }

        val result = syncer.sync()

        assertEquals(AppResult.Success(Unit), result)
        val saved = assertNotNull(savedTrip())
        assertEquals("v-9", saved.serverId)
        assertNull(saved.syncErrorMessage)
        assertNull(saved.syncErrorCode)
    }

    @Test
    fun recargaGuardadaSinRed_seMandaConSuUuid_yQuedaSincronizada() = runTest {
        val fuelId = "7c1d0e5a-2b3f-4a6c-9d8e-0f1a2b3c4d5e"
        seed(trip(running = true).copy(serverId = "v-1"))
        val receipt = store.saveReceipt(byteArrayOf(1, 2, 3), "jpg")
        store.update {
            it.copy(
                fuel = listOf(
                    FuelEntity(
                        localId = fuelId,
                        tripLocalId = "trip-1",
                        number = 1,
                        amount = 200.0,
                        liters = 10.0,
                        receiptPath = receipt,
                        receiptMimeType = "image/jpeg",
                        receiptChanged = true,
                        loadedAt = "2026-10-20T08:30:00-06:00",
                    ),
                ),
            )
        }
        var body = ""
        api.handler = { request ->
            when (request.url.encodedPath.removePrefix("/api/")) {
                "cargas-combustible" -> {
                    body = request.body.toByteArray().decodeToString()
                    respondJson("""{"cargaCombustibleId":"$fuelId","viajeId":"v-1","litros":10.0}""", HttpStatusCode.Created)
                }
                else -> error("Petición inesperada: ${request.url.encodedPath}")
            }
        }

        val result = syncer.sync()

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(body.contains("cargaCombustibleId") && body.contains(fuelId), "La recarga lleva el UUID del teléfono")
        val fuel = store.current().fuel.single()
        assertEquals(fuelId, fuel.serverId)
        assertEquals(FuelSyncState.SYNCED, fuel.state)
    }

    @Test
    fun viajeEnCurso409_deOtroOperador_noSeLiga_yQuedaElMensaje() = runTest {
        seed(trip(running = true))
        api.handler = {
            respondJson(viajeEnCursoBody(operador = "emp-2", fechaInicio = "2026-10-20T08:00:30"), HttpStatusCode.Conflict)
        }

        syncer.sync()

        val saved = assertNotNull(savedTrip())
        assertNull(saved.serverId)
        assertEquals(409, saved.syncErrorCode)
        assertEquals("El carro ABC-123 ya tiene otro viaje en curso.", saved.syncErrorMessage)
    }

    @Test
    fun viajeEnCurso409_mismoOperadorPeroOtraHora_noSeLiga() = runTest {
        seed(trip(running = true))
        api.handler = {
            respondJson(viajeEnCursoBody(operador = "emp-1", fechaInicio = "2026-10-19T15:00:00"), HttpStatusCode.Conflict)
        }

        syncer.sync()

        assertNull(assertNotNull(savedTrip()).serverId)
    }
}
