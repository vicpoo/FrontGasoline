package org.vicpoo.gasolineproject.ui.reservations.list

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.push.PushTarget
import org.vicpoo.gasolineproject.domain.push.PushType
import org.vicpoo.gasolineproject.domain.reservations.LoadMoreMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.MarkReservationUpdatesSeenUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshMyReservationsUseCase
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

/** "Mis solicitudes" (campana): filtros con conteo, grupos por mes, vistas, paginación y push. */
class MyReservationsViewModelTest : MainDispatcherTest() {

    private val repository = FakeReservationRepository(
        initialMine = listOf(
            testReservation("req-1", ReservationStatus.PENDING, requestedAt = "2026-11-20T09:00:00"),
            testReservation("req-2", ReservationStatus.NOT_SENT, requestedAt = "2026-11-21T09:00:00"),
            testReservation("req-3", ReservationStatus.APPROVED, requestedAt = "2026-10-15T09:00:00"),
            testReservation("req-4", ReservationStatus.REJECTED, requestedAt = "2026-10-02T09:00:00"),
        ),
    )
    private val push = PushEvents()
    private val network = FakeConnectivityMonitor(initial = true)
    private val lastRefresh = FakeLastRefreshStore(now = { 42L })

    private fun viewModel() = MyReservationsViewModel(
        observeRequests = ObserveMyReservationsUseCase(repository),
        refreshRequests = RefreshMyReservationsUseCase(repository),
        loadMoreRequests = LoadMoreMyReservationsUseCase(repository),
        markUpdatesSeen = MarkReservationUpdatesSeenUseCase(repository),
        pushEvents = push,
        connectivity = network,
        lastRefresh = lastRefresh,
    )

    @Test
    fun alAbrir_refresca_yMarcaLasRespuestasComoVistas() = runTest {
        repository.unseen.value = 3

        val vm = viewModel()

        assertEquals(1, repository.count("refreshMine"))
        assertEquals(1, repository.count("markUpdatesSeen"))
        assertEquals(0, repository.unseen.value)
        assertEquals(42L, vm.state.value.lastUpdatedAtMillis)
        assertFalse(vm.state.value.isInitialLoading)
    }

    @Test
    fun cuentaPorFiltro_yAgrupaPorMes() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertEquals(4, state.counts[MyReservationsFilter.ALL])
        assertEquals(2, state.counts[MyReservationsFilter.PENDING], "Incluye la que espera enviarse")
        assertEquals(1, state.counts[MyReservationsFilter.APPROVED])
        assertEquals(1, state.counts[MyReservationsFilter.REJECTED])
        assertEquals(listOf("2026-11", "2026-10"), state.groups.map { it.key })
    }

    @Test
    fun elegirFiltro_soloMuestraEsasSolicitudes() = runTest {
        val vm = viewModel()

        vm.onEvent(MyReservationsEvent.FilterSelected(MyReservationsFilter.PENDING))

        assertEquals(MyReservationsFilter.PENDING, vm.state.value.filter)
        assertEquals(listOf("req-2", "req-1"), vm.state.value.groups.flatMap { g -> g.requests.map { it.id } })
    }

    @Test
    fun sinRed_seMarcaSinConexion_sinError() = runTest {
        repository.refreshResult = AppResult.Failure(AppError.NoConnection)

        val vm = viewModel()

        assertTrue(vm.state.value.isOffline)
        assertNull(vm.state.value.error)
    }

    @Test
    fun errorDelServidor_seMuestra() = runTest {
        repository.refreshResult = AppResult.Failure(AppError.Server(500, null))

        val vm = viewModel()

        assertEquals(AppError.Server(500, null), vm.state.value.error)
    }

    @Test
    fun alLlegarAlFinal_conMas_pideLasSiguientes() = runTest {
        repository.mineHasMore.value = true
        val vm = viewModel()

        vm.onEvent(MyReservationsEvent.LoadMore)

        assertEquals(1, repository.count("loadMoreMine"))
    }

    @Test
    fun alLlegarAlFinal_sinMas_noPideNada() = runTest {
        val vm = viewModel()

        vm.onEvent(MyReservationsEvent.LoadMore)

        assertEquals(0, repository.count("loadMoreMine"))
    }

    @Test
    fun pushConLaRespuesta_actualizaLaLista() = runTest {
        viewModel()

        push.notifyReceived(PushTarget(PushType.REQUEST_RESULT, "req-1"))

        assertEquals(2, repository.count("refreshMine"))
    }

    @Test
    fun alVolverLaRed_recargaSola() = runTest {
        network.online.value = false
        viewModel()

        network.online.value = true

        assertEquals(2, repository.count("refreshMine"))
    }
}
