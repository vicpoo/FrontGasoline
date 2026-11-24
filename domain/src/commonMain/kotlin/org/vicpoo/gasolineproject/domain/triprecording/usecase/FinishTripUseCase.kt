package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripField
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripResult
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripValidation
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripValidator
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/**
 * Termina el viaje con el kilometraje final y la foto del odómetro (offline-first: primero se guarda
 * en el teléfono y después se intenta enviar). Así la pantalla puede mostrar "¡Viaje registrado!"
 * o "¡Error al enviar!" sin perder nada.
 */
class FinishTripUseCase(
    private val tracker: TripTracker,
    private val validator: FinishTripValidator,
    private val imageCompressor: ImageCompressor,
) {
    /**
     * NonCancellable: si el usuario sale de la pantalla mientras se comprime la foto o se guarda,
     * el viaje igual queda guardado (antes se podía perder el cierre al salir a medio guardar).
     */
    suspend operator fun invoke(finalMileageText: String, odometerPhoto: PickedFile?): FinishTripResult =
        withContext(NonCancellable) { finish(finalMileageText, odometerPhoto) }

    private suspend fun finish(finalMileageText: String, odometerPhoto: PickedFile?): FinishTripResult {
        val trip = tracker.session.value.trip ?: return FinishTripResult.Failed(AppError.NotFound)

        val photo = odometerPhoto?.let { imageCompressor.compress(it) }
        if (odometerPhoto != null && photo == null) {
            // La foto no se pudo recodificar: se rechaza (no se sube la original con su EXIF)
            val otherErrors = (validator.validate(finalMileageText, trip.initialMileage, odometerPhoto) as? FinishTripValidation.Invalid)
                ?.errors.orEmpty()
            return FinishTripResult.Invalid(otherErrors + (FinishTripField.ODOMETER_PHOTO to ValidationReason.INVALID_FILE_TYPE))
        }
        val mileage = when (val validation = validator.validate(finalMileageText, trip.initialMileage, photo)) {
            is FinishTripValidation.Invalid -> return FinishTripResult.Invalid(validation.errors)
            is FinishTripValidation.Valid -> validation.finalMileage
        }

        // El validador ya exige la foto; si aun así faltara, se marca el campo en lugar de fallar
        val validPhoto = photo
            ?: return FinishTripResult.Invalid(mapOf(FinishTripField.ODOMETER_PHOTO to ValidationReason.REQUIRED))
        return when (val result = tracker.finish(mileage, validPhoto)) {
            is AppResult.Success -> FinishTripResult.Saved(result.data)
            is AppResult.Failure -> FinishTripResult.Failed(result.error)
        }
    }
}
