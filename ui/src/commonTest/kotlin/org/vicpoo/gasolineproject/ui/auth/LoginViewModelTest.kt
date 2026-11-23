package org.vicpoo.gasolineproject.ui.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.auth.usecase.LoginUseCase
import org.vicpoo.gasolineproject.domain.auth.validation.LoginValidator
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.session.LocalDataOwner
import org.vicpoo.gasolineproject.domain.session.Session
import org.vicpoo.gasolineproject.domain.sync.CountLostOnLogoutUseCase
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.testing.FakeLocalDataOwnerStore
import org.vicpoo.gasolineproject.testing.FakePendingSyncTask
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Login: validación, a dónde entra (licencia o inicio), errores y datos sin enviar de otra cuenta. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : MainDispatcherTest() {

    private class FakeAuthRepository : AuthRepository {
        var result: AppResult<Session> = AppResult.Success(testSession())
        val calls = mutableListOf<Pair<String, Boolean>>()

        override suspend fun login(email: String, password: String, clearLocalData: Boolean): AppResult<Session> {
            calls += email to clearLocalData
            return result
        }

        override suspend fun uploadLicense(file: PickedFile): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun refreshPrivileges(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun logout() = Unit
    }

    private val repository = FakeAuthRepository()
    private val owners = FakeLocalDataOwnerStore()
    private val pending = FakePendingSyncTask(PendingSyncKind.RESERVATIONS, count = 0)

    private fun viewModel() = LoginViewModel(
        login = LoginUseCase(repository, owners, CountLostOnLogoutUseCase(listOf(pending))),
        validator = LoginValidator(),
    )

    private fun TestScope.effectsOf(vm: LoginViewModel): List<LoginEffect> {
        val effects = mutableListOf<LoginEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        return effects
    }

    private fun LoginViewModel.fill(email: String = "Test@PolarSoft.mx ", password: String = "secreta") {
        onEvent(LoginEvent.EmailChanged(email))
        onEvent(LoginEvent.PasswordChanged(password))
    }

    @Test
    fun camposVacios_marcaLosErrores_yNoLlamaALaApi() = runTest {
        val vm = viewModel()

        vm.onEvent(LoginEvent.Submit)

        assertEquals(ValidationReason.REQUIRED, vm.state.value.emailError)
        assertEquals(ValidationReason.REQUIRED, vm.state.value.passwordError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun escribir_quitaElErrorDelCampo() = runTest {
        val vm = viewModel()
        vm.onEvent(LoginEvent.Submit)

        vm.onEvent(LoginEvent.EmailChanged("a@b.mx"))

        assertNull(vm.state.value.emailError)
        assertEquals(ValidationReason.REQUIRED, vm.state.value.passwordError)
    }

    @Test
    fun loginCorrecto_vaAlInicio_conElCorreoNormalizado() = runTest {
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fill()

        vm.onEvent(LoginEvent.Submit)

        assertEquals(listOf<LoginEffect>(LoginEffect.GoToHome), effects)
        assertEquals("test@polarsoft.mx", repository.calls.single().first)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun quienManejaSinLicencia_vaASubirLaLicencia() = runTest {
        repository.result = AppResult.Success(testSession(privileges = setOf(Privileges.VIAJE_REGISTRAR), requiresLicense = true))
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fill()

        vm.onEvent(LoginEvent.Submit)

        assertEquals(listOf<LoginEffect>(LoginEffect.GoToUploadLicense), effects)
    }

    @Test
    fun contrasenaIncorrecta_muestraElError() = runTest {
        repository.result = AppResult.Failure(AppError.Unauthorized)
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fill()

        vm.onEvent(LoginEvent.Submit)

        assertEquals(AppError.Unauthorized, vm.state.value.error)
        assertTrue(effects.isEmpty())

        vm.onEvent(LoginEvent.PasswordChanged("otra"))
        assertNull(vm.state.value.error, "Al escribir se quita el error")
    }

    @Test
    fun otraCuentaConPendientes_avisaAntesDeEntrar_ySiAceptaBorraYEntra() = runTest {
        owners.owner = LocalDataOwner(userId = "user-9", email = "otra@polarsoft.mx")
        pending.count = 3
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fill()

        vm.onEvent(LoginEvent.Submit)

        assertEquals(OtherAccountDataWarning("otra@polarsoft.mx", 3), vm.state.value.otherAccountData)
        assertTrue(repository.calls.isEmpty(), "Todavía no entra")

        vm.onEvent(LoginEvent.ConfirmDiscardOtherAccountData)

        assertNull(vm.state.value.otherAccountData)
        assertEquals(listOf("test@polarsoft.mx" to true), repository.calls)
        assertEquals(listOf<LoginEffect>(LoginEffect.GoToHome), effects)
    }

    @Test
    fun otraCuentaConPendientes_siCancela_noEntra() = runTest {
        owners.owner = LocalDataOwner(userId = "user-9", email = "otra@polarsoft.mx")
        pending.count = 3
        val vm = viewModel()
        vm.fill()
        vm.onEvent(LoginEvent.Submit)

        vm.onEvent(LoginEvent.DismissOtherAccountData)

        assertNull(vm.state.value.otherAccountData)
        assertTrue(repository.calls.isEmpty())
    }
}
