package org.vicpoo.gasolineproject.ui.reservations.bell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.push.PushTarget
import org.vicpoo.gasolineproject.domain.push.PushType
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.RefreshUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.testing.FakeReservationRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest

/** Badge de la campana: respuestas sin ver, se actualiza al abrir y con el push de respuesta. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReservationsBellViewModelTest : MainDispatcherTest() {

    private val repository = FakeReservationRepository()
    private val push = PushEvents()

    private fun viewModel() = ReservationsBellViewModel(
        observeUnseen = ObserveUnseenReservationUpdatesUseCase(repository),
        refreshUnseen = RefreshUnseenReservationUpdatesUseCase(repository),
        pushEvents = push,
    )

    @Test
    fun muestraLasRespuestasSinVer_yRefrescaAlAbrir() = runTest {
        repository.unseen.value = 2
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.unseen.collect {} }

        assertEquals(2, vm.unseen.value)
        assertEquals(1, repository.count("refreshUnseen"))

        repository.unseen.value = 3
        assertEquals(3, vm.unseen.value)
    }

    @Test
    fun pushDeRespuesta_vuelveAContar() = runTest {
        viewModel()

        push.notifyReceived(PushTarget(PushType.REQUEST_RESULT, "req-1"))

        assertEquals(2, repository.count("refreshUnseen"))
    }

    @Test
    fun otroPush_noCuentaDeNuevo() = runTest {
        viewModel()

        push.notifyReceived(PushTarget(PushType.NEW_REQUEST, "req-1"))

        assertEquals(1, repository.count("refreshUnseen"))
    }
}
