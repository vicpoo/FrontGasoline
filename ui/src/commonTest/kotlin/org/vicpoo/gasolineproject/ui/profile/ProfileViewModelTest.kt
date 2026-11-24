package org.vicpoo.gasolineproject.ui.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.profile.usecase.ObserveProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.usecase.RefreshProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.usecase.UpdateProfilePhotoUseCase
import org.vicpoo.gasolineproject.domain.session.LogoutAction
import org.vicpoo.gasolineproject.domain.session.OpenTripCheck
import org.vicpoo.gasolineproject.domain.session.OpenTripSummary
import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.domain.settings.usecase.SetThemeModeUseCase
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.domain.sync.usecase.PendingBeforeLogoutUseCase
import org.vicpoo.gasolineproject.testing.FakePendingSyncTask
import org.vicpoo.gasolineproject.testing.FakeProfileRepository
import org.vicpoo.gasolineproject.testing.FakeThemePreferenceStore
import org.vicpoo.gasolineproject.testing.MainDispatcherTest

/** Perfil: foto (subir, error y reintentar), tema claro / oscuro y "Salir" con avisos. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest : MainDispatcherTest() {

    private val repository = FakeProfileRepository()
    private val pending = FakePendingSyncTask(PendingSyncKind.RESERVATIONS, count = 0)
    private val theme = FakeThemePreferenceStore()
    private var openTrip: OpenTripSummary? = null
    private var logoutCalls = 0
    private val photo = PickedFile(name = "perfil.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })

    private fun TestScope.viewModel(): ProfileViewModel {
        val vm = ProfileViewModel(
            observeProfile = ObserveProfileUseCase(repository),
            refreshProfile = RefreshProfileUseCase(repository),
            updatePhoto = UpdateProfilePhotoUseCase(repository, ImageCompressor { it }),
            logout = LogoutAction { logoutCalls++ },
            pendingBeforeLogout = PendingBeforeLogoutUseCase(listOf(pending)),
            setThemeMode = SetThemeModeUseCase(theme),
            openTrip = OpenTripCheck { openTrip },
        )
        // El estado solo se calcula mientras alguien lo escucha (WhileSubscribed)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    private fun TestScope.effectsOf(vm: ProfileViewModel): List<ProfileEffect> {
        val effects = mutableListOf<ProfileEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        return effects
    }

    @Test
    fun alAbrir_muestraElPerfil_yLoActualiza() = runTest {
        val vm = viewModel()

        assertEquals("Juan Pérez", vm.state.value.profile?.name)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun elegirFoto_laSube() = runTest {
        val vm = viewModel()

        vm.onEvent(ProfileEvent.EditPhotoClicked)
        assertTrue(vm.state.value.showPhotoOptions)
        vm.onEvent(ProfileEvent.PhotoPicked(photo))

        assertFalse(vm.state.value.showPhotoOptions)
        assertEquals(1, repository.photos.size)
        assertFalse(vm.state.value.isUploadingPhoto)
        assertNull(vm.state.value.pendingPhoto)
    }

    @Test
    fun siLaFotoFalla_seMuestraElError_yReintentarLaVuelveASubir() = runTest {
        repository.updatePhotoResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()

        vm.onEvent(ProfileEvent.PhotoPicked(photo))
        assertEquals(AppError.NoConnection, vm.state.value.photoError)

        repository.updatePhotoResult = AppResult.Success(Unit)
        vm.onEvent(ProfileEvent.RetryPhoto)

        assertEquals(2, repository.photos.size)
        assertNull(vm.state.value.photoError)
    }

    @Test
    fun cancelarElSelector_noSubeNada() = runTest {
        val vm = viewModel()

        vm.onEvent(ProfileEvent.PhotoPicked(null))

        assertTrue(repository.photos.isEmpty())
    }

    @Test
    fun salirSinPendientes_cierraSesionDirecto() = runTest {
        val vm = viewModel()
        val effects = effectsOf(vm)

        vm.onEvent(ProfileEvent.Logout)

        assertEquals(1, logoutCalls)
        assertEquals(listOf<ProfileEffect>(ProfileEffect.LoggedOut), effects)
    }

    @Test
    fun salirConRegistrosSinEnviar_avisaPrimero() = runTest {
        pending.count = 2
        val vm = viewModel()

        vm.onEvent(ProfileEvent.Logout)

        assertEquals(LogoutWarning(openTrip = null, lostCount = 2), vm.state.value.logoutWarning)
        assertEquals(0, logoutCalls)
        assertEquals(1, pending.syncCalls, "Antes de avisar intenta mandarlos")
    }

    @Test
    fun salirConViajeAbierto_avisaQueQuedaEnPausa_ySalirDeTodosModos() = runTest {
        openTrip = OpenTripSummary(vehicleTitle = "Nissan Versa", plates = "ABC-123", waitingForFinish = false)
        val vm = viewModel()
        val effects = effectsOf(vm)

        vm.onEvent(ProfileEvent.Logout)
        assertEquals(openTrip, vm.state.value.logoutWarning?.openTrip)

        vm.onEvent(ProfileEvent.LogoutAnyway)

        assertNull(vm.state.value.logoutWarning)
        assertEquals(1, logoutCalls)
        assertEquals(listOf<ProfileEffect>(ProfileEffect.LoggedOut), effects)
    }

    @Test
    fun cancelarElAviso_noCierraSesion() = runTest {
        pending.count = 2
        val vm = viewModel()
        vm.onEvent(ProfileEvent.Logout)

        vm.onEvent(ProfileEvent.DismissLogoutWarning)

        assertNull(vm.state.value.logoutWarning)
        assertEquals(0, logoutCalls)
    }

    @Test
    fun tema_cambiaAOscuro_yVolverAlDelTelefonoGuardaSistema() = runTest {
        val vm = viewModel()

        vm.onEvent(ProfileEvent.ToggleTheme(currentlyDark = false, systemDark = false))
        assertEquals(ThemeMode.DARK, theme.mode.value)

        vm.onEvent(ProfileEvent.ToggleTheme(currentlyDark = true, systemDark = false))
        assertEquals(ThemeMode.SYSTEM, theme.mode.value)

        vm.onEvent(ProfileEvent.ToggleTheme(currentlyDark = true, systemDark = true))
        assertEquals(ThemeMode.LIGHT, theme.mode.value)
    }
}
