package org.vicpoo.gasolineproject.ui.auth

/** Eventos de una sola vez (navegación). */
sealed interface LoginEffect {
    data object GoToUploadLicense : LoginEffect
    data object GoToHome : LoginEffect
}
