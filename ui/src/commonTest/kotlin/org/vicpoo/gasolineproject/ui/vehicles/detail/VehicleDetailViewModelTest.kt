package org.vicpoo.gasolineproject.ui.vehicles.detail

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.reservations.RequestOutcome
import org.vicpoo.gasolineproject.domain.reservations.RequestVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleStatus
import org.vicpoo.gasolineproject.domain.vehicles.usecase.DeleteVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.testing.FakePermissionChecker
import org.vicpoo.gasolineproject.testing.FakeReservationRepository
import org.vicpoo.gasolineproject.testing.FakeVehicleRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testVehicle
import org.vicpoo.gasolineproject.ui.vehicles.list.VehiclesMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Detalle de vehículo: RBAC por modo (flota / catálogo), eliminar y solicitar el carro. */
@OptIn(ExperimentalCoroutinesApi::class)
class VehicleDetailViewModelTest : MainDispatcherTest() {

    private val vehicles = FakeVehicleRepository(listOf(testVehicle(id = "veh-1", serverId = "car-1")))
    private val reservations = FakeReservationRepository()
    private val permissions = FakePermissionChecker(
        setOf(Privileges.CARRO_EDITAR, Privileges.CARRO_ELIMINAR, Privileges.SOLICITUD_CREAR),
    )

    private fun TestScope.viewModel(mode: VehiclesMode, id: String = "veh-1"): VehicleDetailViewModel {
        val vm = VehicleDetailViewModel(
            args = VehicleDetailArgs(id, mode),
            observeVehicle = ObserveVehicleUseCase(vehicles),
            deleteVehicle = DeleteVehicleUseCase(vehicles),
            requestVehicle = RequestVehicleUseCase(reservations),
            permissionChecker = permissions,
        )
        // El estado solo se calcula mientras alguien lo escucha (WhileSubscribed)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    private fun TestScope.effectsOf(vm: VehicleDetailViewModel): List<VehicleDetailEffect> {
        val effects = mutableListOf<VehicleDetailEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        return effects
    }

    private fun VehicleDetailViewModel.request(reason: String) {
        onEvent(VehicleDetailEvent.RequestClicked)
        onEvent(VehicleDetailEvent.RequestReasonChanged(reason))
        onEvent(VehicleDetailEvent.SendRequest)
    }

    @Test
    fun enFlota_puedeEditarYEliminar_peroNoSolicitar() = runTest {
        val vm = viewModel(VehiclesMode.FLEET)

        val state = vm.state.value
        assertEquals("veh-1", state.vehicle?.id)
        assertTrue(state.canEdit)
        assertTrue(state.canDelete)
        assertFalse(state.canRequest)
    }

    @Test
    fun enCatalogo_soloPuedeSolicitar_siEstaDisponible() = runTest {
        val vm = viewModel(VehiclesMode.CATALOG)
        assertTrue(vm.state.value.canRequest)
        assertFalse(vm.state.value.canEdit)

        vehicles.vehicles.value = listOf(testVehicle(id = "veh-1", status = VehicleStatus.IN_USE))

        assertFalse(vm.state.value.canRequest)
    }

    @Test
    fun sinPrivilegios_noPuedeNada() = runTest {
        permissions.privileges.value = emptySet()

        val vm = viewModel(VehiclesMode.FLEET)

        assertFalse(vm.state.value.canEdit)
        assertFalse(vm.state.value.canDelete)
    }

    @Test
    fun eliminar_pideConfirmacion_borra_yAvisa() = runTest {
        val vm = viewModel(VehiclesMode.FLEET)
        val effects = effectsOf(vm)

        vm.onEvent(VehicleDetailEvent.DeleteClicked)
        assertTrue(vm.state.value.showDeleteConfirm)
        vm.onEvent(VehicleDetailEvent.DeleteConfirmed)

        assertEquals(listOf("veh-1"), vehicles.deleted)
        assertEquals(listOf<VehicleDetailEffect>(VehicleDetailEffect.Deleted), effects)
        assertFalse(vm.state.value.showDeleteConfirm)
    }

    @Test
    fun siEliminarFalla_muestraElError() = runTest {
        vehicles.failure = AppError.Server(409, "Tiene un viaje en curso")
        val vm = viewModel(VehiclesMode.FLEET)
        val effects = effectsOf(vm)

        vm.onEvent(VehicleDetailEvent.DeleteClicked)
        vm.onEvent(VehicleDetailEvent.DeleteConfirmed)

        assertEquals(AppError.Server(409, "Tiene un viaje en curso"), vm.state.value.error)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun solicitarSinMotivo_marcaElCampo_yNoManda() = runTest {
        val vm = viewModel(VehiclesMode.CATALOG)

        vm.request("   ")

        assertEquals(ValidationReason.REQUIRED, vm.state.value.requestReasonError)
        assertTrue(vm.state.value.showRequestDialog)
        assertTrue(reservations.calls.isEmpty())
    }

    @Test
    fun solicitarConMotivo_laManda() = runTest {
        val vm = viewModel(VehiclesMode.CATALOG)

        vm.request("  Visita a cliente  ")

        assertEquals(listOf("requestVehicle:car-1:Visita a cliente"), reservations.calls)
        assertTrue(vm.state.value.requestSent)
        assertFalse(vm.state.value.showRequestDialog)

        vm.onEvent(VehicleDetailEvent.RequestResultDismissed)
        assertFalse(vm.state.value.requestSent)
    }

    @Test
    fun solicitarSinRed_quedaEnCola() = runTest {
        reservations.requestResult = AppResult.Success(RequestOutcome.QUEUED)
        val vm = viewModel(VehiclesMode.CATALOG)

        vm.request("Visita")

        assertTrue(vm.state.value.requestQueued)
        assertFalse(vm.state.value.requestSent)
    }

    @Test
    fun carroCreadoSinRed_todaviaNoSePuedeSolicitar() = runTest {
        vehicles.vehicles.value = listOf(testVehicle(id = "veh-1", serverId = null))
        val vm = viewModel(VehiclesMode.CATALOG)

        vm.request("Visita")

        assertEquals(AppError.NotFound, vm.state.value.requestError)
        assertTrue(reservations.calls.isEmpty())
    }

    @Test
    fun siLaApiRechazaLaSolicitud_muestraElError() = runTest {
        reservations.requestResult = AppResult.Failure(AppError.Server(409, "Ya tienes una solicitud pendiente"))
        val vm = viewModel(VehiclesMode.CATALOG)

        vm.request("Visita")

        assertEquals(AppError.Server(409, "Ya tienes una solicitud pendiente"), vm.state.value.requestError)
        assertFalse(vm.state.value.showRequestDialog)
        assertNull(vm.state.value.requestReasonError)
    }
}
