package org.vicpoo.gasolineproject.feature.vehicles.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.repository.VehicleReader
import org.vicpoo.gasolineproject.feature.vehicles.domain.repository.VehicleWriter
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleFormInput
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleFormValidator

class ObserveVehiclesUseCase(private val reader: VehicleReader) {
    operator fun invoke(): Flow<List<Vehicle>> = reader.observeVehicles()
}

class ObserveVehicleUseCase(private val reader: VehicleReader) {
    operator fun invoke(id: String): Flow<Vehicle?> = reader.observeVehicle(id)
}

class ObservePendingVehicleSyncUseCase(private val reader: VehicleReader) {
    operator fun invoke(): Flow<Int> = reader.observePendingSyncCount()
}

class RefreshVehiclesUseCase(private val reader: VehicleReader) {
    suspend operator fun invoke(): AppResult<Unit> = reader.refresh()
}

/** Crea (id == null) o edita (id != null) un vehículo después de validar todo el formulario. */
class SaveVehicleUseCase(
    private val writer: VehicleWriter,
    private val validator: VehicleFormValidator,
) {
    suspend operator fun invoke(id: String?, input: VehicleFormInput): AppResult<Vehicle> {
        val errors = validator.validate(input)
        if (errors.isNotEmpty()) return AppResult.Failure(AppError.Validation(errors.values.first()))
        val draft = validator.toDraft(input)
        return if (id == null) writer.create(draft) else writer.update(id, draft)
    }
}

class DeleteVehicleUseCase(private val writer: VehicleWriter) {
    suspend operator fun invoke(id: String): AppResult<Unit> = writer.delete(id)
}
