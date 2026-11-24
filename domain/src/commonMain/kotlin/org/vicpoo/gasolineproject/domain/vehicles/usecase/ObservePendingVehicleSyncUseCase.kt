package org.vicpoo.gasolineproject.domain.vehicles.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleReader

class ObservePendingVehicleSyncUseCase(private val reader: VehicleReader) {
    operator fun invoke(): Flow<Int> = reader.observePendingSyncCount()
}
