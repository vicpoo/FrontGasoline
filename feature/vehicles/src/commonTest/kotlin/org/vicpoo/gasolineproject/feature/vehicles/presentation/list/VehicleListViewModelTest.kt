package org.vicpoo.gasolineproject.feature.vehicles.presentation.list

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.repository.VehicleReader
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObservePendingVehicleSyncUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObserveVehiclesUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.RefreshVehiclesUseCase
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.FakeLastRefreshStore
import org.vicpoo.gasolineproject.testing.FakePermissionChecker
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Lista de vehículos: modo sin conexión, recarga al volver la red y "Actualizado hace X". */
@OptIn(ExperimentalCoroutinesApi::class)
class VehicleListViewModelTest : MainDispatcherTest() {

    /** Lectura de vehículos en memoria; cuenta cuántas veces se pidió refrescar. */
    private class FakeVehicleReader : VehicleReader {
        val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
        var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
        var refreshCalls = 0

        override fun observeVehicles(): Flow<List<Vehicle>> = vehicles
        override fun observeVehicle(id: String): Flow<Vehicle?> = vehicles.map { list -> list.firstOrNull { it.id == id } }
        override fun observePendingSyncCount(): Flow<Int> = flowOf(0)
        override suspend fun refresh(): AppResult<Unit> {
            refreshCalls++
            return refreshResult
        }
    }

    private val reader = FakeVehicleReader()
    private val network = FakeConnectivityMonitor(initial = true)
    private val lastRefresh = FakeLastRefreshStore(now = { 42L })

    private fun viewModel(mode: VehiclesMode = VehiclesMode.FLEET) = VehicleListViewModel(
        args = VehicleListArgs(mode),
        observeVehicles = ObserveVehiclesUseCase(reader),
        observePendingSync = ObservePendingVehicleSyncUseCase(reader),
        refreshVehicles = RefreshVehiclesUseCase(reader),
        permissionChecker = FakePermissionChecker(setOf(Privileges.CARRO_CREAR)),
        connectivity = network,
        lastRefresh = lastRefresh,
    )

    @Test
    fun conRed_alAbrir_refresca_yGuardaCuandoSeActualizo() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(1, reader.refreshCalls)
        assertFalse(vm.state.value.isOffline)
        assertEquals(42L, vm.state.value.lastUpdatedAtMillis)
    }

    @Test
    fun sinRed_seMarcaSinConexion_aunqueNoHayaFalladoNada() = runTest {
        network.online.value = false
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertTrue(vm.state.value.isOffline)
    }

    @Test
    fun peticionSinRed_marcaSinConexion_yNoActualizaLaHora() = runTest {
        reader.refreshResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertTrue(vm.state.value.isOffline)
        assertNull(vm.state.value.error, "Sin red no es un error: se muestra el aviso")
        assertNull(vm.state.value.lastUpdatedAtMillis)
    }

    @Test
    fun alVolverLaRed_recargaSola_yQuitaElAviso() = runTest {
        network.online.value = false
        reader.refreshResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(1, reader.refreshCalls)

        reader.refreshResult = AppResult.Success(Unit)
        network.online.value = true

        assertEquals(2, reader.refreshCalls)
        assertFalse(vm.state.value.isOffline)
    }

    @Test
    fun soloEnGestion_sePuedeAgregar() = runTest {
        val fleet = viewModel(VehiclesMode.FLEET)
        val catalog = viewModel(VehiclesMode.CATALOG)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fleet.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { catalog.state.collect {} }

        assertTrue(fleet.state.value.canAdd)
        assertFalse(catalog.state.value.canAdd)
    }
}
