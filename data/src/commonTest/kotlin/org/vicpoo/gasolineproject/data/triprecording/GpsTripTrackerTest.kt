package org.vicpoo.gasolineproject.data.triprecording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.vicpoo.gasolineproject.data.local.LocalDocumentCache
import org.vicpoo.gasolineproject.data.triprecording.local.RecordingSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.location.LocationFix
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.testing.FakeAppClock
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.testAssignedVehicle
import org.vicpoo.gasolineproject.testing.testCostCenter
import org.vicpoo.gasolineproject.testing.testJson
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Registro GPS del viaje: iniciar, ruta y distancia, pausa / reanudar, detener, "Registrar Viaje"
 * (enviado, sin red, rechazado) y "Reintentar". (Cerrar sesión con viaje abierto: GpsTripTrackerSessionTest.)
 *
 * El tracker guarda en archivos con Dispatchers.Default, así que se espera con [eventually].
 */
class GpsTripTrackerTest {

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

    /** GPS falso: la prueba manda lecturas con [send]. */
    private class FixFeed : LocationProvider {
        val fixes = MutableSharedFlow<LocationFix>(extraBufferCapacity = 64)
        var lastKnownFix: LocationFix? = null
        override fun hasPermission() = true
        override fun isLocationEnabled() = true
        override fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<LocationFix> = fixes
        override suspend fun lastKnown(): LocationFix? = lastKnownFix
        fun send(fix: LocationFix) = check(fixes.tryEmit(fix))
    }

    private class FakeTrackingService : TrackingServiceController {
        var running = false
        override fun start() { running = true }
        override fun stop() { running = false }
    }

    /** Sincronización que "hace" lo que la prueba indique sobre el viaje guardado. */
    private class ScriptedSync : TripRecordingSync {
        var result: AppResult<Unit>? = AppResult.Success(Unit)
        var onSync: suspend () -> Unit = {}
        var requestCalls = 0
        override suspend fun sync(): AppResult<Unit> = syncWithin(0) ?: AppResult.Success(Unit)
        override fun requestSync() { requestCalls++ }
        override suspend fun syncWithin(timeoutMillis: Long): AppResult<Unit>? {
            onSync()
            return result
        }
    }

    private val location = FixFeed()
    private val service = FakeTrackingService()
    private val sync = ScriptedSync()
    private val clock = FakeAppClock(now = 1_000_000L)
    private val sessions = FakeSessionReader(testSession())

