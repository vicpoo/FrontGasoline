package org.vicpoo.gasolineproject.feature.triprecording.domain

import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile

enum class FuelField { AMOUNT, LITERS, COST_CENTER, RECEIPT }

/** Resultado de validar el formulario: valores listos o errores por campo. */
sealed interface FuelValidation {
    data class Valid(val values: FuelRecordValues) : FuelValidation
    data class Invalid(val errors: Map<FuelField, ValidationReason>) : FuelValidation
}

/**
 * Reglas de una recarga (las mismas que valida APIGasoline en CargaCombustible):
 * - Litros > 0 y que no excedan la capacidad del tanque del carro.
 * - Monto >= 0 (en la app es obligatorio para el reporte de gastos).
 * - Foto del recibo obligatoria al crear (jpg, png o webp, máx. 5 MB).
 */
class FuelRecordValidator {

    fun validate(
        draft: FuelRecordDraft,
        costCenters: List<CostCenter>,
        tankCapacityLiters: Double?,
    ): FuelValidation {
        val errors = mutableMapOf<FuelField, ValidationReason>()

        val amount = parseNumber(draft.amountText)
        when {
            draft.amountText.isBlank() -> errors[FuelField.AMOUNT] = ValidationReason.REQUIRED
            amount == null -> errors[FuelField.AMOUNT] = ValidationReason.INVALID_FORMAT
            amount < 0 || amount > MAX_AMOUNT -> errors[FuelField.AMOUNT] = ValidationReason.OUT_OF_RANGE
        }

        val liters = parseNumber(draft.litersText)
        val maxLiters = tankCapacityLiters?.takeIf { it > 0 } ?: MAX_LITERS
        when {
            draft.litersText.isBlank() -> errors[FuelField.LITERS] = ValidationReason.REQUIRED
            liters == null -> errors[FuelField.LITERS] = ValidationReason.INVALID_FORMAT
            liters <= 0 || liters > maxLiters -> errors[FuelField.LITERS] = ValidationReason.OUT_OF_RANGE
        }

        val costCenter = costCenters.firstOrNull { it.id == draft.costCenterId }
        if (costCenters.isNotEmpty() && costCenter == null) {
            errors[FuelField.COST_CENTER] = ValidationReason.REQUIRED
        }

        receiptError(draft.newReceipt, draft.hasCurrentReceipt)?.let { errors[FuelField.RECEIPT] = it }

        return if (errors.isEmpty() && liters != null) {
            FuelValidation.Valid(FuelRecordValues(amount = amount, liters = liters, costCenter = costCenter))
        } else {
            FuelValidation.Invalid(errors)
        }
    }

    fun receiptError(file: PickedFile?, hasCurrentReceipt: Boolean): ValidationReason? = when {
        file == null && !hasCurrentReceipt -> ValidationReason.REQUIRED
        file == null -> null
        file.mimeType.lowercase() !in ALLOWED_MIME_TYPES -> ValidationReason.INVALID_FILE_TYPE
        file.sizeBytes > MAX_RECEIPT_BYTES -> ValidationReason.FILE_TOO_LARGE
        else -> null
    }

    /** Acepta "1,000.50", "1000.5", "$1,000" y "10 L". */
    private fun parseNumber(text: String): Double? {
        val cleaned = text.trim()
            .removePrefix("$")
            .removeSuffix("L").removeSuffix("l")
            .replace(",", "")
            .trim()
        return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    companion object {
        const val MAX_RECEIPT_BYTES = 5 * 1024 * 1024
        const val MAX_LITERS = 999_999.99
        const val MAX_AMOUNT = 99_999_999.99
        val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
    }
}
