package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicleRepository

class GetAssignedVehicleUseCase(private val repository: AssignedVehicleRepository) {
    suspend operator fun invoke(reservationId: String): AssignedVehicle? = repository.find(reservationId)
}
