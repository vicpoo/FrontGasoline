package org.vicpoo.gasolineproject.ui.triprecording.finish

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripField
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripValidator
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.FinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.RetryFinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.StopTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.UndoStopTripUseCase
import org.vicpoo.gasolineproject.testing.FakeAppClock
import org.vicpoo.gasolineproject.testing.FakeTripTracker
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testRecordedTrip

/** "Registrar Viaje": kilometraje final, foto del odómetro y resultado del envío. */
class FinishTripViewModelTest : MainDispatcherTest() {

    private val tracker = FakeTripTracker(TrackingSession(trip = testRecordedTrip(TripPhase.STOPPED)))
    private val photo = PickedFile(name = "odometro.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })
    private val recoded = PickedFile(name = "odometro.jpg", mimeType = "image/jpeg", bytes = ByteArray(5) { 2 })
    private var compressor = ImageCompressor { recoded }

    private fun TestScope.viewModel(forLogout: Boolean = false) = FinishTripViewModel(
        args = FinishTripArgs(forLogout),
        observeTracking = ObserveTrackingUseCase(tracker),
        finishTrip = FinishTripUseCase(tracker, FinishTripValidator(), ImageCompressor { compressor.compress(it) }),
        retryFinish = RetryFinishTripUseCase(tracker),
        stopTrip = StopTripUseCase(tracker),
        undoStop = UndoStopTripUseCase(tracker),
        clock = FakeAppClock(now = 60_000L),
        appScope = AppScope(backgroundScope),
    )

    private fun FinishTripViewModel.fill(mileage: String = "1500", file: PickedFile? = photo) {
        onEvent(FinishTripEvent.FinalMileageChanged(mileage))
        onEvent(FinishTripEvent.PhotoPicked(file))
    }

    @Test
    fun sinViajeDetenido_noHayNadaQueRegistrar() = runTest {
        tracker.state.value = TrackingSession(trip = testRecordedTrip(TripPhase.RUNNING))

        val vm = viewModel()

        assertTrue(vm.state.value.notFound)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun conViajeDetenido_cargaLosDatos() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertFalse(state.notFound)
        assertFalse(state.isLoading)
        assertTrue(state.initialMileageText.isNotEmpty())
        assertEquals("", state.finalMileage)
    }

    @Test
    fun siElServidorLoRechazoAntes_muestraElKilometrajeQueYaSeHabiaCapturado() = runTest {
        tracker.state.value = TrackingSession(trip = testRecordedTrip(TripPhase.STOPPED, finalMileage = 1_500.0))

        val vm = viewModel()

        assertTrue(vm.state.value.finalMileage.isNotEmpty())
    }

    @Test
    fun elKilometraje_soloAceptaNumerosYUnPunto() = runTest {
        val vm = viewModel()

        vm.onEvent(FinishTripEvent.FinalMileageChanged("1,2a3.4.5 km"))

        assertEquals("1,23.45", vm.state.value.finalMileage)
    }

    @Test
    fun guardar_mandaElKilometrajeYLaFotoRecodificada() = runTest {
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FinishTripEvent.Save)

        assertEquals(1_500.0, tracker.finishedWith?.first)
        assertSame(recoded, tracker.finishedWith?.second)
        assertEquals(SendOutcome.Sent, vm.state.value.outcome)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun guardarNormal_noSeMarcaComoCierreDeSesion() = runTest {
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FinishTripEvent.Save)

        assertEquals(false, tracker.finishedByLogout)
    }

    // --- Modo salir (abierto desde "Salir" en Perfil) ---

