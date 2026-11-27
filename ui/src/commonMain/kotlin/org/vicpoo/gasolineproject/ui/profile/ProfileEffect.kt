package org.vicpoo.gasolineproject.ui.profile

sealed interface ProfileEffect {
    data object LoggedOut : ProfileEffect

    /** Abrir "Registrar Viaje" en modo salir (kilometraje final y foto obligatorios). */
    data object OpenCloseTrip : ProfileEffect
}
