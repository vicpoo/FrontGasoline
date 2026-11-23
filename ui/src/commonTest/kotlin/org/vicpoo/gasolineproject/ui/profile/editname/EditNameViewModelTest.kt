package org.vicpoo.gasolineproject.ui.profile.editname

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.profile.ObserveProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.UpdateNameUseCase
import org.vicpoo.gasolineproject.testing.FakeProfileRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Editar nombre: precarga, obligatorio, límite de 150 y errores al guardar. */
class EditNameViewModelTest : MainDispatcherTest() {

    private val repository = FakeProfileRepository()

    private fun viewModel() = EditNameViewModel(
        observeProfile = ObserveProfileUseCase(repository),
        updateName = UpdateNameUseCase(repository),
    )

    @Test
    fun alAbrir_muestraElNombreActual() = runTest {
        assertEquals("Juan Pérez", viewModel().state.value.name)
    }

    @Test
    fun guardar_mandaElNombreLimpio() = runTest {
        val vm = viewModel()

        vm.onEvent(EditNameEvent.NameChanged("  Juan Carlos Pérez  "))
        vm.onEvent(EditNameEvent.Save)

        assertEquals(listOf("Juan Carlos Pérez"), repository.names)
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun nombreVacio_marcaElCampo() = runTest {
        val vm = viewModel()

        vm.onEvent(EditNameEvent.NameChanged("   "))
        vm.onEvent(EditNameEvent.Save)

        assertEquals(ValidationReason.REQUIRED, vm.state.value.fieldError)
        assertTrue(repository.names.isEmpty())

        vm.onEvent(EditNameEvent.NameChanged("Juan"))
        assertNull(vm.state.value.fieldError)
    }

    @Test
    fun nombreMuyLargo_seCortaAlEscribir() = runTest {
        val vm = viewModel()

        vm.onEvent(EditNameEvent.NameChanged("a".repeat(UpdateNameUseCase.MAX_NAME + 20)))

        assertEquals(UpdateNameUseCase.MAX_NAME, vm.state.value.name.length)
    }

    @Test
    fun siFallaAlGuardar_muestraElDialogo_ySePuedeCerrar() = runTest {
        repository.updateNameResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()

        vm.onEvent(EditNameEvent.Save)
        assertEquals(AppError.NoConnection, vm.state.value.saveError)
        assertFalse(vm.state.value.saved)

        vm.onEvent(EditNameEvent.DismissError)
        assertNull(vm.state.value.saveError)
    }
}
