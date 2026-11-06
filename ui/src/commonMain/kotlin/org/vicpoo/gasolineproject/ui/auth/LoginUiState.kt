package org.vicpoo.gasolineproject.ui.auth

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason

/** Estado inmutable de la pantalla de login. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: ValidationReason? = null,
    val passwordError: ValidationReason? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    /** Aviso: en el teléfono hay registros sin enviar de otra cuenta (entrar los borra). */
    val otherAccountData: OtherAccountDataWarning? = null,
)

/** Registros sin enviar que dejó otra cuenta en este teléfono. */
data class OtherAccountDataWarning(val ownerEmail: String, val pendingCount: Int)
