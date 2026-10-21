package org.vicpoo.gasolineproject.feature.triprecording.domain

import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile

enum class FinishTripField { FINAL_MILEAGE, ODOMETER_PHOTO }

/** Resultado de validar "Registrar Viaje": el kilometraje listo o los errores por campo. */
sealed interface FinishTripValidation {
    data class Valid(val finalMileage: Double) : FinishTripValidation
    data class Invalid(val errors: Map<FinishTripField, ValidationReason>) : FinishTripValidation
}

/**
 * Reglas al terminar el viaje (las mismas que valida APIGasoline en Viaje.Finalizar):
 * - Kilometraje final obligatorio, numérico y no menor al inicial (máx. decimal(10,2)).
 * - Foto del odómetro obligatoria (jpg, png o webp, máx. 5 MB): es la evidencia del kilometraje.
 */
class FinishTripValidator {

    fun validate(finalMileageText: String, initialMileage: Double, photo: PickedFile?): FinishTripValidation {
        val errors = mutableMapOf<FinishTripField, ValidationReason>()

        val mileage = parseNumber(finalMileageText)
        when {
            finalMileageText.isBlank() -> errors[FinishTripField.FINAL_MILEAGE] = ValidationReason.REQUIRED
            mileage == null -> errors[FinishTripField.FINAL_MILEAGE] = ValidationReason.INVALID_FORMAT
            mileage < initialMileage || mileage > MAX_MILEAGE ->
                errors[FinishTripField.FINAL_MILEAGE] = ValidationReason.OUT_OF_RANGE
        }

        when {
            photo == null -> errors[FinishTripField.ODOMETER_PHOTO] = ValidationReason.REQUIRED
            photo.mimeType.lowercase() !in ALLOWED_MIME_TYPES -> errors[FinishTripField.ODOMETER_PHOTO] = ValidationReason.INVALID_FILE_TYPE
            photo.sizeBytes > MAX_PHOTO_BYTES -> errors[FinishTripField.ODOMETER_PHOTO] = ValidationReason.FILE_TOO_LARGE
        }

        return if (errors.isEmpty() && mileage != null) FinishTripValidation.Valid(mileage) else FinishTripValidation.Invalid(errors)
    }

    /** Acepta "672,287", "672287.5" y "672 287 km". */
    private fun parseNumber(text: String): Double? {
        val cleaned = text.trim()
            .removeSuffix("km").removeSuffix("KM").removeSuffix("Km")
            .replace(",", "")
            .replace(" ", "")
            .trim()
        return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    companion object {
        const val MAX_MILEAGE = 99_999_999.99
        const val MAX_PHOTO_BYTES = 5 * 1024 * 1024
        val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
    }
}
