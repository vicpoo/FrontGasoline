package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicleRepository

class RefreshAssignedVehiclesUseCase(private val repository: AssignedVehicleRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
