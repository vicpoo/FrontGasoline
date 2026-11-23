package org.vicpoo.gasolineproject.ui.vehicles.form

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleType
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.SaveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleField
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormValidator
import org.vicpoo.gasolineproject.testing.FakeVehicleRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testVehicle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Formulario de vehículo por secciones: hojas, validación, foto, guardar y editar. */
@OptIn(ExperimentalCoroutinesApi::class)
class VehicleFormViewModelTest : MainDispatcherTest() {

    private val repository = FakeVehicleRepository(listOf(testVehicle(id = "veh-1")))
    private val photo = PickedFile(name = "versa.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })
    private val recoded = PickedFile(name = "versa.jpg", mimeType = "image/jpeg", bytes = ByteArray(5) { 2 })

    private fun viewModel(vehicleId: String? = null) = VehicleFormViewModel(
        args = VehicleFormArgs(vehicleId),
        observeVehicle = ObserveVehicleUseCase(repository),
        saveVehicle = SaveVehicleUseCase(repository, VehicleFormValidator(), ImageCompressor { recoded }),
        validator = VehicleFormValidator(),
    )

    private fun TestScope.effectsOf(vm: VehicleFormViewModel): List<VehicleFormEffect> {
        val effects = mutableListOf<VehicleFormEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        return effects
    }

    private fun VehicleFormViewModel.fillSection(section: VehicleFormSection, vararg values: Pair<VehicleField, String>) {
        onEvent(VehicleFormEvent.OpenSection(section))
        values.forEach { (field, value) -> onEvent(VehicleFormEvent.FieldChanged(field, value)) }
        onEvent(VehicleFormEvent.ConfirmSection)
    }

    private fun VehicleFormViewModel.fillAll() {
        fillSection(VehicleFormSection.BASICS, VehicleField.BRAND to "Chevrolet", VehicleField.MODEL to "Aveo")
        fillSection(
            VehicleFormSection.IDENTIFICATION,
            VehicleField.PLATES to "xyz-987",
            VehicleField.YEAR to "2020",
            VehicleField.COLOR to "Rojo",
        )
        fillSection(VehicleFormSection.SPECS, VehicleField.MILEAGE to "32000", VehicleField.TANK to "45")
    }

    @Test
    fun nuevo_empiezaVacio() = runTest {
        val vm = viewModel()

        assertFalse(vm.state.value.isEdit)
        assertTrue(vm.state.value.completed.isEmpty())
    }

    @Test
    fun confirmarUnaSeccion_laPasaALaTarjeta() = runTest {
        val vm = viewModel()

        vm.fillSection(VehicleFormSection.BASICS, VehicleField.BRAND to "Chevrolet", VehicleField.MODEL to "Aveo")

        val state = vm.state.value
        assertNull(state.editing)
        assertEquals("Chevrolet", state.input.brand)
        assertEquals(setOf(VehicleFormSection.BASICS), state.completed)
    }

    @Test
    fun seccionConCamposVacios_marcaLosErrores_yLaHojaSigueAbierta() = runTest {
        val vm = viewModel()

        vm.fillSection(VehicleFormSection.BASICS, VehicleField.BRAND to "Chevrolet")

        assertEquals(VehicleFormSection.BASICS, vm.state.value.editing)
        assertEquals(ValidationReason.REQUIRED, vm.state.value.draftErrors[VehicleField.MODEL])
        assertTrue(vm.state.value.completed.isEmpty())
    }

    @Test
    fun cancelar_descartaLoQueSeEscribioEnLaHoja() = runTest {
        val vm = viewModel()
        vm.onEvent(VehicleFormEvent.OpenSection(VehicleFormSection.BASICS))
        vm.onEvent(VehicleFormEvent.FieldChanged(VehicleField.BRAND, "Chevrolet"))

        vm.onEvent(VehicleFormEvent.CancelSection)

        assertNull(vm.state.value.editing)
        assertEquals("", vm.state.value.input.brand)
    }

    @Test
    fun guardarIncompleto_abreLaPrimeraSeccionConError() = runTest {
        val vm = viewModel()
        vm.fillSection(VehicleFormSection.BASICS, VehicleField.BRAND to "Chevrolet", VehicleField.MODEL to "Aveo")

        vm.onEvent(VehicleFormEvent.Save)

        assertEquals(VehicleFormSection.IDENTIFICATION, vm.state.value.editing)
        assertTrue(vm.state.value.hasError(VehicleFormSection.IDENTIFICATION))
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun guardarCompleto_creaElVehiculo_conLaFotoRecodificada() = runTest {
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fillAll()
        vm.onEvent(VehicleFormEvent.OpenSection(VehicleFormSection.PHOTO))
        vm.onEvent(VehicleFormEvent.PhotoPicked(photo))
        vm.onEvent(VehicleFormEvent.ConfirmSection)
        vm.onEvent(VehicleFormEvent.OpenSection(VehicleFormSection.BASICS))
        vm.onEvent(VehicleFormEvent.TypeChanged(VehicleType.PICKUP))
        vm.onEvent(VehicleFormEvent.ConfirmSection)

        vm.onEvent(VehicleFormEvent.Save)

        val draft = repository.created.single()
        assertEquals("XYZ-987", draft.plates)
        assertEquals(32_000.0, draft.mileageKm)
        assertEquals(VehicleType.PICKUP, draft.type)
        assertSame(recoded, draft.newPhoto)
        assertEquals(listOf<VehicleFormEffect>(VehicleFormEffect.Saved), effects)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun fotoDeTipoNoPermitido_muestraElError() = runTest {
        val vm = viewModel()
        vm.onEvent(VehicleFormEvent.OpenSection(VehicleFormSection.PHOTO))

        vm.onEvent(VehicleFormEvent.PhotoPicked(PickedFile(name = "a.gif", mimeType = "image/gif", bytes = ByteArray(3))))

        assertEquals(ValidationReason.INVALID_FILE_TYPE, vm.state.value.photoError)
        assertNull(vm.state.value.draft.newPhoto)
    }

    @Test
    fun siFallaAlGuardar_muestraElError() = runTest {
        repository.failure = AppError.Server(409, "Placas repetidas")
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fillAll()

        vm.onEvent(VehicleFormEvent.Save)

        assertEquals(AppError.Server(409, "Placas repetidas"), vm.state.value.error)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun editar_cargaElVehiculo_conTodasLasSeccionesLlenas() = runTest {
        val vm = viewModel("veh-1")

        val state = vm.state.value
        assertTrue(state.isEdit)
        assertFalse(state.isLoading)
        assertEquals("Nissan", state.input.brand)
        assertEquals("2022", state.input.year)
        assertEquals(VehicleFormSection.entries.toSet(), state.completed)
        assertTrue(state.existingPhoto != null)
    }

    @Test
    fun editarUnoQueNoExiste_muestraNoEncontrado() = runTest {
        val vm = viewModel("no-existe")

        assertEquals(AppError.NotFound, vm.state.value.error)
    }

    @Test
    fun editarYQuitarLaFoto_laQuita() = runTest {
        val vm = viewModel("veh-1")
        vm.onEvent(VehicleFormEvent.OpenSection(VehicleFormSection.PHOTO))
        vm.onEvent(VehicleFormEvent.RemovePhoto)
        vm.onEvent(VehicleFormEvent.ConfirmSection)

        vm.onEvent(VehicleFormEvent.Save)

        val (id, draft) = repository.updated.single()
        assertEquals("veh-1", id)
        assertTrue(draft.removePhoto)
        assertNull(repository.vehicles.value.single().photo)
    }
}
