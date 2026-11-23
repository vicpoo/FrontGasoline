package org.vicpoo.gasolineproject.ui.triprecording.assigned

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.triprecording.ObserveAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveReleaseBlocksUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RefreshAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseBlock
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseVehicleUseCase
import org.vicpoo.gasolineproject.domain.triprecording.SyncPendingTripsUseCase
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.testing.FakeAssignedVehicleRepository
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.FakeCostCenterRepository
import org.vicpoo.gasolineproject.testing.FakeLastRefreshStore
import org.vicpoo.gasolineproject.testing.FakeTripRecordingSync
import org.vicpoo.gasolineproject.testing.FakeTripTracker
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testAssignedVehicle
import org.vicpoo.gasolineproject.testing.testRecordedTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pestaña "Registrar": carros asignados, modo sin conexión, viaje en curso y "Dejar carro". */
class AssignedVehiclesViewModelTest : MainDispatcherTest() {

    private val versa = testAssignedVehicle(reservationId = "res-1")
    private val aveo = testAssignedVehicle(reservationId = "res-2")
    private val vehicles = FakeAssignedVehicleRepository(listOf(versa, aveo))
    private val costCenters = FakeCostCenterRepository()
    private val tracker = FakeTripTracker()
    private val sync = FakeTripRecordingSync()
    private val network = FakeConnectivityMonitor(initial = true)
    private val lastRefresh = FakeLastRefreshStore(now = { 42L })

    private fun viewModel() = AssignedVehiclesViewModel(
        observeVehicles = ObserveAssignedVehiclesUseCase(vehicles),
        observeTracking = ObserveTrackingUseCase(tracker),
        observeReleaseBlocks = ObserveReleaseBlocksUseCase(vehicles),
        releaseVehicle = ReleaseVehicleUseCase(vehicles),
        refreshVehicles = RefreshAssignedVehiclesUseCase(vehicles),
        refreshCostCenters = RefreshCostCentersUseCase(costCenters),
        syncPendingTrips = SyncPendingTripsUseCase(sync),
        connectivity = network,
        lastRefresh = lastRefresh,
    )

    @Test
    fun alAbrir_refrescaCarrosYCentros_yMandaLoPendiente() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertEquals(listOf(versa, aveo), state.vehicles)
        assertEquals(1, vehicles.refreshCalls)
        assertEquals(1, costCenters.refreshCalls)
        assertEquals(1, sync.requestCalls)
        assertEquals(42L, state.lastUpdatedAtMillis)
        assertFalse(state.isOffline)
        assertFalse(state.isInitialLoading)
    }

    @Test
    fun sinRed_seMarcaSinConexion_sinError_yNoActualizaLaHora() = runTest {
        vehicles.refreshResult = AppResult.Failure(AppError.NoConnection)

        val vm = viewModel()

        assertTrue(vm.state.value.isOffline)
        assertNull(vm.state.value.error)
        assertNull(vm.state.value.lastUpdatedAtMillis)
    }

    @Test
    fun errorDelServidor_seMuestra() = runTest {
        vehicles.refreshResult = AppResult.Failure(AppError.Server(500, null))

        val vm = viewModel()

        assertEquals(AppError.Server(500, null), vm.state.value.error)
        assertFalse(vm.state.value.isOffline)
    }

    @Test
    fun alVolverLaRed_recargaSola() = runTest {
        network.online.value = false
        vehicles.refreshResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()
        assertEquals(1, vehicles.refreshCalls)

        vehicles.refreshResult = AppResult.Success(Unit)
        network.online.value = true

        assertEquals(2, vehicles.refreshCalls)
        assertFalse(vm.state.value.isOffline)
    }

    @Test
    fun deslizarParaRecargar_vuelveAPedirLaLista() = runTest {
        val vm = viewModel()

        vm.onEvent(AssignedVehiclesEvent.Refresh)

        assertEquals(2, vehicles.refreshCalls)
        assertFalse(vm.state.value.isRefreshing)
    }

    @Test
    fun conViajeEnCurso_seMuestraElAcceso() = runTest {
        val trip = testRecordedTrip(TripPhase.RUNNING, vehicle = versa)
        tracker.state.value = TrackingSession(trip = trip)

        val vm = viewModel()

        assertEquals(trip, vm.state.value.activeTrip)
    }

    @Test
    fun dejarCarro_conExito_loQuitaDeLaLista() = runTest {
        val vm = viewModel()

        vm.onEvent(AssignedVehiclesEvent.ReleaseClicked(versa))
        assertEquals(versa, vm.state.value.releaseTarget)
        vm.onEvent(AssignedVehiclesEvent.ReleaseNotesChanged("  Ya terminé  "))
        vm.onEvent(AssignedVehiclesEvent.ReleaseConfirmed)

        assertEquals(listOf("res-1" to "Ya terminé"), vehicles.released)
        assertEquals(versa, vm.state.value.released)
        assertNull(vm.state.value.releaseTarget)
        assertEquals(listOf(aveo), vm.state.value.vehicles)
    }

    @Test
    fun dejarCarroConViajeSinEnviar_noLlamaALaApi_yMuestraElError() = runTest {
        vehicles.releaseBlocks.value = mapOf("res-1" to ReleaseBlock.PENDING_SYNC)
        val vm = viewModel()
        assertEquals(ReleaseBlock.PENDING_SYNC, vm.state.value.releaseBlockOf(versa))
        assertNull(vm.state.value.releaseBlockOf(aveo))

        vm.onEvent(AssignedVehiclesEvent.ReleaseClicked(versa))
        vm.onEvent(AssignedVehiclesEvent.ReleaseConfirmed)

        assertTrue(vehicles.released.isEmpty())
        assertEquals(AppError.Server(409, null), vm.state.value.releaseError)
        assertNull(vm.state.value.released)
    }

    @Test
    fun observacionesMuyLargas_marcanElCampo_yElDialogoSigueAbierto() = runTest {
        val vm = viewModel()

        vm.onEvent(AssignedVehiclesEvent.ReleaseClicked(versa))
        vm.onEvent(AssignedVehiclesEvent.ReleaseNotesChanged("x".repeat(ReleaseVehicleUseCase.MAX_NOTES + 1)))
        vm.onEvent(AssignedVehiclesEvent.ReleaseConfirmed)

        assertEquals(ValidationReason.OUT_OF_RANGE, vm.state.value.releaseNotesError)
        assertEquals(versa, vm.state.value.releaseTarget)
        assertTrue(vehicles.released.isEmpty())

        vm.onEvent(AssignedVehiclesEvent.ReleaseNotesChanged("Listo"))
        assertNull(vm.state.value.releaseNotesError)
    }

    @Test
    fun cerrarElDialogo_cancelaDejarCarro() = runTest {
        val vm = viewModel()

        vm.onEvent(AssignedVehiclesEvent.ReleaseClicked(versa))
        vm.onEvent(AssignedVehiclesEvent.DialogDismissed)

        assertNull(vm.state.value.releaseTarget)
        assertTrue(vehicles.released.isEmpty())
    }
}
