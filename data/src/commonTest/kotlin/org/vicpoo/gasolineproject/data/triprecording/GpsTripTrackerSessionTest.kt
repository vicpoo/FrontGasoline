package org.vicpoo.gasolineproject.data.triprecording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.vicpoo.gasolineproject.data.local.LocalDocumentCache
import org.vicpoo.gasolineproject.data.triprecording.local.AssignedVehicleEntity
import org.vicpoo.gasolineproject.data.triprecording.local.RecordingSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.location.LocationFix
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.testJson
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cerrar sesión con un viaje abierto: el viaje NO se borra, queda en pausa y solo lo retoma su dueño.
 * (Antes se borraba del teléfono y en la API quedaba "en curso" para siempre.)
 */
class GpsTripTrackerSessionTest {

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

    private class FakeLocation : LocationProvider {
        override fun hasPermission() = true
        override fun isLocationEnabled() = true
        override fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<LocationFix> = emptyFlow()
        override suspend fun lastKnown(): LocationFix? = null
    }

    private class FakeTrackingService : TrackingServiceController {
        var running = false
        override fun start() { running = true }
        override fun stop() { running = false }
    }

    private object NoSync : TripRecordingSync {
        override suspend fun sync(): AppResult<Unit> = AppResult.Success(Unit)
        override fun requestSync() = Unit
    }

    private object FixedClock : AppClock {
        override fun nowMillis(): Long = 1_793_990_000_000
        override fun toLocalIso(epochMillis: Long): String = "2026-11-17T12:00:00-06:00"
    }

    // Scheduler propio que nunca avanza: el latido del GPS (delay en bucle) no corre en la prueba
    private val appScope = CoroutineScope(UnconfinedTestDispatcher())
    private val trackingService = FakeTrackingService()

    @AfterTest
    fun tearDown() = appScope.cancel()

    private fun tracker(sessions: FakeSessionReader) = GpsTripTracker(
        store = store,
        locationProvider = FakeLocation(),
        trackingService = trackingService,
        sync = NoSync,
        sessionReader = sessions,
        clock = FixedClock,
        appScope = AppScope(appScope),
    )

    private fun trip(employeeId: String, stopped: Boolean = false) = TripEntity(
        localId = "trip-$employeeId",
        companyId = "company-1",
        employeeId = employeeId,
        vehicle = AssignedVehicleEntity(reservationId = "res-1", vehicleId = "car-1", brand = "Nissan", model = "Versa", plates = "ABC-123"),
        startedAt = "2026-11-17T08:00:00-06:00",
        startedAtMillis = 0,
        endedAt = if (stopped) "2026-11-17T09:00:00-06:00" else null,
        endedAtMillis = if (stopped) 3_600_000 else null,
        initialMileage = 1000.0,
        running = true,
    )

    /** Espera (con tiempo real) a que se cumpla [condition]: el tracker trabaja en otro hilo. */
    private suspend fun eventually(condition: suspend () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    @Test
    fun alCerrarSesion_elViajeEnCursoQuedaEnPausa_yNoSeBorra() = runTest {
        store.update { it.copy(trips = listOf(trip("emp-1"))) }
        val sessions = FakeSessionReader(testSession(employeeId = "emp-1"))
        val tracker = tracker(sessions)
        eventually { tracker.session.value.trip?.phase == TripPhase.RUNNING }
        assertTrue(trackingService.running)

        sessions.state.value = null

        eventually { tracker.session.value.trip == null }
        val saved = assertNotNull(store.current().trips.singleOrNull(), "El viaje sigue guardado en el teléfono")
        val pause = saved.pauses.single()
        assertTrue(pause.isOpen, "Queda en pausa")
        assertEquals("Cierre de sesión en la app", pause.reason)
        assertTrue(saved.running)
        assertTrue(!trackingService.running, "Se apaga el GPS")
    }

    @Test
    fun alVolverAEntrar_laMismaCuenta_retomaElViajeEnPausa() = runTest {
        store.update { it.copy(trips = listOf(trip("emp-1"))) }
        val sessions = FakeSessionReader(testSession(employeeId = "emp-1"))
        val tracker = tracker(sessions)
        eventually { tracker.session.value.trip != null }

        sessions.state.value = null
        eventually { tracker.session.value.trip == null }
        sessions.state.value = testSession(employeeId = "emp-1")

        eventually { tracker.session.value.trip != null }
        assertEquals(TripPhase.PAUSED, tracker.session.value.trip?.phase)
        assertTrue(!trackingService.running, "En pausa el GPS sigue apagado hasta que toque Reanudar")
    }

    @Test
    fun otraCuenta_noVeElViaje_peroSeQuedaGuardadoParaSuDueno() = runTest {
        store.update { it.copy(trips = listOf(trip("emp-1"))) }
        val sessions = FakeSessionReader(testSession(employeeId = "emp-2"))
        val tracker = tracker(sessions)

        // Da tiempo a que intente restaurar
        withContext(Dispatchers.Default) { delay(200) }

        assertNull(tracker.session.value.trip)
        assertEquals("emp-1", store.current().trips.single().employeeId)

        sessions.state.value = testSession(employeeId = "emp-1")
        eventually { tracker.session.value.trip != null }
    }

    @Test
    fun viajeEsperandoKilometraje_alCerrarSesion_quedaIgual_sinPausa() = runTest {
        store.update { it.copy(trips = listOf(trip("emp-1", stopped = true))) }
        val sessions = FakeSessionReader(testSession(employeeId = "emp-1"))
        val tracker = tracker(sessions)
        eventually { tracker.session.value.trip?.phase == TripPhase.STOPPED }

        tracker.onSessionEnded()

        val saved = store.current().trips.single()
        assertTrue(saved.pauses.isEmpty())
        assertNotNull(saved.endedAt)
        assertNull(tracker.session.value.trip)
    }
}
