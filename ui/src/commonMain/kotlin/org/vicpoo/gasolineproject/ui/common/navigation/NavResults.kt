package org.vicpoo.gasolineproject.ui.common.navigation

/**
 * Llaves de resultados entre pantallas de features distintas (savedStateHandle de la pantalla anterior).
 * Viven aquí para que ninguna feature importe a otra.
 */
object NavResults {
    /** "Registrar Viaje" en modo salir guardó el viaje: Perfil termina de cerrar la sesión. */
    const val TRIP_CLOSED_FOR_LOGOUT = "tripClosedForLogout"
}
