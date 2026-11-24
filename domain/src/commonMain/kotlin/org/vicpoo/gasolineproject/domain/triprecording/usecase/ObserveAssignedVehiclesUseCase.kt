package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicleRepository

class ObserveAssignedVehiclesUseCase(private val repository: AssignedVehicleRepository) {
    operator fun invoke(): Flow<List<AssignedVehicle>> = repository.observe()
}
