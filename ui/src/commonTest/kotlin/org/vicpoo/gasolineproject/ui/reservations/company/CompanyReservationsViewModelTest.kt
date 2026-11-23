package org.vicpoo.gasolineproject.ui.reservations.company

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.push.PushTarget
import org.vicpoo.gasolineproject.domain.push.PushType
import org.vicpoo.gasolineproject.domain.reservations.LoadMoreCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.FakeLastRefreshStore
import org.vicpoo.gasolineproject.testing.FakeReservationRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testReservation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pestaña llave: filtro de pendientes, paginación, sin conexión y push de solicitud nueva. */
class CompanyReservationsViewModelTest : MainDispatcherTest() {

    private val repository = FakeReservationRepository(
        initialCompany = listOf(
            testReservation("req-1", ReservationStatus.PENDING),
            testReservation("req-2", ReservationStatus.APPROVED),
            testReservation("req-3", ReservationStatus.PENDING),
        ),
    )
    private val push = PushEvents()
    private val network = FakeConnectivityMonitor(initial = true)
    private val lastRefresh = FakeLastRefreshStore(now = { 42L })

    private fun viewModel() = CompanyReservationsViewModel(
        observeRequests = ObserveCompanyReservationsUseCase(repository),
        refreshRequests = RefreshCompanyReservationsUseCase(repository),
        loadMoreRequests = LoadMoreCompanyReservationsUseCase(repository),
        pushEvents = push,
        connectivity = network,
        lastRefresh = lastRefresh,
    )

    @Test
    fun porDefecto_soloPendientes_yCuentaLasPendientes() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertEquals(CompanyReservationsFilter.PENDING, state.filter)
        assertEquals(listOf("req-1", "req-3"), state.requests.map { it.id })
        assertEquals(2, state.pendingCount)
        assertEquals(1, repository.count("refreshCompany"))
        assertEquals(42L, state.lastUpdatedAtMillis)
    }

    @Test
    fun filtroTodas_muestraTodas() = runTest {
        val vm = viewModel()

        vm.onEvent(CompanyReservationsEvent.FilterChanged(CompanyReservationsFilter.ALL))

        assertEquals(3, vm.state.value.requests.size)
        assertEquals(2, vm.state.value.pendingCount)
    }

    @Test
    fun hayMas_soloSeMuestraEnTodas() = runTest {
        repository.companyHasMore.value = true
        val vm = viewModel()
        assertFalse(vm.state.value.hasMore, "Las pendientes ya llegan todas")

        vm.onEvent(CompanyReservationsEvent.FilterChanged(CompanyReservationsFilter.ALL))

        assertTrue(vm.state.value.hasMore)
    }

    @Test
    fun alLlegarAlFinal_enTodas_pideLasSiguientes() = runTest {
        repository.companyHasMore.value = true
        val vm = viewModel()
        vm.onEvent(CompanyReservationsEvent.FilterChanged(CompanyReservationsFilter.ALL))

        vm.onEvent(CompanyReservationsEvent.LoadMore)

        assertEquals(1, repository.count("loadMoreCompany"))
    }

    @Test
    fun alLlegarAlFinal_enPendientes_noPideMas() = runTest {
        repository.companyHasMore.value = true
        val vm = viewModel()

        vm.onEvent(CompanyReservationsEvent.LoadMore)

        assertEquals(0, repository.count("loadMoreCompany"))
    }

    @Test
    fun siPedirMasFallaSinRed_seMarcaSinConexion() = runTest {
        repository.companyHasMore.value = true
        repository.loadMoreResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()
        vm.onEvent(CompanyReservationsEvent.FilterChanged(CompanyReservationsFilter.ALL))

        vm.onEvent(CompanyReservationsEvent.LoadMore)

        assertTrue(vm.state.value.isOffline)
    }

    @Test
    fun sinRed_seMarcaSinConexion_sinError() = runTest {
        repository.refreshResult = AppResult.Failure(AppError.NoConnection)

        val vm = viewModel()

        assertTrue(vm.state.value.isOffline)
        assertNull(vm.state.value.error)
        assertNull(vm.state.value.lastUpdatedAtMillis)
    }

    @Test
    fun alVolverLaRed_recargaSola() = runTest {
        network.online.value = false
        val vm = viewModel()

        network.online.value = true

        assertEquals(2, repository.count("refreshCompany"))
        assertFalse(vm.state.value.isOffline)
    }

    @Test
    fun pushDeSolicitudNueva_actualizaLaLista() = runTest {
        viewModel()

        push.notifyReceived(PushTarget(PushType.NEW_REQUEST, "req-9"))

        assertEquals(2, repository.count("refreshCompany"))
    }

    @Test
    fun otroTipoDePush_noActualiza() = runTest {
        viewModel()

        push.notifyReceived(PushTarget(PushType.REQUEST_RESULT, "req-9"))

        assertEquals(1, repository.count("refreshCompany"))
    }

    @Test
    fun deslizarParaRecargar_vuelveAPedir() = runTest {
        val vm = viewModel()

        vm.onEvent(CompanyReservationsEvent.Refresh)

        assertEquals(2, repository.count("refreshCompany"))
        assertFalse(vm.state.value.isRefreshing)
    }
}
