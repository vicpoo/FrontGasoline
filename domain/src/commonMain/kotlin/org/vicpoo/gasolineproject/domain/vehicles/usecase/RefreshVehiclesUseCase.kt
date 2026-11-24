package org.vicpoo.gasolineproject.domain.vehicles.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleReader

class RefreshVehiclesUseCase(private val reader: VehicleReader) {
    suspend operator fun invoke(): AppResult<Unit> = reader.refresh()
}
