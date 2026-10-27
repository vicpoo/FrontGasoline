package org.vicpoo.gasolineproject.feature.profile.presentation.editname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.feature.profile.domain.ObserveProfileUseCase
import org.vicpoo.gasolineproject.feature.profile.domain.UpdateNameUseCase

data class EditNameUiState(
    val name: String = "",
    val fieldError: ValidationReason? = null,
    val isSaving: Boolean = false,
    /** Diálogo "¡Error al guardar!" */
    val saveError: AppError? = null,
    /** Diálogo "¡Nombre guardado!" */
    val saved: Boolean = false,
)

sealed interface EditNameEvent {
    data class NameChanged(val value: String) : EditNameEvent
    data object Save : EditNameEvent
    data object DismissError : EditNameEvent
}

class EditNameViewModel(
    private val observeProfile: ObserveProfileUseCase,
    private val updateName: UpdateNameUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditNameUiState())
    val state: StateFlow<EditNameUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val current = observeProfile().first()?.name.orEmpty()
            _state.update { if (it.name.isEmpty()) it.copy(name = current) else it }
        }
    }

    fun onEvent(event: EditNameEvent) {
        when (event) {
            is EditNameEvent.NameChanged -> _state.update {
                it.copy(name = event.value.take(UpdateNameUseCase.MAX_NAME), fieldError = null)
            }
            EditNameEvent.Save -> save()
            EditNameEvent.DismissError -> _state.update { it.copy(saveError = null) }
        }
    }

    private fun save() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            when (val result = updateName(_state.value.name)) {
                is AppResult.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is AppResult.Failure -> _state.update {
                    val error = result.error
                    if (error is AppError.Validation) {
                        it.copy(isSaving = false, fieldError = error.reason)
                    } else {
                        it.copy(isSaving = false, saveError = error)
                    }
                }
            }
        }
    }
}
