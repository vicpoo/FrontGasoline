package org.vicpoo.gasolineproject.ui.triprecording.active

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.triprecording.DeleteFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveFuelRecordsUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.PauseTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ResumeTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.StopTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.testing.FakeAppClock
import org.vicpoo.gasolineproject.testing.FakeFuelRecordRepository
import org.vicpoo.gasolineproject.testing.FakePermissionChecker
import org.vicpoo.gasolineproject.testing.FakeTripRecordingSync
import org.vicpoo.gasolineproject.testing.FakeTripTracker
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testFuelRecord
import org.vicpoo.gasolineproject.testing.testRecordedTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Viaje en curso: pausa / reanudar, terminar, recargas del viaje y privilegios. */
class ActiveTripViewModelTest : MainDispatcherTest() {

    private val tracker = FakeTripTracker(TrackingSession(trip = testRecordedTrip(TripPhase.RUNNING)))
    private val fuel = FakeFuelRecordRepository(
        listOf(testFuelRecord(localId = "fuel-1", tripLocalId = "trip-1"), testFuelRecord(localId = "otra", tripLocalId = "otro-viaje")),
    )
    private val sync = FakeTripRecordingSync()
    private val permissions = FakePermissionChecker(setOf(Privileges.CARGA_REGISTRAR, Privileges.CARGA_ELIMINAR))

    private fun viewModel() = ActiveTripViewModel(
        observeTracking = ObserveTrackingUseCase(tracker),
        observeFuelRecords = ObserveFuelRecordsUseCase(fuel),
        permissionChecker = permissions,
        pauseTrip = PauseTripUseCase(tracker),
        resumeTrip = ResumeTripUseCase(tracker),
        stopTrip = StopTripUseCase(tracker),
        deleteFuelRecord = DeleteFuelRecordUseCase(fuel, sync),
        locationPermissionChanged = LocationPermissionChangedUseCase(tracker),
        clock = FakeAppClock(now = 60_000L),
    )

    @Test
    fun muestraElViaje_ySoloLasRecargasDeEseViaje() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertTrue(state.isReady)
        assertTrue(state.hasTrip)
        assertEquals(listOf("fuel-1"), state.fuelRecords.map { it.localId })
        assertEquals(60_000L, state.elapsedMillis)
    }

    @Test
    fun lasAccionesDeRecargasDependenDeLosPrivilegios() = runTest {
        val vm = viewModel()

        assertEquals(FuelPermissions(canAdd = true, canEdit = false, canDelete = true), vm.state.value.fuel)

        permissions.privileges.value = setOf(Privileges.CARGA_EDITAR)
        assertEquals(FuelPermissions(canAdd = false, canEdit = true, canDelete = false), vm.state.value.fuel)
    }

    @Test
    fun pausarYReanudar_cambianLaFaseDelViaje() = runTest {
        val vm = viewModel()

        vm.onEvent(ActiveTripEvent.PauseClicked)
        assertTrue(vm.state.value.isPaused)
        assertFalse(vm.state.value.isChangingPause)

        vm.onEvent(ActiveTripEvent.ResumeClicked)
        assertFalse(vm.state.value.isPaused)
        assertEquals(listOf("pause", "resume"), tracker.calls)
    }

    @Test
    fun siPausarFalla_muestraElError_ySePuedeCerrar() = runTest {
        tracker.failure = AppError.NotFound
        val vm = viewModel()

        vm.onEvent(ActiveTripEvent.PauseClicked)
        assertEquals(AppError.NotFound, vm.state.value.error)

        vm.onEvent(ActiveTripEvent.ErrorDismissed)
        assertNull(vm.state.value.error)
    }

    @Test
    fun terminar_pideConfirmacion_yAlConfirmarVaARegistrarViaje() = runTest {
        val vm = viewModel()

        vm.onEvent(ActiveTripEvent.FinishClicked)
        assertTrue(vm.state.value.showFinishConfirm)
        assertTrue(tracker.calls.isEmpty(), "Solo abre la confirmación")

        vm.onEvent(ActiveTripEvent.FinishConfirmed)
        assertEquals(listOf("stop"), tracker.calls)
        assertTrue(vm.state.value.navigateToFinish)
        assertFalse(vm.state.value.showFinishConfirm)

        vm.onEvent(ActiveTripEvent.NavigationHandled)
        assertFalse(vm.state.value.navigateToFinish)
    }

    @Test
    fun siDetenerFalla_noNavega_yMuestraElError() = runTest {
        val vm = viewModel()
        tracker.failure = AppError.NotFound

        vm.onEvent(ActiveTripEvent.FinishConfirmed)

        assertFalse(vm.state.value.navigateToFinish)
        assertEquals(AppError.NotFound, vm.state.value.error)
    }

    @Test
    fun siLaAppSeCerroEnRegistrarViaje_alAbrirRegresaAhi() = runTest {
        tracker.state.value = TrackingSession(trip = testRecordedTrip(TripPhase.STOPPED))

        val vm = viewModel()

        assertTrue(vm.state.value.navigateToFinish)
    }

    @Test
    fun borrarRecarga_pideConfirmacion_borra_yManda() = runTest {
        val vm = viewModel()
        val record = vm.state.value.fuelRecords.single()

        vm.onEvent(ActiveTripEvent.DeleteClicked(record))
        assertEquals(record, vm.state.value.deleteCandidate)

        vm.onEvent(ActiveTripEvent.DeleteConfirmed)
        assertEquals(listOf("fuel-1"), fuel.deleted)
        assertEquals(1, sync.requestCalls)
        assertNull(vm.state.value.deleteCandidate)
        assertTrue(vm.state.value.fuelRecords.isEmpty())
    }

    @Test
    fun cancelarElBorrado_noBorraNada() = runTest {
        val vm = viewModel()

        vm.onEvent(ActiveTripEvent.DeleteClicked(vm.state.value.fuelRecords.single()))
        vm.onEvent(ActiveTripEvent.DeleteDismissed)

        assertNull(vm.state.value.deleteCandidate)
        assertTrue(fuel.deleted.isEmpty())
    }

    @Test
    fun cambioDePermiso_avisaAlTracker() = runTest {
        val vm = viewModel()

        vm.onEvent(ActiveTripEvent.PermissionChanged)

        assertEquals(1, tracker.permissionChanges)
    }
}
