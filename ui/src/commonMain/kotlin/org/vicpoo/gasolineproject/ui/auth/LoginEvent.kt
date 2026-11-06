package org.vicpoo.gasolineproject.ui.auth

/** Acciones del usuario. */
sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object Submit : LoginEvent

    /** "Entrar y borrar": acepta perder lo que la otra cuenta no envió. */
    data object ConfirmDiscardOtherAccountData : LoginEvent

    /** "Cancelar": no entra (la otra cuenta puede entrar primero para enviar lo suyo). */
    data object DismissOtherAccountData : LoginEvent
}
