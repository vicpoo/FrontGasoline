package org.vicpoo.gasolineproject.data.triprecording.sync

import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.data.triprecording.local.AssignedVehicleEntity
import org.vicpoo.gasolineproject.data.triprecording.local.FuelEntity
import org.vicpoo.gasolineproject.data.triprecording.local.FuelSyncState
import org.vicpoo.gasolineproject.data.triprecording.local.PauseEntity
import org.vicpoo.gasolineproject.data.triprecording.local.SyncLogSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.RecordingSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.TrackPointEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.triprecording.remote.KtorTripRecordingApi
import org.vicpoo.gasolineproject.domain.triprecording.SyncLogResult
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

    // ---------------- Etapa 1: recargas primero y envío con señal débil ----------------

    private suspend fun seedPendingFuel(fuelId: String = "7c1d0e5a-2b3f-4a6c-9d8e-0f1a2b3c4d5e") {
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
    }

    private suspend fun seedPoints(count: Int) {
        repeat(count) { i ->
            val minute = (i / 60).toString().padStart(2, '0')
            val second = (i % 60).toString().padStart(2, '0')
            store.appendPoint("trip-1", TrackPointEntity(lat = 16.75, lng = -93.11, t = "2026-10-20T08:$minute:$second"))
        }
    }

    private val fuelCreated = """{"cargaCombustibleId":"7c1d0e5a-2b3f-4a6c-9d8e-0f1a2b3c4d5e","viajeId":"v-1","litros":10.0}"""

    @Test
    fun recargasSeMandanAntesQueLosPuntos_aunqueLosPuntosFallen() = runTest {
        seed(trip(running = true).copy(serverId = "v-1"))
        seedPoints(250)
        seedPendingFuel()
        api.handler = { request ->
            when (request.url.encodedPath.removePrefix("/api/")) {
                "cargas-combustible" -> respondJson(fuelCreated, HttpStatusCode.Created)
                // Señal débil: los lotes de puntos no alcanzan a llegar
                else -> noConnection()
            }
        }

        val result = syncer.sync()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals("POST cargas-combustible", api.requests.first(), "La recarga va primero")
        assertEquals(FuelSyncState.SYNCED, store.current().fuel.single().state, "La recarga llegó aunque los puntos fallaran")
        assertEquals(1, scheduler.whenOnlineCalls)
    }

    @Test
    fun puntosEnLotesDe100() = runTest {
        seed(trip(running = true).copy(serverId = "v-1"))
        seedPoints(250)
        api.handler = { respondJson("""{"viajeId":"v-1","recibidos":100,"insertados":100}""") }

        val result = syncer.sync()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(List(3) { "POST viajes/v-1/puntos-ruta" }, api.requests)
        assertEquals(250, assertNotNull(savedTrip()).pointsSent)
    }

    @Test
    fun viajeTerminado_ordenRecargasPausasPuntosYCierre() = runTest {
        seed(
            trip(running = false).copy(
                serverId = "v-1",
                pauses = listOf(
                    PauseEntity(
                        localId = "p-1",
                        startedAt = "2026-10-20T08:10:00",
                        startedAtMillis = 600_000,
                        endedAt = "2026-10-20T08:15:00",
                        endedAtMillis = 900_000,
                    ),
                ),
            ),
        )
        seedPoints(2)
        seedPendingFuel()
        api.handler = { request ->
            when (request.url.encodedPath.removePrefix("/api/")) {
                "cargas-combustible" -> respondJson(fuelCreated, HttpStatusCode.Created)
                else -> respondJson("""{"viajeId":"v-1","estado":"enCurso","recibidos":2,"insertados":2}""")
            }
        }

        syncer.sync()

        assertEquals(
            listOf(
                "POST cargas-combustible",
                "POST viajes/v-1/pausar",
                "POST viajes/v-1/reanudar",
                "POST viajes/v-1/puntos-ruta",
                "POST viajes/v-1/finalizar",
            ),
            api.requests,
        )
    }

    private fun tripWithPause() = trip(running = false).copy(
        serverId = "v-1",
        pauses = listOf(
            PauseEntity(
                localId = "p-1",
                startedAt = "2026-10-20T08:10:00",
                startedAtMillis = 600_000,
                endedAt = "2026-10-20T08:15:00",
                endedAtMillis = 900_000,
            ),
        ),
    )

    @Test
    fun pausaConError500_noSePierde_yElViajeNoSeCierraHastaQueLlegue() = runTest {
        seed(tripWithPause())
        var serverDown = true
        api.handler = { request ->
            val path = request.url.encodedPath.removePrefix("/api/")
            if (serverDown && path == "viajes/v-1/pausar") {
                respondProblem(HttpStatusCode.InternalServerError, "Error interno")
            } else {
                respondJson("""{"viajeId":"v-1","estado":"enCurso"}""")
            }
        }

        syncer.sync()

        // Primer intento: la pausa no se marca como enviada y NO se finaliza (se perdería)
        assertEquals(listOf("POST viajes/v-1/pausar"), api.requests)
        val pending = assertNotNull(savedTrip()).pauses.single()
        assertEquals(false, pending.startSent)
        assertEquals(false, assertNotNull(savedTrip()).closedOnServer)

        // Siguiente sincronización con el servidor bien: pausa, reanudar y cierre en orden
        serverDown = false
        api.requests.clear()
        syncer.sync()

        assertEquals(
            listOf("POST viajes/v-1/pausar", "POST viajes/v-1/reanudar", "POST viajes/v-1/finalizar"),
            api.requests,
        )
    }

    @Test
    fun pausaRechazada400_seDaPorEnviada_yElViajeSigue() = runTest {
        // 400 "ya está en pausa": la llamada anterior sí llegó aunque se perdió la respuesta
        seed(tripWithPause())
        api.handler = { request ->
            when (request.url.encodedPath.removePrefix("/api/")) {
                "viajes/v-1/pausar" -> respondProblem(HttpStatusCode.BadRequest, "El viaje ya está en pausa.")
                else -> respondJson("""{"viajeId":"v-1","estado":"finalizado"}""")
            }
        }

        syncer.sync()

        assertEquals(
            listOf("POST viajes/v-1/pausar", "POST viajes/v-1/reanudar", "POST viajes/v-1/finalizar"),
            api.requests,
        )
    }

    @Test
    fun bitacora_registraCadaPasoConSuResultado() = runTest {
        val logCache = JsonFileCache(
            storage.fileSystem, storage.directories, "trip_sync_log.json", SyncLogSnapshot.serializer(), testJson, SyncLogSnapshot(),
        )
        val log = TripSyncLog(logCache, FixedClock)
        val loggedSyncer = TripRecordingSyncer(
            store = store,
            api = KtorTripRecordingApi(api.client),
            appScope = AppScope(CoroutineScope(StandardTestDispatcher())),
            scheduler = scheduler,
            log = log,
        )
        seed(trip(running = true).copy(serverId = "v-1"))
        seedPoints(5)
        seedPendingFuel()
        api.handler = { request ->
            when (request.url.encodedPath.removePrefix("/api/")) {
                "cargas-combustible" -> respondProblem(HttpStatusCode.BadRequest, "Más litros que la capacidad del tanque")
                else -> noConnection()
            }
        }

        loggedSyncer.sync()

        val entries = log.entries.first()
        // El más reciente primero
        assertEquals(SyncLogResult.OFFLINE, entries[0].result)
        assertTrue(entries[0].step.startsWith("Ruta GPS"))
        assertEquals(SyncLogResult.REJECTED, entries[1].result)
        assertEquals("Recarga 1#: enviar", entries[1].step)
        assertEquals("Más litros que la capacidad del tanque", entries[1].message)
        assertEquals(400, entries[1].code)
    }

    @Test
    fun syncWithin_conSenalLenta_noDejaEsperando() = runTest {
        val gate = CompletableDeferred<Unit>()
        val slowSyncer = TripRecordingSyncer(
            store = store,
            api = KtorTripRecordingApi(api.client),
            appScope = AppScope(backgroundScope),
            scheduler = scheduler,
        )
        seed(trip(running = true).copy(serverId = "v-1"))
        seedPendingFuel()
        api.handler = {
            gate.await() // la subida no termina mientras dura la prueba
            respondJson("{}")
        }

        val result = slowSyncer.syncWithin(timeoutMillis = 8_000)

        assertNull(result, "Regresa sin esperar a que termine; el envío sigue en segundo plano")
        assertEquals(FuelSyncState.PENDING_CREATE, store.current().fuel.single().state, "La recarga sigue guardada")
        gate.complete(Unit)
    }

    private object FixedClock : AppClock {
        override fun nowMillis(): Long = 1_793_990_000_000
        override fun toLocalIso(epochMillis: Long): String = "2026-11-06T12:00:00-06:00"
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
