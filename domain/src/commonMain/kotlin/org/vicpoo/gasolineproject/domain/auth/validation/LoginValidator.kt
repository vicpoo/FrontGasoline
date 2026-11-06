package org.vicpoo.gasolineproject.domain.auth.validation

import org.vicpoo.gasolineproject.domain.common.ValidationReason

data class LoginValidation(
    val emailError: ValidationReason? = null,
    val passwordError: ValidationReason? = null,
) {
    val isValid: Boolean get() = emailError == null && passwordError == null
}

/** Reglas del formulario de login (separadas para poder probarlas sin UI). */
class LoginValidator {
    fun validate(email: String, password: String) = LoginValidation(
        emailError = if (email.isBlank()) ValidationReason.REQUIRED else null,
        passwordError = if (password.isBlank()) ValidationReason.REQUIRED else null,
    )
}
