package org.vicpoo.gasolineproject.feature.reservations.domain

import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.feature.reservations.api.VehicleRequester

/** Valida el motivo y envía la solicitud. Otras features lo usan como [VehicleRequester]. */
class RequestVehicleUseCase(private val repository: ReservationRepository) : VehicleRequester {

    override suspend operator fun invoke(vehicleServerId: String?, reason: String): AppResult<Unit> {
        val clean = reason.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_REASON) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        // Un carro creado sin conexión aún no existe en la API
        if (vehicleServerId == null) return AppResult.Failure(AppError.NotFound)
        return repository.requestVehicle(vehicleServerId, clean)
    }

    companion object {
        const val MAX_REASON = VehicleRequester.MAX_REASON
    }
}
