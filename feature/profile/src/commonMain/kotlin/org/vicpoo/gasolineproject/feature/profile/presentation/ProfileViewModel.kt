package org.vicpoo.gasolineproject.feature.profile.presentation

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
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.LogoutAction
import org.vicpoo.gasolineproject.feature.profile.domain.ObserveProfileUseCase
import org.vicpoo.gasolineproject.feature.profile.domain.Profile
import org.vicpoo.gasolineproject.feature.profile.domain.RefreshProfileUseCase
import org.vicpoo.gasolineproject.feature.profile.domain.UpdateProfilePhotoUseCase

data class ProfileUiState(
    val profile: Profile? = null,
    val showPhotoOptions: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val photoError: AppError? = null,
    /** Foto pendiente para el botón "Reintentar". */
    val pendingPhoto: PickedFile? = null,
)

sealed interface ProfileEvent {
    data object EditPhotoClicked : ProfileEvent
    data object DismissPhotoOptions : ProfileEvent
    data class PhotoPicked(val file: PickedFile?) : ProfileEvent
    data object RetryPhoto : ProfileEvent
    data object DismissPhotoError : ProfileEvent
    data object Logout : ProfileEvent
}

sealed interface ProfileEffect {
    data object LoggedOut : ProfileEffect
}

class ProfileViewModel(
    observeProfile: ObserveProfileUseCase,
    private val refreshProfile: RefreshProfileUseCase,
    private val updatePhoto: UpdateProfilePhotoUseCase,
    private val logout: LogoutAction,
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
            ProfileEvent.Logout -> viewModelScope.launch {
                logout()
                _effects.send(ProfileEffect.LoggedOut)
            }
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
