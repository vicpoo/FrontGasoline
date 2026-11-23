package org.vicpoo.gasolineproject.ui.trips.detail

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.trips.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.domain.trips.ObserveTripUseCase
import org.vicpoo.gasolineproject.testing.FakeTripsRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Detalle de un viaje: lo guardado al instante, pide sus cargas y ruta, y el visor del recibo. */
@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailViewModelTest : MainDispatcherTest() {

    private val repository = FakeTripsRepository(listOf(testTrip("t1"), testTrip("t2")))

    private fun TestScope.viewModel(id: String = "t2"): TripDetailViewModel {
        val vm = TripDetailViewModel(TripDetailArgs(id), ObserveTripUseCase(repository), LoadTripDetailsUseCase(repository))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test
    fun alAbrir_muestraElViaje_yPideSusDetalles() = runTest {
        val vm = viewModel()

        assertEquals("t2", vm.state.value.trip?.id)
        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf(listOf("t2")), repository.detailRequests)
    }

    @Test
    fun viajeQueNoExiste_noMuestraNada() = runTest {
        val vm = viewModel("no-existe")

        assertNull(vm.state.value.trip)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun visorDelRecibo_seAbreYSeCierra() = runTest {
        val vm = viewModel()

        vm.openReceipt("http://13.222.72.73/fotos/recibo.jpg")
        assertEquals("http://13.222.72.73/fotos/recibo.jpg", vm.state.value.openReceiptUrl)

        vm.closeReceipt()
        assertNull(vm.state.value.openReceiptUrl)
    }
}
