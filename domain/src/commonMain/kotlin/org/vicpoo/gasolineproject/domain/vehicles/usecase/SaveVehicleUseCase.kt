package org.vicpoo.gasolineproject.domain.vehicles.usecase

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleWriter
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormInput
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormValidator

/**
 * Crea (id == null) o edita (id != null) un vehículo después de validar todo el formulario.
 * La foto nueva se recodifica antes de guardarla: pierde el EXIF (ubicación GPS de la galería).
 * Si no se puede leer se rechaza; nunca se guarda ni se sube la original.
 */
class SaveVehicleUseCase(
    private val writer: VehicleWriter,
    private val validator: VehicleFormValidator,
    private val imageCompressor: ImageCompressor,
) {
    suspend operator fun invoke(id: String?, input: VehicleFormInput): AppResult<Vehicle> {
        val errors = validator.validate(input)
        if (errors.isNotEmpty()) return AppResult.Failure(AppError.Validation(errors.values.first()))
        val photo = input.newPhoto?.let { original ->
            imageCompressor.compress(original)
                ?: return AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE))
        }
        val draft = validator.toDraft(input.copy(newPhoto = photo))
        return if (id == null) writer.create(draft) else writer.update(id, draft)
    }
}
