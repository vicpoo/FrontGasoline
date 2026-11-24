package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.CostCenter
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/**
 * Inicia un viaje con el carro asignado.
 * Reglas: un solo viaje a la vez y, si la empresa tiene centros de costo, elegir uno.
 */
class StartTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(
        vehicle: AssignedVehicle,
        costCenter: CostCenter?,
        availableCostCenters: List<CostCenter>,
    ): AppResult<RecordedTrip> {
        if (availableCostCenters.isNotEmpty() && costCenter == null) {
            return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        }
        tracker.session.value.trip?.let { running -> return AppResult.Success(running) }
        return tracker.start(vehicle, costCenter)
    }
}
