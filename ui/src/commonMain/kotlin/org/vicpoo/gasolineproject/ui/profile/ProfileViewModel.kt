package org.vicpoo.gasolineproject.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.profile.usecase.ObserveProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.usecase.RefreshProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.usecase.UpdateProfilePhotoUseCase
import org.vicpoo.gasolineproject.domain.session.LogoutAction
import org.vicpoo.gasolineproject.domain.session.OpenTripCheck
import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.domain.settings.usecase.SetThemeModeUseCase
import org.vicpoo.gasolineproject.domain.sync.usecase.PendingBeforeLogoutUseCase

class ProfileViewModel(
    observeProfile: ObserveProfileUseCase,
    private val refreshProfile: RefreshProfileUseCase,
    private val updatePhoto: UpdateProfilePhotoUseCase,
    private val logout: LogoutAction,
    private val pendingBeforeLogout: PendingBeforeLogoutUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val openTrip: OpenTripCheck,
) : ViewModel() {

    private val local = MutableStateFlow(ProfileUiState())

    val state: StateFlow<ProfileUiState> = combine(observeProfile(), local) { profile, ui ->
        ui.copy(profile = profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init {
        // Trae nombre/foto actualizados; sin red se queda lo guardado
        viewModelScope.launch { refreshProfile() }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.EditPhotoClicked -> local.update { it.copy(showPhotoOptions = true) }
            ProfileEvent.DismissPhotoOptions -> local.update { it.copy(showPhotoOptions = false) }
            is ProfileEvent.PhotoPicked -> {
                local.update { it.copy(showPhotoOptions = false) }
                event.file?.let(::upload)
            }
            ProfileEvent.RetryPhoto -> local.value.pendingPhoto?.let(::upload)
            ProfileEvent.DismissPhotoError -> local.update { it.copy(photoError = null, pendingPhoto = null) }
            ProfileEvent.Logout -> checkPendingAndLogout()
            ProfileEvent.LogoutAnyway -> {
                local.update { it.copy(logoutWarning = null) }
                doLogout()
            }
            ProfileEvent.DismissLogoutWarning -> local.update { it.copy(logoutWarning = null) }
            ProfileEvent.CloseTripToLogout -> {
                local.update { it.copy(logoutWarning = null) }
                viewModelScope.launch { _effects.send(ProfileEffect.OpenCloseTrip) }
            }
            // El viaje ya quedó guardado (enviado o pendiente): se revisa otra vez lo pendiente y se sale
            ProfileEvent.TripClosedForLogout -> checkPendingAndLogout()
            // Se guarda en el teléfono; App() lo escucha y los colores cambian con una transición suave.
            // Si el modo elegido es el mismo que el del teléfono se guarda SYSTEM: la app vuelve a seguir al teléfono.
            is ProfileEvent.ToggleTheme -> viewModelScope.launch {
                val wantDark = !event.currentlyDark
                setThemeMode(
                    when {
                        wantDark == event.systemDark -> ThemeMode.SYSTEM
                        wantDark -> ThemeMode.DARK
                        else -> ThemeMode.LIGHT
                    },
                )
            }
        }
    }

    /**
     * Antes de cerrar sesión se avisa si:
     * - Hay un viaje abierto: para salir hay que cerrarlo con el kilometraje final y la foto
     *   ("Registrar Viaje" en modo salir). Se guarda como interrumpido por cierre de sesión.
     *   Se revisa PRIMERO y sin red (está en memoria): antes se esperaba a mandar todo lo pendiente
     *   (puntos GPS, fotos...) y el aviso tardaba lo que tardaran esos envíos o sus timeouts.
     *   El viaje se manda al guardarlo en "Registrar Viaje" y, al regresar, se revisa lo demás.
     * - Quedan registros sin enviar que SÍ se borran (solicitudes, vehículos...): se intenta mandarlos.
     * Sin nada de eso se sale directo.
     */
    private fun checkPendingAndLogout() {
        if (local.value.isCheckingLogout) return
        viewModelScope.launch {
            local.update { it.copy(isCheckingLogout = true) }
            val trip = openTrip()
            val lost = if (trip == null) pendingBeforeLogout() else 0
            local.update { it.copy(isCheckingLogout = false) }
            if (lost == 0 && trip == null) {
                doLogout()
            } else {
                local.update { it.copy(logoutWarning = LogoutWarning(openTrip = trip, lostCount = lost)) }
            }
        }
    }

    private fun doLogout() {
        viewModelScope.launch {
            logout()
            _effects.send(ProfileEffect.LoggedOut)
        }
    }

    private fun upload(file: PickedFile) {
        if (local.value.isUploadingPhoto) return
        viewModelScope.launch {
            local.update { it.copy(isUploadingPhoto = true, photoError = null, pendingPhoto = file) }
            when (val result = updatePhoto(file)) {
                is AppResult.Success -> local.update { it.copy(isUploadingPhoto = false, pendingPhoto = null) }
                is AppResult.Failure -> local.update { it.copy(isUploadingPhoto = false, photoError = result.error) }
            }
        }
    }
}
