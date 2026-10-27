package org.vicpoo.gasolineproject.feature.reservations.api

import org.vicpoo.gasolineproject.core.common.AppResult

/**
 * API pública de la feature de reservas: lo único que otras features pueden usar de ella.
 *
 * Vehículos necesita "solicitar este carro" desde su detalle, pero no debe conocer el repositorio
 * ni los casos de uso de reservas. Al pasar a multimódulo, este paquete será :feature:reservations:api.
 */
interface VehicleRequester {

    /** Valida el motivo y envía la solicitud del carro. [vehicleServerId] null = carro aún no sincronizado. */
    suspend operator fun invoke(vehicleServerId: String?, reason: String): AppResult<Unit>

    companion object {
        /** Largo máximo del motivo (lo usa la pantalla para el contador). */
        const val MAX_REASON = 255
    }
}
