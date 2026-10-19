package org.vicpoo.gasolineproject.feature.auth.presentation.login

import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.ValidationReason

/** Estado inmutable de la pantalla de login. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: ValidationReason? = null,
    val passwordError: ValidationReason? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

/** Acciones del usuario. */
sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object Submit : LoginEvent
}

/** Eventos de una sola vez (navegación). */
sealed interface LoginEffect {
    data object GoToUploadLicense : LoginEffect
    data object GoToHome : LoginEffect
}
