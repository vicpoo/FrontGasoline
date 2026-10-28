package org.vicpoo.gasolineproject.feature.reservations.domain

import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.feature.reservations.api.RequestOutcome
import org.vicpoo.gasolineproject.feature.reservations.api.RequestedVehicle
import org.vicpoo.gasolineproject.feature.reservations.api.VehicleRequester

/** Valida el motivo y envía la solicitud (o la deja en cola sin red). Otras features lo usan como [VehicleRequester]. */
class RequestVehicleUseCase(private val repository: ReservationRepository) : VehicleRequester {

    override suspend operator fun invoke(vehicle: RequestedVehicle, reason: String): AppResult<RequestOutcome> {
        val clean = reason.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_REASON) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        // Un carro creado sin conexión aún no existe en la API
        if (vehicle.serverId == null) return AppResult.Failure(AppError.NotFound)
        return repository.requestVehicle(vehicle, clean)
    }

    companion object {
        const val MAX_REASON = VehicleRequester.MAX_REASON
    }
}
