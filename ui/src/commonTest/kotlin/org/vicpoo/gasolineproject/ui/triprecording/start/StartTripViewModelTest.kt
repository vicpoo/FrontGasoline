package org.vicpoo.gasolineproject.ui.triprecording.start

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.triprecording.CheckLocationPermissionUseCase
import org.vicpoo.gasolineproject.domain.triprecording.GetAssignedVehicleUseCase
import org.vicpoo.gasolineproject.domain.triprecording.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.StartTripUseCase
import org.vicpoo.gasolineproject.testing.FakeAssignedVehicleRepository
import org.vicpoo.gasolineproject.testing.FakeCostCenterRepository
import org.vicpoo.gasolineproject.testing.FakeLocationProvider
import org.vicpoo.gasolineproject.testing.FakeTripTracker
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testAssignedVehicle
import org.vicpoo.gasolineproject.testing.testCostCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** "Iniciar Viaje": centro de costos obligatorio, permiso de ubicación y arranque del GPS. */
@OptIn(ExperimentalCoroutinesApi::class)
class StartTripViewModelTest : MainDispatcherTest() {

    private val vehicle = testAssignedVehicle(reservationId = "res-1")
    private val vehicles = FakeAssignedVehicleRepository(listOf(vehicle))
    private val costCenters = FakeCostCenterRepository()
    private val tracker = FakeTripTracker()
    private val location = FakeLocationProvider(permission = true)

    private fun viewModel(reservationId: String = "res-1") = StartTripViewModel(
        args = StartTripArgs(reservationId),
        getVehicle = GetAssignedVehicleUseCase(vehicles),
        observeCostCenters = ObserveCostCentersUseCase(costCenters),
        refreshCostCenters = RefreshCostCentersUseCase(costCenters),
        startTrip = StartTripUseCase(tracker),
        hasLocationPermission = CheckLocationPermissionUseCase(location),
        locationPermissionChanged = LocationPermissionChangedUseCase(tracker),
    )

    /** Junta los efectos que manda el ViewModel. */
    private fun TestScope.effectsOf(vm: StartTripViewModel): List<StartTripEffect> {
        val effects = mutableListOf<StartTripEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.toList(effects) }
        return effects
    }

    @Test
    fun alAbrir_cargaElCarro_yRefrescaLosCentrosDeCosto() = runTest {
        val vm = viewModel()

        assertFalse(vm.state.value.isLoading)
        assertEquals(vehicle, vm.state.value.vehicle)
        assertNull(vm.state.value.error)
        assertEquals(1, costCenters.refreshCalls)
    }

    @Test
    fun carroQueNoExiste_muestraNoEncontrado() = runTest {
        val vm = viewModel(reservationId = "otra")

        assertNull(vm.state.value.vehicle)
        assertEquals(AppError.NotFound, vm.state.value.error)
    }

    @Test
    fun conUnSoloCentroDeCosto_seEligeSolo() = runTest {
        costCenters.centers.value = listOf(testCostCenter("cc-1"))

        val vm = viewModel()

        assertEquals("cc-1", vm.state.value.selectedCostCenterId)
    }

    @Test
    fun conVariosCentrosYNingunoElegido_noArranca_yMarcaElError() = runTest {
        costCenters.centers.value = listOf(testCostCenter("cc-1"), testCostCenter("cc-2"))
        val vm = viewModel()

        vm.onEvent(StartTripEvent.StartClicked)

        assertTrue(vm.state.value.showCostCenterError)
        assertTrue(tracker.calls.isEmpty())
    }

    @Test
    fun sinPermisoDeUbicacion_loPide_yNoArranca() = runTest {
        location.permission = false
        val vm = viewModel()
        val effects = effectsOf(vm)

        vm.onEvent(StartTripEvent.StartClicked)

        assertEquals(listOf<StartTripEffect>(StartTripEffect.RequestLocationPermission), effects)
        assertTrue(tracker.calls.isEmpty())
    }

    @Test
    fun conPermiso_arrancaConElCentroElegido_yAvisa() = runTest {
        costCenters.centers.value = listOf(testCostCenter("cc-1"), testCostCenter("cc-2"))
        val vm = viewModel()
        val effects = effectsOf(vm)

        vm.onEvent(StartTripEvent.CostCenterSelected("cc-2"))
        vm.onEvent(StartTripEvent.StartClicked)

        assertEquals(listOf("start"), tracker.calls)
        assertEquals("cc-2", tracker.startedWith?.second?.id)
        assertEquals(listOf<StartTripEffect>(StartTripEffect.TripStarted), effects)
        assertFalse(vm.state.value.isStarting)
    }

    @Test
    fun permisoNegado_seMarca_yNoArranca() = runTest {
        val vm = viewModel()

        vm.onEvent(StartTripEvent.PermissionResult(granted = false))

        assertTrue(vm.state.value.permissionDenied)
        assertEquals(1, tracker.permissionChanges)
        assertTrue(tracker.calls.isEmpty())
    }

    @Test
    fun permisoConcedido_arranca() = runTest {
        val vm = viewModel()

        vm.onEvent(StartTripEvent.PermissionResult(granted = true))

        assertEquals(listOf("start"), tracker.calls)
        assertFalse(vm.state.value.permissionDenied)
    }

    @Test
    fun siElTrackerFalla_muestraElError_ySePuedeReintentar() = runTest {
        tracker.failure = AppError.Server(500, "Falla del servidor")
        val vm = viewModel()

        vm.onEvent(StartTripEvent.StartClicked)

        assertEquals(AppError.Server(500, "Falla del servidor"), vm.state.value.error)
        assertFalse(vm.state.value.isStarting)
    }
}
