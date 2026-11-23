package org.vicpoo.gasolineproject.ui.triprecording.fuel

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.triprecording.CaptureFuelLocationUseCase
import org.vicpoo.gasolineproject.domain.triprecording.FuelField
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordValidator
import org.vicpoo.gasolineproject.domain.triprecording.GetFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RecordSyncState
import org.vicpoo.gasolineproject.domain.triprecording.RetryFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.SaveFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.testing.FakeAppClock
import org.vicpoo.gasolineproject.testing.FakeCostCenterRepository
import org.vicpoo.gasolineproject.testing.FakeFuelRecordRepository
import org.vicpoo.gasolineproject.testing.FakeLocationProvider
import org.vicpoo.gasolineproject.testing.FakeTripRecordingSync
import org.vicpoo.gasolineproject.testing.FakeTripTracker
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testCostCenter
import org.vicpoo.gasolineproject.testing.testFuelRecord
import org.vicpoo.gasolineproject.testing.testRecordedTrip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Agregar / editar recarga: validación, foto del recibo, envío y "Reintentar" sin duplicar. */
class FuelRecordFormViewModelTest : MainDispatcherTest() {

    private val ventas = testCostCenter("cc-1", "Ventas")
    private val operaciones = testCostCenter("cc-2", "Operaciones")
    private val tracker = FakeTripTracker(TrackingSession(trip = testRecordedTrip(TripPhase.RUNNING, costCenter = ventas)))
    private val costCenters = FakeCostCenterRepository(listOf(ventas, operaciones))
    private val fuel = FakeFuelRecordRepository()
    private val sync = FakeTripRecordingSync()
    private val receipt = PickedFile(name = "recibo.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })
    private val recoded = PickedFile(name = "recibo.jpg", mimeType = "image/jpeg", bytes = ByteArray(5) { 2 })
    private var compressor = ImageCompressor { recoded }

    private fun viewModel(recordId: String? = null): FuelRecordFormViewModel {
        val clock = FakeAppClock()
        val captureLocation = CaptureFuelLocationUseCase(tracker, FakeLocationProvider(), clock)
        return FuelRecordFormViewModel(
            args = FuelRecordFormArgs(recordId),
            getRecord = GetFuelRecordUseCase(fuel),
            observeCostCenters = ObserveCostCentersUseCase(costCenters),
            observeTracking = ObserveTrackingUseCase(tracker),
            saveRecord = SaveFuelRecordUseCase(
                repository = fuel,
                sync = sync,
                tracker = tracker,
                costCenters = costCenters,
                validator = FuelRecordValidator(),
                imageCompressor = ImageCompressor { compressor.compress(it) },
                captureLocation = captureLocation,
            ),
            retryRecord = RetryFuelRecordUseCase(fuel, sync),
            captureLocation = captureLocation,
        )
    }

    private fun FuelRecordFormViewModel.fill(amount: String = "500", liters: String = "20", file: PickedFile? = receipt) {
        onEvent(FuelRecordFormEvent.AmountChanged(amount))
        onEvent(FuelRecordFormEvent.LitersChanged(liters))
        onEvent(FuelRecordFormEvent.ReceiptPicked(file))
    }

    @Test
    fun nueva_eligePorDefectoElCentroDeCostoDelViaje() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertFalse(state.notFound)
        assertFalse(state.isEdit)
        assertEquals("cc-1", state.selectedCostCenterId)
        assertEquals(2, state.costCenters.size)
        assertNotNull(state.tankCapacityText)
    }

    @Test
    fun sinViajeEnCurso_noSePuedeAgregar() = runTest {
        tracker.state.value = TrackingSession()

        val vm = viewModel()

        assertTrue(vm.state.value.notFound)
    }

    @Test
    fun guardar_mandaElReciboRecodificado_yMuestraEnviado() = runTest {
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FuelRecordFormEvent.Save)

