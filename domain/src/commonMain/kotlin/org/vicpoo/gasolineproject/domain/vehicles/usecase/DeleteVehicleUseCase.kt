package org.vicpoo.gasolineproject.domain.vehicles.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleWriter

class DeleteVehicleUseCase(private val writer: VehicleWriter) {
    suspend operator fun invoke(id: String): AppResult<Unit> = writer.delete(id)
}
