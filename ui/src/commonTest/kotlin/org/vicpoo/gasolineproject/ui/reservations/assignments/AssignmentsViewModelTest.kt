package org.vicpoo.gasolineproject.ui.reservations.assignments

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.reservations.usecase.GetAssignmentsUseCase
import org.vicpoo.gasolineproject.testing.FakeAssignmentsRepository
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.MainDispatcherTest

/** Asignaciones: carga, sin conexión, "Ver por" y búsqueda. */
class AssignmentsViewModelTest : MainDispatcherTest() {

    private val repository = FakeAssignmentsRepository()
    private val network = FakeConnectivityMonitor(initial = true)

    private fun viewModel() = AssignmentsViewModel(
        getAssignments = GetAssignmentsUseCase(repository),
        connectivity = network,
    )

    @Test
    fun alAbrir_cargaLasAsignaciones_porCarros() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertFalse(state.isInitialLoading)
        assertEquals(2, state.visible.vehicles.size)
        assertEquals(AssignmentsSegment.VEHICLES, state.viewBy)
        assertEquals(1, repository.loadCalls)
    }

    @Test
    fun sinRed_muestraElAviso_sinError() = runTest {
        repository.loadFailure = AppError.NoConnection

        val vm = viewModel()

        assertTrue(vm.state.value.isOffline)
        assertNull(vm.state.value.error)
    }

    @Test
    fun errorDelServidor_seMuestra() = runTest {
        repository.loadFailure = AppError.Server(500, null)

        val vm = viewModel()

        assertEquals(AppError.Server(500, null), vm.state.value.error)
        assertFalse(vm.state.value.isOffline)
    }

    @Test
    fun siSePierdeLaRed_seMarcaSinConexion_yAlVolverRecarga() = runTest {
        val vm = viewModel()

        network.online.value = false
        assertTrue(vm.state.value.isOffline)

        network.online.value = true
        assertFalse(vm.state.value.isOffline)
        assertEquals(2, repository.loadCalls)
    }

    @Test
    fun verPor_seEligeYSeQuitaConElMismoBoton() = runTest {
        val vm = viewModel()

        vm.onEvent(AssignmentsEvent.SegmentToggled(AssignmentsSegment.EMPLOYEES))
        assertEquals(AssignmentsSegment.EMPLOYEES, vm.state.value.viewBy)
        assertEquals(1, vm.state.value.activeFilters)

        vm.onEvent(AssignmentsEvent.SegmentToggled(AssignmentsSegment.EMPLOYEES))
        assertNull(vm.state.value.segment)
        assertEquals(AssignmentsSegment.VEHICLES, vm.state.value.viewBy)
    }

    @Test
    fun buscar_filtraPorPlacasOEmpleado_yBorrarTodoLimpia() = runTest {
        val vm = viewModel()

        vm.onEvent(AssignmentsEvent.SearchOpened)
        vm.onEvent(AssignmentsEvent.QueryChanged("xyz"))
        assertTrue(vm.state.value.isSearchOpen)
        assertEquals(listOf("XYZ-987"), vm.state.value.visible.vehicles.map { it.vehicle.plates })
        assertEquals(1, vm.state.value.visible.employees.size, "El empleado aparece por las placas de su carro")

        vm.onEvent(AssignmentsEvent.QueryChanged("juan"))
        assertEquals(2, vm.state.value.visible.vehicles.size, "Los carros aparecen por quien los tiene")

        vm.onEvent(AssignmentsEvent.SegmentToggled(AssignmentsSegment.EMPLOYEES))
        vm.onEvent(AssignmentsEvent.SearchCleared)
        assertEquals("", vm.state.value.query)
        assertNull(vm.state.value.segment)
        assertFalse(vm.state.value.hasSearchOrFilters)
    }

    @Test
    fun deslizarParaRecargar_vuelveAPedir() = runTest {
        val vm = viewModel()

        vm.onEvent(AssignmentsEvent.Refresh)

        assertEquals(2, repository.loadCalls)
        assertFalse(vm.state.value.isRefreshing)
    }
}