        val state = vm.state.value
        assertEquals(SendOutcome.Sent, state.outcome)
        assertEquals("fuel-1", state.savedRecordId)
        assertNull(state.newReceipt, "Ya quedó guardado")
        assertTrue(state.hasCurrentReceipt)
        assertSame(recoded, fuel.savedReceipts.single())
        assertEquals("cc-1", fuel.records.value.single().costCenter?.id)
    }

    @Test
    fun guardarDosVeces_editaLaMismaRecarga_noDuplica() = runTest {
        val vm = viewModel()
        vm.fill()
        vm.onEvent(FuelRecordFormEvent.Save)
        vm.onEvent(FuelRecordFormEvent.ResultDismissed)

        vm.onEvent(FuelRecordFormEvent.LitersChanged("25"))
        vm.onEvent(FuelRecordFormEvent.Save)

        assertEquals(1, fuel.records.value.size)
        assertEquals(25.0, fuel.records.value.single().liters)
    }

    @Test
    fun sinRecibo_marcaElCampo_yNoGuarda() = runTest {
        val vm = viewModel()
        vm.fill(file = null)

        vm.onEvent(FuelRecordFormEvent.Save)

        assertEquals(ValidationReason.REQUIRED, vm.state.value.errors[FuelField.RECEIPT])
        assertTrue(fuel.records.value.isEmpty())
    }

    @Test
    fun litrosMayoresAlTanque_marcaElCampo() = runTest {
        val vm = viewModel()
        vm.fill(liters = "80") // el tanque de prueba es de 50 L

        vm.onEvent(FuelRecordFormEvent.Save)

        assertEquals(ValidationReason.OUT_OF_RANGE, vm.state.value.errors[FuelField.LITERS])
        assertTrue(fuel.records.value.isEmpty())
    }

    @Test
    fun reciboQueNoSePuedeLeer_seRechaza_yNoSeGuardaLaOriginal() = runTest {
        compressor = ImageCompressor { null }
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FuelRecordFormEvent.Save)

        assertEquals(ValidationReason.INVALID_FILE_TYPE, vm.state.value.errors[FuelField.RECEIPT])
        assertTrue(fuel.savedReceipts.isEmpty())
    }

    @Test
    fun corregirUnCampo_quitaSuError() = runTest {
        val vm = viewModel()
        vm.fill(amount = "")
        vm.onEvent(FuelRecordFormEvent.Save)
        assertEquals(ValidationReason.REQUIRED, vm.state.value.errors[FuelField.AMOUNT])

        vm.onEvent(FuelRecordFormEvent.AmountChanged("$1,00a0.50"))

        assertNull(vm.state.value.errors[FuelField.AMOUNT])
        assertEquals("1,000.50", vm.state.value.amount)
    }

    @Test
    fun rechazadaPorElServidor_muestraElMotivo() = runTest {
        fuel.stateAfterSave = RecordSyncState.ERROR
        fuel.errorMessageAfterSave = "Los litros exceden la capacidad del tanque"
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FuelRecordFormEvent.Save)

        assertEquals(SendOutcome.Rejected("Los litros exceden la capacidad del tanque"), vm.state.value.outcome)
    }

    @Test
    fun reintentar_marcaLaRecargaParaReenviar() = runTest {
        fuel.stateAfterSave = RecordSyncState.ERROR
        val vm = viewModel()
        vm.fill()
        vm.onEvent(FuelRecordFormEvent.Save)
        sync.onSync = {
            fuel.records.value = fuel.records.value.map { it.copy(syncState = RecordSyncState.SYNCED) }
        }

        vm.onEvent(FuelRecordFormEvent.Retry)

        assertEquals(listOf("fuel-1"), fuel.retried)
        assertEquals(SendOutcome.Sent, vm.state.value.outcome)
    }

    @Test
    fun editar_cargaLaRecargaGuardada() = runTest {
        fuel.records.value = listOf(testFuelRecord(localId = "fuel-7", costCenter = operaciones))

        val vm = viewModel(recordId = "fuel-7")

        val state = vm.state.value
        assertTrue(state.isEdit)
        assertTrue(state.hasCurrentReceipt)
        assertEquals("cc-2", state.selectedCostCenterId)
        assertTrue(state.liters.isNotEmpty())
        assertTrue(state.amount.isNotEmpty())
    }

    @Test
    fun editarSinCambiarElRecibo_seGuardaSinFotoNueva() = runTest {
        fuel.records.value = listOf(testFuelRecord(localId = "fuel-7", costCenter = operaciones))
        val vm = viewModel(recordId = "fuel-7")

        vm.onEvent(FuelRecordFormEvent.LitersChanged("30"))
        vm.onEvent(FuelRecordFormEvent.Save)

        assertEquals(SendOutcome.Sent, vm.state.value.outcome)
        assertNull(fuel.savedReceipts.single())
        assertEquals(30.0, fuel.records.value.single().liters)
    }

    @Test
    fun editarUnaRecargaQueNoExiste_muestraNoEncontrada() = runTest {
        val vm = viewModel(recordId = "no-existe")

        assertTrue(vm.state.value.notFound)
    }
}
