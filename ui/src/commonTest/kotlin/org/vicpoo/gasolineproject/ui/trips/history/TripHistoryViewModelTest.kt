package org.vicpoo.gasolineproject.ui.trips.history

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
import org.vicpoo.gasolineproject.domain.trips.usecase.LoadMoreTripsUseCase
import org.vicpoo.gasolineproject.domain.trips.usecase.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.domain.trips.usecase.ObserveTripsUseCase
import org.vicpoo.gasolineproject.domain.trips.usecase.RefreshTripsUseCase
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.FakeLastRefreshStore
import org.vicpoo.gasolineproject.testing.FakeTripsRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testTrip

/** Historial: se muestran de 10 en 10, detalles por bloque, más viajes de la API y sin conexión. */
@OptIn(ExperimentalCoroutinesApi::class)
class TripHistoryViewModelTest : MainDispatcherTest() {

    private val repository = FakeTripsRepository(List(25) { testTrip("t$it") })
    private val network = FakeConnectivityMonitor(initial = true)
    private val lastRefresh = FakeLastRefreshStore(now = { 42L })

    private fun TestScope.viewModel(): TripHistoryViewModel {
        val vm = TripHistoryViewModel(
            observeTrips = ObserveTripsUseCase(repository),
            refreshTrips = RefreshTripsUseCase(repository),
            loadTripDetails = LoadTripDetailsUseCase(repository),
            loadMoreTrips = LoadMoreTripsUseCase(repository),
            connectivity = network,
            lastRefresh = lastRefresh,
        )
        // El estado solo se calcula mientras alguien lo escucha (WhileSubscribed)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    private fun ids(range: IntRange) = range.map { "t$it" }

    @Test
    fun alAbrir_muestraLos10MasRecientes_yPideSusDetalles() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertEquals(ids(0..9), state.trips.map { it.id })
        assertTrue(state.hasMore)
        assertEquals(1, repository.refreshCalls)
        assertEquals(listOf(ids(0..9)), repository.detailRequests)
        assertEquals(42L, state.lastUpdatedAtMillis)
        assertFalse(state.isInitialLoading)
    }

    @Test
    fun alBajar_muestra10Mas_yPideSoloLosDetallesDeEsos() = runTest {
        val vm = viewModel()

        vm.loadMore()

        assertEquals(20, vm.state.value.trips.size)
        assertEquals(ids(10..19), repository.detailRequests.last())
        assertEquals(0, repository.loadMoreCalls, "Todavía había guardados: no se pide a la API")
    }

    @Test
    fun alTerminarLosGuardados_pideLosSiguientesALaApi() = runTest {
        repository.hasMore.value = true
        repository.nextPage = List(3) { testTrip("n$it") }
        val vm = viewModel()
        vm.loadMore() // 20
        vm.loadMore() // 25

        vm.loadMore() // se acabaron los guardados: a la API

        assertEquals(1, repository.loadMoreCalls)
        assertEquals(28, vm.state.value.trips.size)
        assertEquals(listOf("n0", "n1", "n2"), repository.detailRequests.last())
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun sinMasEnLaApi_noPideNada() = runTest {
        val vm = viewModel()
        vm.loadMore()
        vm.loadMore()

        vm.loadMore()

        assertEquals(0, repository.loadMoreCalls)
        assertEquals(25, vm.state.value.trips.size)
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun sinRed_muestraLoGuardado_sinError_yNoPideDetalles() = runTest {
        repository.refreshResult = AppResult.Failure(AppError.NoConnection)

        val vm = viewModel()

        assertTrue(vm.state.value.isOffline)
        assertNull(vm.state.value.error)
        assertEquals(10, vm.state.value.trips.size)
        assertTrue(repository.detailRequests.isEmpty())
        assertNull(vm.state.value.lastUpdatedAtMillis)
    }

    @Test
    fun errorDelServidor_seMuestra() = runTest {
        repository.refreshResult = AppResult.Failure(AppError.Server(500, null))

        val vm = viewModel()

        assertEquals(AppError.Server(500, null), vm.state.value.error)
    }

    @Test
    fun jalarParaRecargar_vuelveAPedir_yQuitaElIndicador() = runTest {
        val vm = viewModel()

        vm.refresh()

        assertEquals(2, repository.refreshCalls)
        assertFalse(vm.state.value.isRefreshing)
    }

    @Test
    fun alVolverLaRed_recargaSola() = runTest {
        network.online.value = false
        val vm = viewModel()
        assertTrue(vm.state.value.isOffline)

        network.online.value = true

        assertEquals(2, repository.refreshCalls)
        assertFalse(vm.state.value.isOffline)
    }
}
