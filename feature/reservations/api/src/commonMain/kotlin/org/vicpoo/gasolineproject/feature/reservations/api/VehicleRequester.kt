package org.vicpoo.gasolineproject.feature.reservations.api

import org.vicpoo.gasolineproject.core.common.AppResult

/**
 * API pública de la feature de reservas: lo único que otras features pueden usar de ella.
 *
 * Vehículos necesita "solicitar este carro" desde su detalle, pero no debe conocer el repositorio
 * ni los casos de uso de reservas.
 */
interface VehicleRequester {

    /**
     * Valida el motivo y envía la solicitud del carro.
     * Sin conexión NO falla: la guarda en el teléfono, regresa [RequestOutcome.QUEUED] y se envía
     * sola en cuanto vuelva la red (aparece en "Mis solicitudes" como "Por enviar").
     */
    suspend operator fun invoke(vehicle: RequestedVehicle, reason: String): AppResult<RequestOutcome>

    companion object {
        /** Largo máximo del motivo (lo usa la pantalla para el contador). */
        const val MAX_REASON = 255
    }
}

/**
 * El carro que se pide. Además del id se mandan los datos para pintarlo en "Mis solicitudes"
 * mientras la solicitud espera la red (todavía no hay respuesta de la API con el carro).
 */
data class RequestedVehicle(
    /** carroId en la API; null = el carro se creó sin conexión y aún no existe allá. */
    val serverId: String?,
    val title: String? = null,
    val plates: String? = null,
    val photoUrl: String? = null,
)

enum class RequestOutcome {
    /** La API ya la recibió. */
    SENT,

    /** Sin conexión: quedó guardada y se enviará sola. */
    QUEUED,
}
