package org.vicpoo.gasolineproject.ui.auth

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
import org.vicpoo.gasolineproject.domain.license.mustUploadLicense
import org.vicpoo.gasolineproject.domain.auth.usecase.LoginResult
import org.vicpoo.gasolineproject.domain.auth.usecase.LoginUseCase
import org.vicpoo.gasolineproject.domain.auth.validation.LoginValidator

class LoginViewModel(
    private val login: LoginUseCase,
    private val validator: LoginValidator,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _state.update { it.copy(email = event.value, emailError = null, error = null) }
            is LoginEvent.PasswordChanged -> _state.update { it.copy(password = event.value, passwordError = null, error = null) }
            LoginEvent.Submit -> submit()
            LoginEvent.ConfirmDiscardOtherAccountData -> {
                _state.update { it.copy(otherAccountData = null) }
                submit(discardOtherAccountData = true)
            }
            LoginEvent.DismissOtherAccountData -> _state.update { it.copy(otherAccountData = null) }
        }
    }

    private fun submit(discardOtherAccountData: Boolean = false) {
        val current = _state.value
        if (current.isLoading) return

        val validation = validator.validate(current.email, current.password)
        if (!validation.isValid) {
            _state.update { it.copy(emailError = validation.emailError, passwordError = validation.passwordError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = login(current.email, current.password, discardOtherAccountData)) {
                is LoginResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(
                        // La licencia solo se pide a quien maneja (viaje.registrar)
                        if (result.session.mustUploadLicense) LoginEffect.GoToUploadLicense else LoginEffect.GoToHome,
                    )
                }
                is LoginResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
                is LoginResult.OtherAccountPendingData -> _state.update {
                    it.copy(isLoading = false, otherAccountData = OtherAccountDataWarning(result.ownerEmail, result.pendingCount))
                }
            }
        }
    }
}