    @Test
    fun modoSalir_conViajeEnCurso_loDetieneYCargaLosDatos() = runTest {
        tracker.state.value = TrackingSession(trip = testRecordedTrip(TripPhase.RUNNING))

        val vm = viewModel(forLogout = true)

        assertTrue("stop" in tracker.calls)
        assertTrue(vm.state.value.forLogout)
        assertFalse(vm.state.value.notFound)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun modoSalir_conViajeEnPausa_loDetiene() = runTest {
        tracker.state.value = TrackingSession(trip = testRecordedTrip(TripPhase.PAUSED))

        val vm = viewModel(forLogout = true)

        assertTrue("stop" in tracker.calls)
        assertFalse(vm.state.value.notFound)
    }

    @Test
    fun modoSalir_yaDetenido_noLoVuelveADetener() = runTest {
        val vm = viewModel(forLogout = true)

        assertFalse("stop" in tracker.calls)
        assertFalse(vm.state.value.notFound)
    }

    @Test
    fun modoSalir_guardar_cierraElViajePorCierreDeSesion() = runTest {
        tracker.state.value = TrackingSession(trip = testRecordedTrip(TripPhase.RUNNING))
        tracker.finishResult = AppResult.Success(SendOutcome.SavedOffline)
        val vm = viewModel(forLogout = true)
        vm.fill()

        vm.onEvent(FinishTripEvent.Save)
        vm.onEvent(FinishTripEvent.ResultDismissed)

        assertEquals(true, tracker.finishedByLogout)
        assertEquals(1_500.0, tracker.finishedWith?.first)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun modoSalir_sinKilometrajeNiFoto_noSeGuarda() = runTest {
        tracker.state.value = TrackingSession(trip = testRecordedTrip(TripPhase.RUNNING))
        val vm = viewModel(forLogout = true)

        vm.onEvent(FinishTripEvent.Save)

        assertNull(tracker.finishedWith)
        assertTrue(FinishTripField.FINAL_MILEAGE in vm.state.value.errors)
        assertTrue(FinishTripField.ODOMETER_PHOTO in vm.state.value.errors)
        assertFalse(vm.state.value.done)
    }

    @Test
    fun guardadoSinRed_alCerrarElDialogo_terminaElFlujo() = runTest {
        tracker.finishResult = AppResult.Success(SendOutcome.SavedOffline)
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FinishTripEvent.Save)
        vm.onEvent(FinishTripEvent.ResultDismissed)

        assertTrue(vm.state.value.done)
        assertNull(vm.state.value.outcome)
    }

    @Test
    fun rechazado_alCerrarElDialogo_seQuedaParaCorregir() = runTest {
        tracker.finishResult = AppResult.Success(SendOutcome.Rejected("El kilometraje es menor al inicial"))
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FinishTripEvent.Save)
        assertEquals(SendOutcome.Rejected("El kilometraje es menor al inicial"), vm.state.value.outcome)

        vm.onEvent(FinishTripEvent.ResultDismissed)
        assertFalse(vm.state.value.done)
    }

    @Test
    fun sinFoto_marcaElCampo_yNoGuarda() = runTest {
        val vm = viewModel()
        vm.fill(file = null)

        vm.onEvent(FinishTripEvent.Save)

        assertEquals(ValidationReason.REQUIRED, vm.state.value.errors[FinishTripField.ODOMETER_PHOTO])
        assertFalse("finish" in tracker.calls)
    }

    @Test
    fun kilometrajeMenorAlInicial_marcaElCampo_yNoGuarda() = runTest {
        val vm = viewModel()
        vm.fill(mileage = "500")

        vm.onEvent(FinishTripEvent.Save)

        assertEquals(ValidationReason.OUT_OF_RANGE, vm.state.value.errors[FinishTripField.FINAL_MILEAGE])
        assertFalse("finish" in tracker.calls)
    }

    @Test
    fun fotoQueNoSePuedeLeer_seRechaza_yNoSeMandaLaOriginal() = runTest {
        compressor = ImageCompressor { null }
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FinishTripEvent.Save)

        assertEquals(ValidationReason.INVALID_FILE_TYPE, vm.state.value.errors[FinishTripField.ODOMETER_PHOTO])
        assertFalse("finish" in tracker.calls)
    }

    @Test
    fun corregirElCampo_quitaSuError() = runTest {
        val vm = viewModel()
        vm.fill(file = null)
        vm.onEvent(FinishTripEvent.Save)

        vm.onEvent(FinishTripEvent.PhotoPicked(photo))

        assertNull(vm.state.value.errors[FinishTripField.ODOMETER_PHOTO])
    }

    @Test
    fun siFallaAlGuardar_muestraElError() = runTest {
        tracker.finishResult = AppResult.Failure(AppError.NotFound)
        val vm = viewModel()
        vm.fill()

        vm.onEvent(FinishTripEvent.Save)

        assertEquals(AppError.NotFound, vm.state.value.failure)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun reintentar_vuelveAEnviar_yMuestraElNuevoResultado() = runTest {
        tracker.finishResult = AppResult.Success(SendOutcome.SavedOffline)
        tracker.retryOutcome = SendOutcome.Sent
        val vm = viewModel()
        vm.fill()
        vm.onEvent(FinishTripEvent.Save)

        vm.onEvent(FinishTripEvent.Retry)

        assertEquals(SendOutcome.Sent, vm.state.value.outcome)
        assertTrue("retryFinishSend" in tracker.calls)
    }

    @Test
    fun opcionesDeFoto_seAbrenYSeCierran() = runTest {
        val vm = viewModel()

        vm.onEvent(FinishTripEvent.PhotoClicked)
        assertTrue(vm.state.value.showPhotoOptions)

        vm.onEvent(FinishTripEvent.PhotoPicked(null)) // canceló
        assertFalse(vm.state.value.showPhotoOptions)
        assertNull(vm.state.value.photo)
    }
}
