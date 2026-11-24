package org.vicpoo.gasolineproject.ui.reservations.assignmentdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.reservations.usecase.GetAssignmentsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveAssignmentDetailUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.UnlinkAssignmentUseCase
import org.vicpoo.gasolineproject.testing.FakeAssignmentsRepository
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.MainDispatcherTest

/** Detalle de Asignaciones: por carro o por empleado, y "Desvincular" con motivo obligatorio. */
@OptIn(ExperimentalCoroutinesApi::class)
class AssignmentDetailViewModelTest : MainDispatcherTest() {

    private val repository = FakeAssignmentsRepository()
    private val network = FakeConnectivityMonitor(initial = true)

    private val versaTarget = UnlinkTarget(
        reservationId = "res-1",
        vehicleTitle = "Nissan Versa 2022",
        plates = "ABC-123",
        employeeName = "Juan Pérez",
    )

    private fun TestScope.viewModel(key: String): AssignmentDetailViewModel {
        val vm = AssignmentDetailViewModel(
            keyValue = key,
            observeDetail = ObserveAssignmentDetailUseCase(repository),
            getAssignments = GetAssignmentsUseCase(repository),
            unlinkAssignment = UnlinkAssignmentUseCase(repository),
            connectivity = network,
        )
        // El estado solo se calcula mientras alguien lo escucha (WhileSubscribed)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    private fun AssignmentDetailViewModel.unlinkWith(target: UnlinkTarget, reason: String) {
        onEvent(AssignmentDetailEvent.UnlinkClicked(target))
        onEvent(AssignmentDetailEvent.UnlinkReasonChanged(reason))
        onEvent(AssignmentDetailEvent.UnlinkConfirmed)
    }

    @Test
    fun desdeCarros_muestraElCarroYQuienLoTiene() = runTest {
        val vm = viewModel("r:res-1")

        val detail = vm.state.value.detail
        assertEquals("Juan Pérez", detail?.employee?.name)
        assertEquals(listOf("ABC-123"), detail?.vehicles?.map { it.vehicle.plates })
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun desdeEmpleados_muestraTodosSusCarros() = runTest {
        val vm = viewModel("e:emp-1")

        assertEquals(2, vm.state.value.detail?.vehicles?.size)
    }

    @Test
    fun llaveInvalida_noMuestraNada() = runTest {
        val vm = viewModel("otra-cosa")

        assertNull(vm.state.value.detail)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun sinRed_noSePuedeDesvincular() = runTest {
        repository.loadFailure = AppError.NoConnection

        val vm = viewModel("r:res-1")

        assertTrue(vm.state.value.isOffline)
        assertFalse(vm.state.value.canUnlink)
        assertNull(vm.state.value.error)
    }

    @Test
    fun desvincularSinMotivo_marcaElCampo_yNoLlamaALaApi() = runTest {
        val vm = viewModel("r:res-1")

        vm.unlinkWith(versaTarget, "  ")

        assertEquals(ValidationReason.REQUIRED, vm.state.value.unlinkReasonError)
        assertEquals(versaTarget, vm.state.value.unlinkTarget, "El diálogo sigue abierto")
        assertTrue(repository.unlinked.isEmpty())
    }

    @Test
    fun desvincularElUnicoCarro_muestraElExito_yAlCerrarRegresaALaLista() = runTest {
        val vm = viewModel("r:res-1")

        vm.unlinkWith(versaTarget, "  Cambio de área  ")

        assertEquals(listOf("res-1" to "Cambio de área"), repository.unlinked)
        assertEquals(versaTarget, vm.state.value.unlinked)
        assertEquals("Juan Pérez", vm.state.value.detail?.employee?.name, "Se conserva lo que se veía mientras se muestra el éxito")

        vm.onEvent(AssignmentDetailEvent.DialogDismissed)

        assertTrue(vm.state.value.finished)
        assertNull(vm.state.value.detail)
    }

    @Test
    fun desvincularUnoDeVariosCarros_sigueEnElDetalleConLosDemas() = runTest {
        val vm = viewModel("e:emp-1")

        vm.unlinkWith(versaTarget, "Cambio de área")
        vm.onEvent(AssignmentDetailEvent.DialogDismissed)

        assertFalse(vm.state.value.finished)
        assertEquals(listOf("XYZ-987"), vm.state.value.detail?.vehicles?.map { it.vehicle.plates })
    }

    @Test
    fun siLaApiLoRechaza_muestraElError_yRecarga() = runTest {
        repository.unlinkResult = AppResult.Failure(AppError.Server(409, "Tiene un viaje en curso"))
        val vm = viewModel("r:res-1")

        vm.unlinkWith(versaTarget, "Cambio de área")

        assertEquals(AppError.Server(409, "Tiene un viaje en curso"), vm.state.value.unlinkError)
        assertNull(vm.state.value.unlinkTarget)
        assertNull(vm.state.value.unlinked)
        assertEquals(2, repository.loadCalls)

        vm.onEvent(AssignmentDetailEvent.DialogDismissed)
        assertNull(vm.state.value.unlinkError)
    }
}
