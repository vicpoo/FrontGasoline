package org.vicpoo.gasolineproject.domain.session

/**
 * Viaje de la cuenta actual que sigue abierto en el teléfono (en curso, en pausa o esperando el
 * kilometraje final). Al cerrar sesión NO se borra: queda en pausa y se retoma al volver a entrar.
 */
data class OpenTripSummary(
    /** "Nissan Versa". */
    val vehicleTitle: String,
    val plates: String,
    /** true si ya se detuvo y solo falta "Registrar Viaje" (kilometraje y foto). */
    val waitingForFinish: Boolean,
)

/**
 * "¿Hay un viaje abierto?" visto desde Perfil (para avisar antes de "Salir") sin depender de la
 * feature de viajes. La implementa el registro de viajes y se registra en Koin.
 */
fun interface OpenTripCheck {
    suspend operator fun invoke(): OpenTripSummary?
}