    // Scheduler propio que nunca avanza: el latido del GPS (delay en bucle) no corre en la prueba
    private val appScope = CoroutineScope(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = appScope.cancel()

    private fun tracker() = GpsTripTracker(
        store = store,
        locationProvider = location,
        trackingService = service,
        sync = sync,
        sessionReader = sessions,
        clock = clock,
        appScope = AppScope(appScope),
    )

    private val photo = PickedFile(name = "odometro.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })

    private suspend fun eventually(condition: suspend () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    private fun AppResult<RecordedTrip>.trip(): RecordedTrip = when (this) {
        is AppResult.Success -> data
        is AppResult.Failure -> error("No debía fallar: $error")
    }

    private suspend fun savedTrip(): TripEntity = store.current().trips.single()

    /** Lectura buena (±5 m) en [lat] grados; 0.0009° de latitud ≈ 100 m. */
    private fun fix(lat: Double, atMillis: Long, accuracy: Double = 5.0, speed: Double? = 10.0) =
        LocationFix(lat = lat, lng = -93.12, accuracyMeters = accuracy, speedMetersPerSecond = speed, timeMillis = atMillis)

    // ---------------- Iniciar ----------------

    @Test
    fun iniciar_guardaElViajeEnElTelefono_enciendeElGps_yManda() = runTest {
        val tracker = tracker()

        val trip = tracker.start(testAssignedVehicle(), testCostCenter("cc-1")).trip()

        assertEquals(TripPhase.RUNNING, trip.phase)
        assertEquals("cc-1", trip.costCenter?.id)
        assertEquals(1_000.0, trip.initialMileage)
        val saved = savedTrip()
        assertEquals("emp-1", saved.employeeId)
        assertEquals("company-1", saved.companyId)
        assertTrue(service.running)
        assertEquals(1, sync.requestCalls)
    }

    @Test
    fun iniciarDosVeces_regresaElMismoViaje() = runTest {
        val tracker = tracker()

        val first = tracker.start(testAssignedVehicle(), null).trip()
        val second = tracker.start(testAssignedVehicle(reservationId = "otro"), null).trip()

        assertEquals(first.localId, second.localId)
        assertEquals(1, store.current().trips.size)
    }

    @Test
    fun sinEmpleado_noSePuedeIniciar() = runTest {
        sessions.state.value = testSession(employeeId = null)
        val tracker = tracker()

        val result = tracker.start(testAssignedVehicle(), null)

        assertEquals(AppResult.Failure(AppError.NoCompany), result)
        assertTrue(store.current().trips.isEmpty())
        assertFalse(service.running)
    }

    @Test
    fun conUnaUbicacionRecienteYPrecisa_esElPuntoDePartida() = runTest {
        location.lastKnownFix = fix(16.75, atMillis = clock.now - 10_000, speed = null)
        val tracker = tracker()

        val trip = tracker.start(testAssignedVehicle(), null).trip()

        assertEquals(1, tracker.session.value.route.size)
        assertEquals(1, store.countPoints(trip.localId))
    }

    @Test
    fun conUnaUbicacionVieja_noSeUsaComoPartida() = runTest {
        location.lastKnownFix = fix(16.75, atMillis = clock.now - 10 * 60_000, speed = null)
        val tracker = tracker()

        val trip = tracker.start(testAssignedVehicle(), null).trip()

        assertTrue(tracker.session.value.route.isEmpty())
        assertEquals(0, store.countPoints(trip.localId))
    }

    // ---------------- Ruta y distancia ----------------

    @Test
    fun lecturasBuenas_sumanDistancia_yLasImprecisasNo() = runTest {
        val tracker = tracker()
        val trip = tracker.start(testAssignedVehicle(), null).trip()

        location.send(fix(16.7500, atMillis = 1_000))
        location.send(fix(16.7509, atMillis = 11_000)) // ~100 m en 10 s
        location.send(fix(16.7600, atMillis = 12_000, accuracy = 80.0)) // ±80 m: no cuenta

        eventually { store.countPoints(trip.localId) >= 2 && tracker.session.value.route.size >= 2 }
        withContext(Dispatchers.Default) { delay(100) } // da tiempo a la lectura imprecisa
        val distance = tracker.session.value.trip?.distanceMeters ?: 0.0
        assertTrue(distance in 90.0..110.0, "Distancia: $distance")
        assertEquals(2, store.countPoints(trip.localId))
        assertEquals(2, tracker.session.value.route.size)
        assertTrue(tracker.session.value.gpsActive)
    }

    // ---------------- Pausa / reanudar ----------------

    @Test
    fun pausar_apagaElGps_yGuardaLaPausa_yReanudarLaCierra() = runTest {
        val tracker = tracker()
        tracker.start(testAssignedVehicle(), null)

        val paused = tracker.pause().trip()
        assertEquals(TripPhase.PAUSED, paused.phase)
        assertFalse(service.running)
        assertTrue(savedTrip().pauses.single().isOpen)

        clock.now += 60_000
        val resumed = tracker.resume().trip()
        assertEquals(TripPhase.RUNNING, resumed.phase)
        assertTrue(service.running)
        assertFalse(savedTrip().pauses.single().isOpen)
    }

    @Test
    fun pausarSinViaje_noEncontrado() = runTest {
        val tracker = tracker()

        assertEquals(AppResult.Failure(AppError.NotFound), tracker.pause())
    }

    // ---------------- Detener / Registrar Viaje ----------------

    @Test
    fun detener_apagaElGps_yDeshacer_loVuelveAEncender() = runTest {
        val tracker = tracker()
        tracker.start(testAssignedVehicle(), null)

        val stopped = tracker.stop().trip()
        assertEquals(TripPhase.STOPPED, stopped.phase)
        assertFalse(service.running)
        assertNotNull(savedTrip().endedAt)

        val undone = tracker.undoStop().trip()
        assertEquals(TripPhase.RUNNING, undone.phase)
        assertTrue(service.running)
        assertNull(savedTrip().endedAt)
    }

    @Test
    fun registrarSinHaberDetenido_noSePuede() = runTest {
        val tracker = tracker()
        tracker.start(testAssignedVehicle(), null)

        assertEquals(AppResult.Failure(AppError.NotFound), tracker.finish(1_500.0, photo))
    }

    @Test
    fun registrar_enviado_terminaElViaje() = runTest {
        val tracker = tracker()
        val trip = tracker.start(testAssignedVehicle(), null).trip()
        tracker.stop()
        sync.onSync = { store.updateTrip(trip.localId) { it.copy(closedOnServer = true) } }

        val result = tracker.finish(1_500.0, photo)

        assertEquals(AppResult.Success(SendOutcome.Sent), result)
        assertNull(tracker.session.value.trip)
        val saved = savedTrip()
        assertEquals(1_500.0, saved.finalMileage)
        assertFalse(saved.running)
        assertNotNull(saved.odometerPhotoPath)
    }

    @Test
    fun registrarSinRed_quedaGuardado_yTerminaElViaje() = runTest {
        val tracker = tracker()
        tracker.start(testAssignedVehicle(), null)
        tracker.stop()
        sync.result = AppResult.Failure(AppError.NoConnection)

        val result = tracker.finish(1_500.0, photo)

        assertEquals(AppResult.Success(SendOutcome.SavedOffline), result)
        assertNull(tracker.session.value.trip)
        assertFalse(savedTrip().running)
    }

    @Test
    fun registrarRechazado_vuelveARegistrarViaje_yReintentarLoManda() = runTest {
        val tracker = tracker()
        val trip = tracker.start(testAssignedVehicle(), null).trip()
        tracker.stop()
        sync.onSync = {
            store.updateTrip(trip.localId) {
                it.copy(syncErrorMessage = "El kilometraje final es menor al inicial", syncErrorCode = 400)
            }
        }

        val result = tracker.finish(500.0, photo)

        assertEquals(AppResult.Success(SendOutcome.Rejected("El kilometraje final es menor al inicial")), result)
        assertEquals(TripPhase.STOPPED, tracker.session.value.trip?.phase, "Regresa a corregir el kilometraje")

        sync.onSync = { store.updateTrip(trip.localId) { it.copy(closedOnServer = true) } }
        val retried = tracker.retryFinishSend()

        assertEquals(SendOutcome.Sent, retried)
        assertNull(savedTrip().syncErrorMessage)
    }

    @Test
    fun registrarDosVeces_reemplazaLaFotoAnterior() = runTest {
        val tracker = tracker()
        val trip = tracker.start(testAssignedVehicle(), null).trip()
        tracker.stop()
        sync.onSync = {
            store.updateTrip(trip.localId) { it.copy(syncErrorMessage = "Rechazado", syncErrorCode = 400) }
        }
        tracker.finish(500.0, photo)
        val firstPhoto = savedTrip().odometerPhotoPath

        sync.onSync = { store.updateTrip(trip.localId) { it.copy(syncErrorMessage = null, syncErrorCode = null) } }
        tracker.finish(1_500.0, photo.copyWithBytes(ByteArray(20) { 3 }))

        val secondPhoto = savedTrip().odometerPhotoPath
        assertNotNull(firstPhoto)
        assertNotNull(secondPhoto)
        assertTrue(firstPhoto != secondPhoto)
        assertNull(store.readReceipt(firstPhoto), "La foto anterior se borró del teléfono")
    }

    private fun PickedFile.copyWithBytes(bytes: ByteArray) = PickedFile(name = name, mimeType = mimeType, bytes = bytes)
}
