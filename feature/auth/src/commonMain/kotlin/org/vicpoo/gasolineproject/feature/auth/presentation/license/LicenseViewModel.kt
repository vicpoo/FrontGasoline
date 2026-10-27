package org.vicpoo.gasolineproject.feature.auth.presentation.license

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.UploadLicenseUseCase

class LicenseViewModel(
    private val uploadLicense: UploadLicenseUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LicenseUiState())
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    private val _effects = Channel<LicenseEffect>(Channel.BUFFERED)
    val effects: Flow<LicenseEffect> = _effects.receiveAsFlow()

    fun onEvent(event: LicenseEvent) {
        when (event) {
            is LicenseEvent.FilePicked -> {
                _state.update { it.copy(showSourceOptions = false) }
                event.file?.let(::upload) // null = el usuario canceló
            }
            LicenseEvent.Retry -> _state.value.pendingFile?.let(::upload)
            LicenseEvent.UploadClicked -> if (!_state.value.isUploading) {
                _state.update { it.copy(showSourceOptions = true, error = null) }
            }
            LicenseEvent.SourceOptionsDismissed -> _state.update { it.copy(showSourceOptions = false) }
        }
    }

    private fun upload(file: PickedFile) {
        if (_state.value.isUploading) return
        viewModelScope.launch {
            _state.update {
                it.copy(selectedFileName = file.name, isUploading = true, error = null, pendingFile = file)
            }
            when (val result = uploadLicense(file)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isUploading = false, pendingFile = null) }
                    _effects.send(LicenseEffect.GoToHome)
                }
                is AppResult.Failure -> _state.update { it.copy(isUploading = false, error = result.error) }
            }
        }
    }
}
