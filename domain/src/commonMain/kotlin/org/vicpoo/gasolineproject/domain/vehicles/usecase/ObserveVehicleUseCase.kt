package org.vicpoo.gasolineproject.domain.vehicles.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleReader

class ObserveVehicleUseCase(private val reader: VehicleReader) {
    operator fun invoke(id: String): Flow<Vehicle?> = reader.observeVehicle(id)
}
